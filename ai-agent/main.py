"""Uvicorn entrypoint shim.

The Docker image and docker-compose run ``uvicorn main:app``; the real
application lives in :mod:`app.main`.
"""

from app.main import app

__all__ = ["app"]
