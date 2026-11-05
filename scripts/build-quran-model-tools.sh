#!/usr/bin/env bash
# Build pinned host-only conversion/inspection tools. Does not download model weights.
set -euo pipefail
NURAYA_PROJECT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
source "$NURAYA_PROJECT/scripts/env.sh"
NURAYA_WHISPER_SOURCE="$NURAYA_TOOLS/whisper.cpp"
NURAYA_WHISPER_REVISION=a8d002cfd879315632a579e73f0148d06959de36
if [[ ! -d "$NURAYA_WHISPER_SOURCE" ]]; then
    git clone --depth 1 --branch v1.7.6 https://github.com/ggml-org/whisper.cpp.git "$NURAYA_WHISPER_SOURCE"
fi
if [[ "$(git -C "$NURAYA_WHISPER_SOURCE" rev-parse HEAD)" != "$NURAYA_WHISPER_REVISION" ]]; then
    printf 'Unexpected whisper.cpp revision; use a separate NURAYA_TOOLS directory.\n' >&2
    exit 1
fi
NURAYA_CMAKE="$ANDROID_HOME/cmake/3.22.1/bin/cmake"
"$NURAYA_CMAKE" -S "$NURAYA_WHISPER_SOURCE" -B "$NURAYA_TOOLS/whisper-host" -G Ninja \
    -DCMAKE_MAKE_PROGRAM="$ANDROID_HOME/cmake/3.22.1/bin/ninja" -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_SHARED_LIBS=OFF -DWHISPER_BUILD_TESTS=OFF -DWHISPER_BUILD_SERVER=OFF \
    -DWHISPER_CURL=OFF -DGGML_NATIVE=OFF -DGGML_OPENMP=OFF
"$NURAYA_CMAKE" --build "$NURAYA_TOOLS/whisper-host" --target quantize whisper-cli -j 4
printf 'Host model tools ready in %s/whisper-host/bin\n' "$NURAYA_TOOLS"
