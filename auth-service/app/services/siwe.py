"""SIWE ownership verification — cryptographic signature recovery only.

This layer deliberately does NOT check the nonce, domain, issued-at/expiration
window, chain id, or whether the address maps to a known user. The orchestrator
owns all SIWE / application policy.
"""

from __future__ import annotations

import logging

from eth_account import Account
from eth_account.messages import encode_defunct
from web3 import Web3

from app.exceptions import SignatureRecoveryError
from app.schemas.siwe import SiweVerifyRequest, SiweVerifyResponse

logger = logging.getLogger(__name__)


class SiweService:
    def recover_address(self, request: SiweVerifyRequest) -> SiweVerifyResponse:
        """Recover the checksummed address behind a ``personal_sign`` signature.

        Any malformed or unrecoverable input raises :class:`SignatureRecoveryError`
        (HTTP 400). ``eth_account`` raises a range of exception types for bad
        input, so the catch is intentionally broad.
        """
        try:
            recovered = Account.recover_message(
                encode_defunct(text=request.message),
                signature=request.signature,
            )
        except Exception as exc:
            logger.info("signature recovery failed [%s]", exc)
            raise SignatureRecoveryError(f"Signature recovery failed: {exc}") from exc

        return SiweVerifyResponse(address=Web3.to_checksum_address(recovered))
