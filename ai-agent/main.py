import os
from fastapi import FastAPI

app = FastAPI(title="PolyPilot AI Agent")


@app.get("/health")
def health():
    """
    Hello world endpoint.
    Phase 4: will also verify LLM API keys and RabbitMQ connectivity.
    """
    return {
        "service": "ai-agent",
        "status": "ok",
        "anthropic_configured": bool(os.getenv("ANTHROPIC_API_KEY", "").strip("replace_me")),
        "rabbitmq_host": os.getenv("RABBITMQ_HOST", "not set"),
    }


# ── Phase 4 stubs (not wired yet) ────────────────────────────────────────────

@app.post("/ai/analyze")
def analyze(market_id: str):
    """
    Phase 4: triggers the LangGraph pipeline for a given market.
    Fetches news → summarizes → scores sentiment → publishes result to RabbitMQ.
    The orchestrator calls this fire-and-forget; result arrives via the queue.
    """
    return {"detail": "not implemented yet", "market_id": market_id}
