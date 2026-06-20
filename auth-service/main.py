import os
from fastapi import FastAPI

app = FastAPI(title="PolyPilot Auth Service")


@app.get("/health")
def health():
    """
    Hello world endpoint.
    Phase 2: this will also verify the private key is loaded and Web3 is reachable.
    """
    private_key_loaded = bool(os.getenv("POLYMARKET_PRIVATE_KEY", "").strip("replace_me"))
    return {
        "service": "auth-service",
        "status": "ok",
        "private_key_configured": private_key_loaded,
    }


# ── Phase 2 stubs (not wired yet) ────────────────────────────────────────────

@app.post("/auth/sign-order")
def sign_order():
    """
    Phase 2: accepts an unsigned order dict, returns an EIP-712 signed payload
    ready to POST to the Polymarket CLOB.
    """
    return {"detail": "not implemented yet"}


@app.post("/auth/credentials")
def get_credentials():
    """
    Phase 2: derives L2 API key + secret from the L1 wallet private key.
    Called once at orchestrator startup.
    """
    return {"detail": "not implemented yet"}
