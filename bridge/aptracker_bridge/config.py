from __future__ import annotations

import os
from dataclasses import dataclass


def _get(name: str, default: str | None = None, required: bool = False) -> str | None:
    value = os.environ.get(name, default)
    if required and not value:
        raise SystemExit(
            f"Missing required environment variable: {name}. "
            f"Copy bridge/.env.example to bridge/.env and fill it in."
        )
    return value


@dataclass(frozen=True)
class Config:
    supabase_url: str
    supabase_service_key: str
    ntfy_base_url: str
    ntfy_default_topic: str | None
    sync_interval: int
    reconnect_max_backoff: int
    tls_insecure: bool
    log_level: str
    fcm_service_account_file: str | None

    @classmethod
    def from_env(cls) -> "Config":
        return cls(
            supabase_url=_get("SUPABASE_URL", required=True).rstrip("/"),  # type: ignore[union-attr]
            supabase_service_key=_get("SUPABASE_SERVICE_ROLE_KEY", required=True),  # type: ignore[assignment]
            ntfy_base_url=_get("NTFY_BASE_URL", "https://ntfy.sh").rstrip("/"),  # type: ignore[union-attr]
            ntfy_default_topic=_get("NTFY_DEFAULT_TOPIC"),
            sync_interval=int(_get("SYNC_INTERVAL_SECONDS", "60")),  # type: ignore[arg-type]
            reconnect_max_backoff=int(_get("RECONNECT_MAX_BACKOFF_SECONDS", "60")),  # type: ignore[arg-type]
            tls_insecure=str(_get("AP_TLS_INSECURE", "false")).lower() == "true",
            log_level=str(_get("LOG_LEVEL", "INFO")).upper(),
            fcm_service_account_file=_get("FCM_SERVICE_ACCOUNT_FILE"),
        )
