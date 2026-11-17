#!/bin/bash
set -euo pipefail
script_dir="$(cd "$(dirname "$0")" && pwd)"
ios_dir="$(cd "$script_dir/.." && pwd)"
cd "$ios_dir"
xcodebuild -project Nuraya.xcodeproj -scheme Nuraya -configuration Release -sdk iphoneos -destination 'generic/platform=iOS' -derivedDataPath DerivedData CODE_SIGNING_ALLOWED=NO build
python3 scripts/verify_bundle.py DerivedData/Build/Products/Release-iphoneos/Nuraya.app
mkdir -p .build/Payload
# ditto preserves the app's bundle layout and framework permissions for local signing.
ditto DerivedData/Build/Products/Release-iphoneos/Nuraya.app .build/Payload/Nuraya.app
cd .build
zip -qr Nuraya-iOS-XR-unsigned.ipa Payload
