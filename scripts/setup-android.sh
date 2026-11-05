#!/usr/bin/env bash
set -euo pipefail
NURAYA_PROJECT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
NURAYA_TOOLS="${NURAYA_TOOLS:-/workspace/.tools}"
mkdir -p "$NURAYA_TOOLS/downloads" "$NURAYA_TOOLS/android-sdk/cmdline-tools" "$NURAYA_TOOLS/android-user" "$NURAYA_TOOLS/gradle-home"
download() {
    local url="$1" path="$2" checksum="$3" algorithm="$4"
    if [[ ! -f "$path" ]]; then
        curl --fail --location --retry 2 --proto '=https' --tlsv1.2 "$url" -o "$path.part"
        mv "$path.part" "$path"
    fi
    printf '%s  %s\n' "$checksum" "$path" | "$algorithm" -c -
}
if [[ ! -x "$NURAYA_TOOLS/jdk-17.0.14+7/bin/javac" ]]; then
    download 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.14%2B7/OpenJDK17U-jdk_x64_linux_hotspot_17.0.14_7.tar.gz' "$NURAYA_TOOLS/downloads/jdk17.tar.gz" a3af83983fb94dd7d11b13ba2dba0fb6819dc2caaf87e6937afd22ad4680ae9a sha256sum
    tar -xzf "$NURAYA_TOOLS/downloads/jdk17.tar.gz" -C "$NURAYA_TOOLS"
fi
if [[ ! -x "$NURAYA_TOOLS/gradle-8.11.1/bin/gradle" ]]; then
    download 'https://services.gradle.org/distributions/gradle-8.11.1-bin.zip' "$NURAYA_TOOLS/downloads/gradle.zip" f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6 sha256sum
    unzip -q "$NURAYA_TOOLS/downloads/gradle.zip" -d "$NURAYA_TOOLS"
fi
if [[ ! -x "$NURAYA_TOOLS/android-sdk/cmdline-tools/19.0/bin/sdkmanager" ]]; then
    # SHA-1 is the checksum published in Google's HTTPS SDK repository manifest.
    download 'https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip' "$NURAYA_TOOLS/downloads/android-cmdline.zip" 5fdcc763663eefb86a5b8879697aa6088b041e70 sha1sum
    unzip -q "$NURAYA_TOOLS/downloads/android-cmdline.zip" -d "$NURAYA_TOOLS/android-sdk/cmdline-tools"
    mv "$NURAYA_TOOLS/android-sdk/cmdline-tools/cmdline-tools" "$NURAYA_TOOLS/android-sdk/cmdline-tools/19.0"
fi
if [[ -n "${CODEX_PROXY_CERT:-}" && -f "$CODEX_PROXY_CERT" ]]; then
    cp "$NURAYA_TOOLS/jdk-17.0.14+7/lib/security/cacerts" "$NURAYA_TOOLS/java-cacerts"
    "$NURAYA_TOOLS/jdk-17.0.14+7/bin/keytool" -importcert -noprompt -alias nuraya-cloud-proxy -file "$CODEX_PROXY_CERT" -keystore "$NURAYA_TOOLS/java-cacerts" -storepass changeit
fi
source "$NURAYA_PROJECT/scripts/env.sh"
# Accept the Android SDK licenses required by the explicitly requested build.
set +o pipefail
yes | sdkmanager --licenses >/dev/null
license_result="${PIPESTATUS[1]}"
set -o pipefail
[[ "$license_result" -eq 0 ]]
sdkmanager 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0' 'ndk;27.2.12479018' 'cmake;3.22.1'
if [[ ! -f "$NURAYA_PROJECT/local.properties" ]]; then
    printf 'sdk.dir=%s\n' "$ANDROID_HOME" > "$NURAYA_PROJECT/local.properties"
fi
java -version
gradle --version
printf 'Android toolchain ready. Build with: scripts/gradle.sh assembleDebug testDebugUnitTest lintDebug\n'
