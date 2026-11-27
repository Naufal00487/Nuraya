#!/usr/bin/env python3
"""Pinned model and existing Android recording for native simulator tests only."""
import hashlib, json, pathlib, subprocess, urllib.request
ROOT=pathlib.Path(__file__).resolve().parents[2]
DEST=ROOT/'ios/.build/TestFixtures'
DEST.mkdir(parents=True,exist_ok=True)
spec=json.loads((ROOT/'ios/Nuraya/Resources/Content/quran_model.json').read_text())
model=DEST/'model.bin'
if not model.exists():
    with urllib.request.urlopen(spec['url'],timeout=120) as response:
        model.write_bytes(response.read())
data=model.read_bytes()
assert len(data)==spec['bytes'] and hashlib.sha256(data).hexdigest()==spec['sha256']
subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-i',str(ROOT/'app/src/androidTest/assets/alafasy1.mp3'),'-t','12','-map','0:a:0','-ar','16000','-ac','1','-f','f32le',str(DEST/'quran.pcm')],check=True)
print('Pinned native test model verified; existing Android test recording converted to PCM.')

# Exact reader recordings, distinct from the chapter recording above. Pin them
# so tests cannot silently use a different recitation or a changed source file.
reader_files=json.loads((ROOT/'ios/scripts/reader-audio-fixtures.json').read_text())
for fixture in reader_files:
    file=DEST/fixture['file']
    if not file.exists():
        with urllib.request.urlopen(fixture['url'],timeout=120) as response:
            file.write_bytes(response.read())
    data=file.read_bytes()
    assert len(data)==fixture['bytes'] and hashlib.sha256(data).hexdigest()==fixture['sha256'],fixture['file']
(DEST/'reader-audio.json').write_text(json.dumps(reader_files))
print('Four pinned reader recordings verified for continuous and per-verse playback tests.')
