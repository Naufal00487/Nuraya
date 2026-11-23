#!/usr/bin/env python3
"""Reject an app bundle missing or changing the canonical Android assets."""
import hashlib,json,pathlib,plistlib,sys
bundle=pathlib.Path(sys.argv[1])
root=bundle/'Content'
manifest=json.loads((root/'android_assets_sha256.json').read_text())
assert len(manifest)==1740
for relative,digest in manifest.items():
    file=root/relative
    assert file.is_file(),f'Missing bundled source: {relative}'
    assert hashlib.sha256(file.read_bytes()).hexdigest()==digest,f'Changed source: {relative}'
info=plistlib.loads((bundle/'Info.plist').read_bytes())
assert info['CFBundleIdentifier']=='com.nuraya.app'
assert float(info['MinimumOSVersion'])<=18.0
assert 1 in info['UIDeviceFamily']
assert 'audio' in info['UIBackgroundModes']
assert info['UILaunchStoryboardName']=='LaunchScreen'
assert (bundle/'LaunchScreen.storyboardc').is_dir(),'Missing system launch artwork'
for name in ['Assets.car','PrivacyInfo.xcprivacy','adhan_short.caf','Content/amiri_quran.ttf','Content/hadith/hadith.sqlite']:
    assert (bundle/name).is_file(),f'Missing app resource: {name}'
assert not (bundle/'TestFixtures').exists(),'Test model leaked into application bundle'
print(f'PASS: {len(manifest)} original assets verified inside {bundle.name}; required native resources present.')
