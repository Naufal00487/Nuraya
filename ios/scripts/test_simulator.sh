#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
runtime=$(xcrun simctl list runtimes --json | python3 -c 'import json,sys; r=[x for x in json.load(sys.stdin)["runtimes"] if x.get("isAvailable") and x["identifier"].startswith("com.apple.CoreSimulator.SimRuntime.iOS-18-")]; assert r,"Install an iOS 18 simulator runtime for iPhone XR"; print(sorted(r,key=lambda x:tuple(map(int,x["version"].split("."))))[-1]["identifier"])')
device=$(xcrun simctl create 'Nuraya iPhone XR' com.apple.CoreSimulator.SimDeviceType.iPhone-XR "$runtime")
trap 'xcrun simctl shutdown "$device" >/dev/null 2>&1 || true; xcrun simctl delete "$device" >/dev/null 2>&1 || true' EXIT
xcrun simctl boot "$device"
xcrun simctl bootstatus "$device" -b
xcrun simctl status_bar "$device" override --time '09:41' --batteryState charged --batteryLevel 100
xcodebuild -project Nuraya.xcodeproj -scheme Nuraya -configuration Debug \
  -sdk iphonesimulator -destination "platform=iOS Simulator,id=$device" \
  -derivedDataPath DerivedData -resultBundlePath .build/SimulatorTests.xcresult \
  -parallel-testing-enabled NO CODE_SIGNING_ALLOWED=NO test
