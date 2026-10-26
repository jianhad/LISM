#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
RELEASE_DIR=$(dirname "$SCRIPT_DIR")
cd "$RELEASE_DIR"

: "${DB_URL:?请配置 DB_URL}"
: "${DB_USERNAME:?请配置 DB_USERNAME}"
: "${DB_PASSWORD:?请配置 DB_PASSWORD}"
: "${JWT_SECRET:?请配置 JWT_SECRET}"

exec java -jar ./backend/lims-backend.jar

