"""SIWE signature-recovery tests — a throwaway key signs a real EIP-4361-shaped
message and we assert the service recovers the same address. No real network,
no real wallet: eth_account.Account.create() generates a fresh keypair locally.
"""

from __future__ import annotations

from eth_account import Account
from eth_account.messages import encode_defunct
from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def _hex_signature(signed) -> str:
    raw = signed.signature.hex()
    return raw if raw.startswith("0x") else f"0x{raw}"


def test_siwe_verify_recovers_the_signing_address():
    account = Account.create()
    message = (
        "example.com wants you to sign in with your Ethereum account:\n"
        f"{account.address}\n\nSign in to PolyPilot\n\nNonce: abc123"
    )
    signed = account.sign_message(encode_defunct(text=message))

    response = client.post(
        "/auth/siwe/verify",
        json={"message": message, "signature": _hex_signature(signed)},
    )

    assert response.status_code == 200
    assert response.json()["address"] == account.address


def test_siwe_verify_malformed_signature_returns_the_service_error_shape():
    response = client.post(
        "/auth/siwe/verify",
        json={"message": "any message", "signature": "0xnotarealsignature"},
    )

    assert response.status_code == 400
    # SiweService appends the underlying eth_account exception text, so this only
    # pins the stable prefix (the service's error "shape"), not the exact message.
    assert response.json()["detail"].startswith("Signature recovery failed")
