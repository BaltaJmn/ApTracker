from __future__ import annotations

import asyncio
import json
import logging
import ssl
import time
import uuid
from dataclasses import dataclass, field
from typing import Any, Awaitable, Callable

import websockets

log = logging.getLogger(__name__)

# The tracker connects as a spectator. The "Tracker" tag lets several clients
# share a slot without kicking the real game client, and items_handling=0 means
# the server won't replay the whole ReceivedItems backlog on every reconnect —
# we rely on the live PrintJSON stream instead, which is never replayed.
AP_VERSION = {"major": 0, "minor": 5, "build": 0, "class": "Version"}


@dataclass
class DataPackage:
    """id -> name lookups per game, inverted from the server's name -> id maps."""

    items: dict[str, dict[int, str]] = field(default_factory=dict)      # game -> {id: name}
    locations: dict[str, dict[int, str]] = field(default_factory=dict)  # game -> {id: name}

    def merge(self, games: dict[str, Any]) -> None:
        for game, data in games.items():
            item_map = {v: k for k, v in (data.get("item_name_to_id") or {}).items()}
            loc_map = {v: k for k, v in (data.get("location_name_to_id") or {}).items()}
            self.items[game] = item_map
            self.locations[game] = loc_map

    def item_name(self, game: str | None, item_id: int | None) -> str:
        if item_id is None:
            return "?"
        return self.items.get(game or "", {}).get(item_id, f"Item {item_id}")

    def location_name(self, game: str | None, location_id: int | None) -> str:
        if location_id is None:
            return "?"
        return self.locations.get(game or "", {}).get(location_id, f"Location {location_id}")


# A resolved, slot-relative event handed to the orchestrator.
@dataclass
class TrackerEvent:
    event_type: str          # "received" | "sent" | "hint"
    item_name: str
    location_name: str
    sender_name: str | None
    receiver_name: str | None
    flags: int


EventHandler = Callable[[dict, dict, TrackerEvent], Awaitable[None]]


