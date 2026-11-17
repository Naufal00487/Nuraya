#!/bin/bash
set -euo pipefail
script_dir="$(cd "$(dirname "$0")" && pwd)"
ios_dir="$(cd "$script_dir/.." && pwd)"
platform="${PLATFORM_NAME:-iphoneos}"
if [[ "$platform" != 'iphoneos' && "$platform" != 'iphonesimulator' ]]; then
  echo "Unsupported Apple platform: $platform" >&2
  exit 1
fi
arch="${ARCHS:-${CURRENT_ARCH:-arm64}}"
if [[ "$arch" == 'undefined_arch' ]]; then
  if [[ "$platform" == 'iphonesimulator' ]]; then arch="$(uname -m)"; else arch='arm64'; fi
fi
arch_key="${arch// /-}"
cmake_arches="${arch// /;}"
build_dir="$ios_dir/.build-native/$platform/$arch_key"
lib_dir="$ios_dir/.build-native/$platform/lib"
if [[ -f "$lib_dir/libwhisper.a" && -f "$lib_dir/.arch-$arch_key" ]]; then exit 0; fi
cmake -S "$ios_dir/Native" -B "$build_dir" -G Ninja \
  -DCMAKE_SYSTEM_NAME=iOS -DCMAKE_OSX_SYSROOT="$(xcrun --sdk "$platform" --show-sdk-path)" \
  -DCMAKE_OSX_ARCHITECTURES="$cmake_arches" -DCMAKE_OSX_DEPLOYMENT_TARGET=16.0 \
  -DCMAKE_BUILD_TYPE=Release -DBUILD_SHARED_LIBS=OFF -DWHISPER_BUILD_TESTS=OFF \
  -DWHISPER_BUILD_EXAMPLES=OFF -DWHISPER_BUILD_SERVER=OFF -DWHISPER_CURL=OFF \
  -DGGML_NATIVE=OFF -DGGML_METAL=OFF -DGGML_ACCELERATE=ON -DGGML_OPENMP=OFF \
  -DGGML_BACKEND_DL=OFF -DGGML_CPU_ALL_VARIANTS=OFF -DGGML_BLAS=OFF
cmake --build "$build_dir" --config Release --parallel 3
mkdir -p "$lib_dir"
find "$build_dir" -name 'lib*.a' -exec cp '{}' "$lib_dir/" \;
touch "$lib_dir/.arch-$arch_key"
