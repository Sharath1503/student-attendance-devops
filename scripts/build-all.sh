#!/usr/bin/env bash
# One-command local build: React UI -> copied into Spring Boot -> tests -> single runnable jar.
# Usage:  ./scripts/build-all.sh            (runs "mvn clean verify": unit + integration tests)
#         ./scripts/build-all.sh -DskipTests (faster, skips tests - not for the pipeline)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "==> Building frontend"
cd "$ROOT/frontend"
npm ci --no-audit --no-fund
npm test
npm run build

echo "==> Copying UI into the backend"
rm -rf "$ROOT/backend/src/main/resources/static"
mkdir -p "$ROOT/backend/src/main/resources/static"
cp -r "$ROOT/frontend/dist/." "$ROOT/backend/src/main/resources/static/"

echo "==> Maven build"
cd "$ROOT/backend"
mvn -B clean verify "$@"

echo
echo "Done. Run it with:  java -jar backend/target/student-attendance.jar   (see README: set the environment variables first)"
