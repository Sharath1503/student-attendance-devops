#!/usr/bin/env bash
# Starts the packaged app using the values from ./.env  (copy .env.example to .env and edit it first).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [ ! -f "$ROOT/.env" ]; then
  echo "Missing .env - run:  cp .env.example .env   then edit the CHANGE_ME values" >&2
  exit 1
fi
set -a
# shellcheck disable=SC1091
source "$ROOT/.env"
set +a
exec java -jar "$ROOT/backend/target/student-attendance.jar"
