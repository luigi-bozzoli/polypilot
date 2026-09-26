#!/bin/sh
# First-run setup: creates .env from .env.example with generated secrets.
# Idempotent — does nothing if .env already exists, so it's safe to re-run.
#
# Usage: ./scripts/init-env.sh

set -eu

script_dir=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
repo_root=$(CDPATH='' cd -- "$script_dir/.." && pwd)
env_file="$repo_root/.env"
example_file="$repo_root/.env.example"

if [ -f "$env_file" ]; then
    echo ".env already exists at $env_file — leaving it untouched."
    exit 0
fi

if ! command -v openssl >/dev/null 2>&1; then
    echo "openssl is required to generate secrets. Install it and re-run." >&2
    exit 1
fi

if [ ! -f "$example_file" ]; then
    echo ".env.example not found at $example_file" >&2
    exit 1
fi

cp "$example_file" "$env_file"
chmod 600 "$env_file"

# sed -i with an explicit backup suffix (no space before it) is portable across
# BSD sed (macOS) and GNU sed (Linux/CI). "|" is the delimiter since generated
# values are base64/hex and never contain it.
set_var() {
    key=$1
    value=$2
    sed -i.bak "s|^${key}=.*|${key}=${value}|" "$env_file"
    rm -f "$env_file.bak"
}

# Same format EncryptionService/JwtService require: 32 raw bytes, base64-encoded.
set_var JWT_SECRET "$(openssl rand -base64 32)"
set_var ENCRYPTION_KEY "$(openssl rand -base64 32)"

set_var DB_USER "polypilot"
set_var DB_PASSWORD "$(openssl rand -hex 16)"

set_var RABBITMQ_USER "polypilot"
set_var RABBITMQ_PASSWORD "$(openssl rand -hex 16)"

echo "Created $env_file with generated secrets (values not printed)."
echo "Before running 'docker compose up --build', open .env and set ANTHROPIC_API_KEY."
