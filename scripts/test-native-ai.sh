#!/usr/bin/env bash
set -euo pipefail
NURAYA_PROJECT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
source "$NURAYA_PROJECT/scripts/env.sh"
NURAYA_MODEL_PATH="${1:?Pass the verified converted Quran GGML model path}"
NURAYA_NATIVE_TEST_BUILD="$NURAYA_TOOLS/nuraya-native-host"
NURAYA_CMAKE="$ANDROID_HOME/cmake/3.22.1/bin/cmake"
"$NURAYA_CMAKE" -S "$NURAYA_PROJECT/app/src/main/cpp" -B "$NURAYA_NATIVE_TEST_BUILD" -G Ninja \
  -DCMAKE_MAKE_PROGRAM="$ANDROID_HOME/cmake/3.22.1/bin/ninja" -DJAVA_HOME="$JAVA_HOME" -DCMAKE_BUILD_TYPE=Release
"$NURAYA_CMAKE" --build "$NURAYA_NATIVE_TEST_BUILD" -j 4
mkdir -p "$NURAYA_NATIVE_TEST_BUILD/java"
# First 12 seconds of the existing test recording; no expected words are passed to inference.
ffmpeg -hide_banner -loglevel error -y -i "$NURAYA_PROJECT/app/src/androidTest/assets/alafasy1.mp3" \
  -t 12 -map 0:a:0 -ar 16000 -ac 1 -f f32le "$NURAYA_NATIVE_TEST_BUILD/quran-test.pcm"
"$JAVA_HOME/bin/javac" -d "$NURAYA_NATIVE_TEST_BUILD/java" "$NURAYA_PROJECT/scripts/native-test/QuranWhisperEngine.java"
"$JAVA_HOME/bin/java" -cp "$NURAYA_NATIVE_TEST_BUILD/java" com.nuraya.app.recitation.ai.QuranWhisperEngine \
  "$NURAYA_NATIVE_TEST_BUILD/libnuraya_quran_ai.so" "$NURAYA_MODEL_PATH" "$NURAYA_NATIVE_TEST_BUILD/quran-test.pcm"
