#!/usr/bin/env bash
set -euo pipefail
NURAYA_PROJECT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
source "$NURAYA_PROJECT/scripts/env.sh"
cd "$NURAYA_PROJECT"
exec ./gradlew --no-daemon "$@"
