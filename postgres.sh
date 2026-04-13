#!/usr/bin/env bash
set -euo pipefail

# Loads POSTGRES_* from .env if present (gitignored). Fails fast if missing.
if [[ -f .env ]]; then
	set -a
	# shellcheck disable=SC1091
	source .env
	set +a
fi

: "${POSTGRES_USER:?POSTGRES_USER must be set (copy .env.example to .env)}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD must be set (copy .env.example to .env)}"
: "${POSTGRES_DB:?POSTGRES_DB must be set (copy .env.example to .env)}"

podman run \
	--name appdb \
	-e POSTGRES_PASSWORD="${POSTGRES_PASSWORD}" \
	-e POSTGRES_USER="${POSTGRES_USER}" \
	-e POSTGRES_DB="${POSTGRES_DB}" \
	-p 127.0.0.1:5432:5432 \
	-d postgres
