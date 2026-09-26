# ── dev (docker-compose.yaml uses build.target: dev) ───────────────────────────
FROM python:3.12-slim AS dev

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8002

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8002", "--reload"]

# ── prod (docker-compose.prod.yaml uses build.target: prod) ────────────────────
FROM python:3.12-slim AS prod

WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

RUN addgroup --system polypilot && adduser --system --ingroup polypilot polypilot

# requirements.txt copied first so this layer caches across source-only changes.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .
RUN chown -R polypilot:polypilot /app
USER polypilot

EXPOSE 8002

# No curl in this image — urllib is stdlib, keeping the prod image minimal.
HEALTHCHECK --interval=10s --timeout=5s --retries=5 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8002/health', timeout=3)" || exit 1

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8002"]
