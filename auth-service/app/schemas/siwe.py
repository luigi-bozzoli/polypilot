"""SIWE (EIP-4361) signature-recovery request/response models."""

from __future__ import annotations

from pydantic import BaseModel


class SiweVerifyRequest(BaseModel):
    message: str
    """The exact EIP-4361 message string that was signed."""

    signature: str
    """``0x…`` signature produced by ``personal_sign``."""


class SiweVerifyResponse(BaseModel):
    address: str
    """EIP-55 checksummed address recovered from the signature."""
