from __future__ import annotations

import logging
from typing import Any

import httpx

log = logging.getLogger(__name__)


class SupabaseREST:
    """Thin async wrapper over Supabase's PostgREST endpoint.

    Uses the service-role key, so RLS is bypassed: the bridge reads every user's
    rooms/slots and inserts activity rows on their behalf.
    """

    def __init__(self, url: str, service_key: str) -> None:
        self._base = f"{url}/rest/v1"
        self._headers = {
            "apikey": service_key,
            "Authorization": f"Bearer {service_key}",
            "Content-Type": "application/json",
        }
        self._client = httpx.AsyncClient(timeout=30)

    async def fetch_rooms(self) -> list[dict[str, Any]]:
        resp = await self._client.get(
            f"{self._base}/rooms", headers=self._headers, params={"select": "*"}
        )
        resp.raise_for_status()
        return resp.json()

    async def fetch_slots(self) -> list[dict[str, Any]]:
        resp = await self._client.get(
            f"{self._base}/slots", headers=self._headers, params={"select": "*"}
        )
        resp.raise_for_status()
        return resp.json()

    async def insert_activity(self, row: dict[str, Any]) -> None:
        headers = dict(self._headers, Prefer="return=minimal")
        resp = await self._client.post(
            f"{self._base}/activity", headers=headers, json=row
        )
        resp.raise_for_status()

    async def fetch_push_tokens(self) -> list[dict[str, Any]]:
        resp = await self._client.get(
            f"{self._base}/push_tokens",
            headers=self._headers,
            params={"select": "token,platform,user_id"},
        )
        resp.raise_for_status()
        return resp.json()

    async def delete_push_token(self, token: str) -> None:
        headers = dict(self._headers, Prefer="return=minimal")
        resp = await self._client.delete(
            f"{self._base}/push_tokens", headers=headers, params={"token": f"eq.{token}"}
        )
        resp.raise_for_status()

    async def aclose(self) -> None:
        await self._client.aclose()
