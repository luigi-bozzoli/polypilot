"""Logging setup shared by all PolyPilot Python services.

Kept byte-identical across ``auth-service`` and ``ai-agent`` on purpose — see the
repo-root ``CLAUDE.md``. If you change it here, change it there too.
"""

from __future__ import annotations

from logging.config import dictConfig

_LOG_FORMAT = "%(asctime)s %(levelname)-8s %(name)s : %(message)s"
_DATE_FORMAT = "%Y-%m-%dT%H:%M:%S%z"


def configure_logging(level: str = "INFO") -> None:
    """Install a single stdout handler and route uvicorn's loggers through it.

    Idempotent: safe to call again under ``uvicorn --reload``.
    """
    normalized = level.upper()
    dictConfig(
        {
            "version": 1,
            "disable_existing_loggers": False,
            "formatters": {
                "standard": {"format": _LOG_FORMAT, "datefmt": _DATE_FORMAT},
            },
            "handlers": {
                "console": {
                    "class": "logging.StreamHandler",
                    "formatter": "standard",
                },
            },
            "root": {"handlers": ["console"], "level": normalized},
            "loggers": {
                name: {"handlers": ["console"], "level": normalized, "propagate": False}
                for name in ("uvicorn", "uvicorn.error", "uvicorn.access")
            },
        }
    )
