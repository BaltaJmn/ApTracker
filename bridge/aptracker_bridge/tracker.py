from __future__ import annotations

import asyncio
import logging
import time
from datetime import datetime, timedelta, timezone
from typing import Any

from .archipelago import RoomWatcher, TrackerEvent
from .config import Config
from .fcm import FcmSender
from .ntfy import Ntfy
from .supabase import SupabaseREST

log = logging.getLogger(__name__)

FLAG_PROGRESSION = 0b001
FLAG_USEFUL = 0b010
FLAG_TRAP = 0b100


class Tracker:
    """Top-level orchestrator: keeps one RoomWatcher per room with tracked slots,
    re-syncing the set from Supabase on an interval."""

    def __init__(self, config: Config) -> None:
        self.config = config
        self.supabase = SupabaseREST(config.supabase_url, config.supabase_service_key)
        self.ntfy = Ntfy(config.ntfy_base_url)
        self.fcm: FcmSender | None = None
        if config.fcm_service_account_file:
            try:
                self.fcm = FcmSender(config.fcm_service_account_file)
            except Exception as exc:  # noqa: BLE001
                log.error("FCM disabled — could not load service account: %s", exc)
        self._watchers: dict[str, dict[str, Any]] = {}  # room_id -> {task, hash}
        self._slots_by_room: dict[str, list[dict[str, Any]]] = {}
        self._android_tokens_by_user: dict[str, list[str]] = {}
        self._last_cleanup = 0.0

    async def run(self) -> None:
        log.info("ApTracker bridge started (sync every %ss)", self.config.sync_interval)
        try:
            while True:
                try:
                    await self._sync_once()
                except asyncio.CancelledError:
                    raise
                except Exception as exc:  # noqa: BLE001
                    log.error("sync failed: %s", exc)
                await asyncio.sleep(self.config.sync_interval)
        finally:
            await self._shutdown()

    async def _sync_once(self) -> None:
        rooms = await self.supabase.fetch_rooms()
        slots = await self.supabase.fetch_slots()

        slots_by_room: dict[str, list[dict[str, Any]]] = {}
        for slot in slots:
            slots_by_room.setdefault(slot["room_id"], []).append(slot)
        self._slots_by_room = slots_by_room

        if self.fcm:
            await self._refresh_push_tokens()

        await self._cleanup_old_activity()

        # Only watch rooms that actually have tracked slots.
        wanted = {r["id"]: r for r in rooms if slots_by_room.get(r["id"])}

        # Stop watchers for rooms that disappeared or lost all slots.
        for room_id in list(self._watchers):
            if room_id not in wanted:
                await self._stop_watcher(room_id)

        # Start or restart watchers.
        for room_id, room in wanted.items():
            slot_hash = self._slot_hash(slots_by_room[room_id])
            existing = self._watchers.get(room_id)
            if existing and existing["hash"] == slot_hash and not existing["task"].done():
                continue
            if existing:
                await self._stop_watcher(room_id)
            watcher = RoomWatcher(
                room=room,
                get_slots=lambda rid=room_id: self._slots_by_room.get(rid, []),
                on_event=self._on_event,
                tls_insecure=self.config.tls_insecure,
                max_backoff=self.config.reconnect_max_backoff,
            )
            task = asyncio.create_task(watcher.run(), name=f"room:{room_id}")
            self._watchers[room_id] = {"task": task, "hash": slot_hash}
            log.info("watching room '%s' (%d slots)", room.get("name"), len(slots_by_room[room_id]))

    @staticmethod
    def _slot_hash(slots: list[dict[str, Any]]) -> int:
        return hash(tuple(sorted(s["slot_name"] for s in slots if s.get("slot_name"))))

    async def _stop_watcher(self, room_id: str) -> None:
        entry = self._watchers.pop(room_id, None)
        if not entry:
            return
        entry["task"].cancel()
        try:
            await entry["task"]
        except (asyncio.CancelledError, Exception):  # noqa: BLE001
            pass
        log.info("stopped watching room %s", room_id)

    async def _on_event(self, room: dict[str, Any], slot: dict[str, Any], event: TrackerEvent) -> None:
        # Persist to the activity feed regardless of notification prefs, so the
        # in-app history stays complete even when a category is muted.
        row = {
            "room_id": room["id"],
            "slot_name": slot.get("slot_name"),
            "event_type": event.event_type,
            "item_name": event.item_name,
            "location_name": event.location_name,
            "sender_name": event.sender_name,
            "receiver_name": event.receiver_name,
            "item_flags": event.flags,
        }
        try:
            await self.supabase.insert_activity(row)
        except Exception as exc:  # noqa: BLE001
            log.warning("failed to save activity: %s", exc)

        if self._should_notify(slot, event):
            topic = room.get("ntfy_topic") or self.config.ntfy_default_topic
            title, message, tags, priority = self._format(slot, event)
            await self.ntfy.publish(topic, title, message, tags, priority)
            await self._send_fcm(room, title, message)

    async def _cleanup_old_activity(self) -> None:
        """Once a day, prune activity older than ACTIVITY_RETENTION_DAYS so the
        table (and the free Supabase tier) doesn't grow without bound."""
        days = self.config.activity_retention_days
        if days <= 0 or time.monotonic() - self._last_cleanup < 86_400:
            return
        self._last_cleanup = time.monotonic()
        cutoff = (datetime.now(timezone.utc) - timedelta(days=days)).isoformat()
        try:
            await self.supabase.delete_activity_before(cutoff)
            log.info("pruned activity older than %s days", days)
        except Exception as exc:  # noqa: BLE001
            log.warning("activity cleanup failed: %s", exc)

    async def _refresh_push_tokens(self) -> None:
        try:
            tokens = await self.supabase.fetch_push_tokens()
        except Exception as exc:  # noqa: BLE001
            log.warning("could not fetch push tokens: %s", exc)
            return
        by_user: dict[str, list[str]] = {}
        for row in tokens:
            if row.get("platform") == "android" and row.get("token"):
                by_user.setdefault(row["user_id"], []).append(row["token"])
        self._android_tokens_by_user = by_user

    async def _send_fcm(self, room: dict[str, Any], title: str, message: str) -> None:
        if not self.fcm:
            return
        tokens = self._android_tokens_by_user.get(room.get("user_id"), [])
        for token in list(tokens):
            result = await self.fcm.send(token, title, message)
            if result == "stale":
                tokens.remove(token)
                try:
                    await self.supabase.delete_push_token(token)
                except Exception as exc:  # noqa: BLE001
                    log.warning("could not delete stale token: %s", exc)

    @staticmethod
    def _should_notify(slot: dict[str, Any], event: TrackerEvent) -> bool:
        # Push only for items coming *to* this slot (and hints about them); "sent"
        # events are recorded for the feed but don't buzz your phone.
        if event.event_type not in ("received", "hint"):
            return False
        if not slot.get("notify_enabled", True):
            return False
        sender = event.sender_name
        slot_name = slot.get("slot_name")
        if slot.get("suppress_local", False) and sender == slot_name:
            return False
        if slot.get("suppress_others", False) and sender != slot_name:
            return False
        flags = event.flags
        if flags & FLAG_PROGRESSION:
            return slot.get("notify_progression", True)
        if flags & FLAG_USEFUL:
            return slot.get("notify_useful", True)
        return slot.get("notify_filler", False)

    @staticmethod
    def _format(slot: dict[str, Any], event: TrackerEvent) -> tuple[str, str, list[str], int]:
        slot_name = slot.get("slot_name") or "Slot"
        if event.event_type == "hint":
            message = f"💡 {event.item_name} está en {event.location_name}"
            return f"Pista · {slot_name}", message, ["bulb"], 3
        # received
        sender = event.sender_name or "alguien"
        message = f"{sender} te envió {event.item_name}"
        if event.flags & FLAG_TRAP:
            return f"⚠️ {slot_name}", f"{sender} te envió una trampa: {event.item_name}", ["skull"], 4
        if event.flags & FLAG_PROGRESSION:
            return f"⭐ {slot_name}", message, ["star"], 4
        if event.flags & FLAG_USEFUL:
            return slot_name, message, ["gift"], 3
        return slot_name, message, ["package"], 2

    async def _shutdown(self) -> None:
        for room_id in list(self._watchers):
            await self._stop_watcher(room_id)
        await self.supabase.aclose()
        await self.ntfy.aclose()
        if self.fcm:
            await self.fcm.aclose()
