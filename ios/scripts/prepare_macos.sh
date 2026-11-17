#!/bin/bash
set -euo pipefail
script_dir="$(cd "$(dirname "$0")" && pwd)"
ios_dir="$(cd "$script_dir/.." && pwd)"
python3 "$script_dir/prepare_assets.py"
mkdir -p "$ios_dir/Nuraya/Resources/Assets.xcassets/AppIcon.appiconset"
# Resource compilation: resize the unchanged Android logo to Apple's required icon size.
sips -s format png -z 1024 1024 "$ios_dir/../app/src/main/assets/brand/logo.jpg" --out "$ios_dir/Nuraya/Resources/Assets.xcassets/AppIcon.appiconset/AppIcon.png" >/dev/null
cat > "$ios_dir/Nuraya/Resources/Assets.xcassets/AppIcon.appiconset/Contents.json" <<'JSON'
{"images":[{"filename":"AppIcon.png","idiom":"universal","platform":"ios","size":"1024x1024"}],"info":{"author":"xcode","version":1}}
JSON
# Local-notification audio must be <30 seconds on iOS. The full original remains in Content.
ffmpeg -hide_banner -loglevel error -y -i "$ios_dir/../app/src/main/res/raw/adhan_mecca.mp3" -t 28 -ac 1 -ar 22050 -c:a pcm_s16le "$ios_dir/Nuraya/Resources/adhan_short.caf"
cp "$ios_dir/../app/src/main/res/raw/adhan_mecca.mp3" "$ios_dir/Nuraya/Resources/Content/adhan_mecca.mp3"
cd "$ios_dir"
xcodegen generate
