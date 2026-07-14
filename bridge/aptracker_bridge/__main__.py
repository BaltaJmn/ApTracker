from __future__ import annotations

import asyncio
import logging

from .config import Config
from .tracker import Tracker


def _load_dotenv() -> None:
    try:
        from dotenv import load_dotenv
    except ImportError:
        return
    load_dotenv()


def main() -> None:
    _load_dotenv()
    config = Config.from_env()
    logging.basicConfig(
        level=getattr(logging, config.log_level, logging.INFO),
        format="%(asctime)s %(levelname)-7s %(name)s: %(message)s",
    )
    tracker = Tracker(config)
    try:
        asyncio.run(tracker.run())
    except KeyboardInterrupt:
        logging.getLogger(__name__).info("shutting down")


if __name__ == "__main__":
    main()
