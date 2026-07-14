from __future__ import annotations

import logging
from typing import Sequence

import httpx

log = logging.getLogger(__name__)


class Ntfy:
    """Publishes push notifications via ntfy's JSON endpoint (UTF-8 safe)."""

    def __init__(self, base_url: str) -> None:
        self._base = base_url
        self._client = httpx.AsyncClient(timeout=15)

    async def publish(
        self,
        topic: str | None,
        title: str,
        message: str,
        tags: Sequence[str] | None = None,
        priority: int | None = None,
    ) -> None:
        if not topic:
            log.warning("No ntfy topic configured; dropping notification: %s", title)
            return
        payload: dict = {"topic": topic, "message": message}
        if title:
            payload["title"] = title
        if tags:
            payload["tags"] = list(tags)
        if priority:
            payload["priority"] = priority
        try:
            resp = await self._client.post(self._base, json=payload)
            resp.raise_for_status()
        except Exception as exc:  # noqa: BLE001 - never let a push failure kill the bridge
            log.warning("ntfy publish to '%s' failed: %s", topic, exc)

    async def aclose(self) -> None:
        await self._client.aclose()
