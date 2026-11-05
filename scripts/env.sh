#!/usr/bin/env bash
# Source this file from Bash. Tool paths and caches stay outside the checkout.
NURAYA_TOOLS="${NURAYA_TOOLS:-/workspace/.tools}"
export JAVA_HOME="${NURAYA_JAVA_HOME:-$NURAYA_TOOLS/jdk-17.0.14+7}"
export ANDROID_HOME="${NURAYA_ANDROID_HOME:-$NURAYA_TOOLS/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export ANDROID_USER_HOME="$NURAYA_TOOLS/android-user"
export ANDROID_EMULATOR_HOME="$ANDROID_USER_HOME"
export ANDROID_AVD_HOME="$ANDROID_USER_HOME/avd"
export GRADLE_USER_HOME="$NURAYA_TOOLS/gradle-home"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/19.0/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$NURAYA_TOOLS/gradle-8.11.1/bin:$PATH"

# Java does not automatically use HTTPS_PROXY. Use the provided proxy without
# extracting credentials. TLS validation stays on with the supplied public CA.
NURAYA_JAVA_NETWORK_OPTS=""
if [[ -n "${HTTPS_PROXY:-}" ]]; then
    read -r NURAYA_PROXY_HOST NURAYA_PROXY_PORT < <(python3 - <<'PY'
import os, urllib.parse
u = urllib.parse.urlsplit(os.environ['HTTPS_PROXY'])
print(u.hostname or '', u.port or 8080)
PY
)
    NURAYA_JAVA_NETWORK_OPTS="-Dhttps.proxyHost=$NURAYA_PROXY_HOST -Dhttps.proxyPort=$NURAYA_PROXY_PORT -Dhttp.proxyHost=$NURAYA_PROXY_HOST -Dhttp.proxyPort=$NURAYA_PROXY_PORT -Dhttp.nonProxyHosts=localhost|127.*"
fi
if [[ -f "$NURAYA_TOOLS/java-cacerts" ]]; then
    NURAYA_JAVA_NETWORK_OPTS="$NURAYA_JAVA_NETWORK_OPTS -Djavax.net.ssl.trustStore=$NURAYA_TOOLS/java-cacerts"
fi
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} $NURAYA_JAVA_NETWORK_OPTS"
