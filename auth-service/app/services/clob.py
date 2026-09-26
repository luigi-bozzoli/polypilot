"""ClobAuth relay verification.

Relays an already-signed EIP-712 ClobAuth message to Polymarket's L1 auth
endpoint (``/auth/api-key``) and returns the derived L2 API credentials.
Polymarket verifies the signature itself; this service holds no private key for
this flow.

Deprecated/frozen — see repo-root CLAUDE.md's Polymarket API policy. Do not
extend, add new callers, or build functionality on top of the credentials this
derives.
"""

from __future__ import annotations

import logging

import httpx
from web3 import Web3

from app.config import Settings
from app.exceptions import (
    InvalidWalletAddressError,
    UpstreamRejectedError,
    UpstreamUnreachableError,
)
from app.schemas.clob import L2CredentialsResponse, VerifyAndDeriveRequest

logger = logging.getLogger(__name__)


class ClobAuthService:
    def __init__(self, http_client: httpx.Client, settings: Settings) -> None:
        self._http = http_client
        self._settings = settings

    def verify_and_derive(self, request: VerifyAndDeriveRequest) -> L2CredentialsResponse:
        """Check the address, relay to the CLOB, and return the L2 triple.

        Raises :class:`InvalidWalletAddressError` (400) for a bad address,
        :class:`UpstreamUnreachableError` (502) on transport failure, and
        :class:`UpstreamRejectedError` (upstream status passed through) when the
        CLOB responds non-2xx.
        """
        try:
            address = Web3.to_checksum_address(request.address)
        except ValueError as exc:
            raise InvalidWalletAddressError() from exc

        url = f"{self._settings.clob_base_url}/auth/api-key"
        try:
            response = self._http.post(
                url,
                headers={
                    "POLY_ADDRESS": address,
                    "POLY_SIGNATURE": request.signature,
                    "POLY_TIMESTAMP": request.timestamp,
                    "POLY_NONCE": request.nonce,
                },
            )
        except httpx.RequestError as exc:
            raise UpstreamUnreachableError(f"Polymarket CLOB unreachable: {exc}") from exc

        if response.status_code >= 400:
            raise UpstreamRejectedError(response.text, status_code=response.status_code)

        body = response.json()
        return L2CredentialsResponse(
            api_key=body.get("apiKey") or body.get("api_key"),
            secret=body.get("secret"),
            passphrase=body.get("passphrase"),
        )
