"""ClobAuth relay request/response models.

The request is the ``{address, timestamp, nonce}`` triple the orchestrator's
wallet-connect challenge hands back, plus the client-side EIP-712 signature.
The response is the L2 API credential triple Polymarket derives.
"""

from __future__ import annotations

from pydantic import BaseModel


class VerifyAndDeriveRequest(BaseModel):
    address: str
    signature: str
    timestamp: str
    nonce: str


class L2CredentialsResponse(BaseModel):
    api_key: str | None
    secret: str | None
    passphrase: str | None