class RoomWatcher:
    """Maintains one persistent Tracker connection to a single Archipelago room
    and emits resolved events for every tracked slot involved in an item send."""

    def __init__(
        self,
        room: dict[str, Any],
        get_slots: Callable[[], list[dict[str, Any]]],
        on_event: EventHandler,
        *,
        tls_insecure: bool = False,
        max_backoff: int = 60,
    ) -> None:
        self.room = room
        self.get_slots = get_slots
        self.on_event = on_event
        self.tls_insecure = tls_insecure
        self.max_backoff = max_backoff

        self.host: str = room["host"]
        self.port: int = int(room.get("port") or 38281)
        self.password: str | None = room.get("password")
        self.name = room.get("name") or self.host
        self._uuid = f"aptracker-{room['id']}"

        # Per-connection state (reset on each (re)connect).
        self._datapackage = DataPackage()
        self._slot_by_number: dict[int, dict[str, Any]] = {}
        self._name_to_number: dict[str, int] = {}
        self._games: list[str] = []
        self._connect_sent = False
        self._datapackage_requested = False
        self._candidate_index = 0
        self._preferred_url: str | None = None

    async def run(self) -> None:
        backoff = 1
        while True:
            started = time.monotonic()
            try:
                await self._connect_and_run()
            except asyncio.CancelledError:
                raise
            except Exception as exc:  # noqa: BLE001
                log.warning("[%s] connection dropped: %s", self.name, exc)
            # Reset backoff only if we stayed connected for a meaningful while,
            # otherwise ramp up to avoid hammering a refusing/flapping server.
            if time.monotonic() - started > 30:
                backoff = 1
            await asyncio.sleep(backoff)
            backoff = min(backoff * 2, self.max_backoff)

    def _reset_connection_state(self) -> None:
        self._datapackage = DataPackage()
        self._slot_by_number = {}
        self._name_to_number = {}
        self._games = []
        self._connect_sent = False
        self._datapackage_requested = False
        self._candidate_index = 0

    def _urls(self) -> list[str]:
        secure = f"wss://{self.host}:{self.port}"
        plain = f"ws://{self.host}:{self.port}"
        if self._preferred_url == plain:
            return [plain, secure]
        return [secure, plain]

    def _ssl_arg(self, url: str):
        if not url.startswith("wss://"):
            return None
        if self.tls_insecure:
            ctx = ssl.create_default_context()
            ctx.check_hostname = False
            ctx.verify_mode = ssl.CERT_NONE
            return ctx
        return ssl.create_default_context()

    async def _connect_and_run(self) -> None:
        last_exc: Exception | None = None
        for url in self._urls():
            try:
                async with websockets.connect(
                    url,
                    ssl=self._ssl_arg(url),
                    ping_interval=20,
                    ping_timeout=20,
                    open_timeout=15,
                    max_size=32 * 1024 * 1024,
                ) as ws:
                    self._preferred_url = url
                    self._reset_connection_state()
                    log.info("[%s] connected via %s", self.name, url.split("://")[0])
                    await self._session(ws)
                    return
            except asyncio.CancelledError:
                raise
            except (OSError, ssl.SSLError, websockets.InvalidHandshake, websockets.InvalidURI) as exc:
                last_exc = exc
                continue
        if last_exc:
            raise last_exc

    async def _session(self, ws) -> None:
        async for raw in ws:
            try:
                packets = json.loads(raw)
            except (ValueError, TypeError):
                continue
            if not isinstance(packets, list):
                continue
            for packet in packets:
                if isinstance(packet, dict):
                    await self._handle_packet(ws, packet)

    async def _handle_packet(self, ws, packet: dict[str, Any]) -> None:
        cmd = packet.get("cmd")
        if cmd == "RoomInfo":
            self._games = list(packet.get("games") or [])
            if not self._datapackage_requested:
                self._datapackage_requested = True
                await self._send(ws, [{"cmd": "GetDataPackage", "games": self._games}])
            if not self._connect_sent:
                await self._send_connect(ws)
        elif cmd == "DataPackage":
            games = (packet.get("data") or {}).get("games") or {}
            self._datapackage.merge(games)
            log.debug("[%s] datapackage loaded for %d games", self.name, len(games))
        elif cmd == "Connected":
            self._on_connected(packet)
        elif cmd == "ConnectionRefused":
            await self._on_refused(ws, packet)
        elif cmd == "PrintJSON":
            await self._on_print_json(packet)

    async def _send(self, ws, payload: list) -> None:
        await ws.send(json.dumps(payload))

    def _connect_candidates(self) -> list[str]:
        names = [s["slot_name"] for s in self.get_slots() if s.get("slot_name")]
        # de-dupe, keep order
        seen: set[str] = set()
        return [n for n in names if not (n in seen or seen.add(n))]

    async def _send_connect(self, ws) -> None:
        candidates = self._connect_candidates()
        if not candidates:
            log.warning("[%s] no tracked slots; nothing to watch", self.name)
            return
        if self._candidate_index >= len(candidates):
            log.error("[%s] all tracked slot names were refused by the server", self.name)
            return
        name = candidates[self._candidate_index]
        self._connect_sent = True
        await self._send(ws, [{
            "cmd": "Connect",
            "game": "",
            "name": name,
            "password": self.password or "",
            "version": AP_VERSION,
            "items_handling": 0,
            "tags": ["Tracker"],
            "uuid": self._uuid,
            "slot_data": False,
        }])

    def _on_connected(self, packet: dict[str, Any]) -> None:
        slot_info = packet.get("slot_info") or {}
        self._slot_by_number = {}
        self._name_to_number = {}
        for number_str, info in slot_info.items():
            try:
                number = int(number_str)
            except (TypeError, ValueError):
                continue
            self._slot_by_number[number] = info
            name = info.get("name")
            if name:
                self._name_to_number[name] = number
        log.info(
            "[%s] tracking as spectator; %d players in room",
            self.name,
            len(self._slot_by_number),
        )

    async def _on_refused(self, ws, packet: dict[str, Any]) -> None:
        errors = packet.get("errors") or []
        log.warning("[%s] connection refused: %s", self.name, ", ".join(errors) or "unknown")
        # If the slot name was invalid, try the next tracked slot name.
        if "InvalidSlot" in errors or "InvalidGame" in errors:
            self._candidate_index += 1
            self._connect_sent = False
            await self._send_connect(ws)
        else:
            # e.g. bad password — bail out so the backoff loop retries later.
            raise RuntimeError(f"refused: {', '.join(errors) or 'unknown'}")

    async def _on_print_json(self, packet: dict[str, Any]) -> None:
        ptype = packet.get("type")
        if ptype not in ("ItemSend", "Hint"):
            return
        if not self._slot_by_number:
            return  # not fully connected yet

        item = packet.get("item") or {}
        receiving = packet.get("receiving")
        sender_number = item.get("player")
        item_id = item.get("item")
        location_id = item.get("location")
        flags = int(item.get("flags") or 0)

        receiver_info = self._slot_by_number.get(receiving, {})
        sender_info = self._slot_by_number.get(sender_number, {})
        item_name = self._datapackage.item_name(receiver_info.get("game"), item_id)
        location_name = self._datapackage.location_name(sender_info.get("game"), location_id)
        sender_name = sender_info.get("name")
        receiver_name = receiver_info.get("name")

        for slot in self.get_slots():
            number = self._name_to_number.get(slot.get("slot_name"))
            if number is None:
                continue
            if ptype == "Hint":
                # Only surface hints where this slot is the receiver of the item.
                if receiving != number:
                    continue
                event_type = "hint"
            elif receiving == number:
                event_type = "received"
            elif sender_number == number:
                event_type = "sent"
            else:
                continue

            event = TrackerEvent(
                event_type=event_type,
                item_name=item_name,
                location_name=location_name,
                sender_name=sender_name,
                receiver_name=receiver_name,
                flags=flags,
            )
            await self.on_event(self.room, slot, event)
