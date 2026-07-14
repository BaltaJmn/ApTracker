from __future__ import annotations

import asyncio
import json
import logging

import httpx

log = logging.getLogger(__name__)

FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging"


class FcmSender:
    """Sends native push to Android devices via the FCM HTTP v1 API.

    Uses a service-account key for OAuth2. Only constructed when
    FCM_SERVICE_ACCOUNT_FILE is configured, so google-auth is an optional dep.
    """

    def __init__(self, service_account_file: str) -> None:
        # Imported lazily so the bridge runs without google-auth when FCM is off.
        from google.oauth2 import service_account

        self._creds = service_account.Credentials.from_service_account_file(
            service_account_file, scopes=[FCM_SCOPE]
        )
        with open(service_account_file) as fh:
            self._project_id = json.load(fh)["project_id"]
        self._url = f"https://fcm.googleapis.com/v1/projects/{self._project_id}/messages:send"
        self._client = httpx.AsyncClient(timeout=15)
        log.info("FCM enabled for project %s", self._project_id)

    def _access_token(self) -> str:
        from google.auth.transport.requests import Request

        if not self._creds.valid:
            self._creds.refresh(Request())
        return self._creds.token

    async def send(self, token: str, title: str, body: str) -> str:
        """Returns 'ok', 'stale' (token dead — drop it), or 'error'."""
        try:
            access = await asyncio.to_thread(self._access_token)
        except Exception as exc:  # noqa: BLE001
            log.warning("FCM auth failed: %s", exc)
            return "error"
        payload = {
            "message": {
                "token": token,
                "data": {"title": title, "body": body},
                "android": {"priority": "high"},
            }
        }
        try:
            resp = await self._client.post(
                self._url, headers={"Authorization": f"Bearer {access}"}, json=payload
            )
        except Exception as exc:  # noqa: BLE001
            log.warning("FCM send error: %s", exc)
            return "error"
        if resp.status_code < 300:
            return "ok"
        if resp.status_code == 404 or "UNREGISTERED" in resp.text:
            return "stale"
        log.warning("FCM send failed %s: %s", resp.status_code, resp.text[:200])
        return "error"

    async def aclose(self) -> None:
        await self._client.aclose()
