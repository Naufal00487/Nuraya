#!/usr/bin/env python3
"""Verify ayah catalog integrity; optionally download/decode real verse and timed audio.

Host checks do not claim Android playback or every recording was listened to.
"""
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import argparse
import gzip
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile
import urllib.request

ROOT = Path(__file__).resolve().parents[1]


def verify_catalog():
    catalog = json.loads((ROOT / 'app/src/main/assets/verse_reciters.json').read_text())
    raw = gzip.decompress((ROOT / 'docs/sources/islamic-network-ayah-manifest.json.gz').read_bytes())
    assert hashlib.sha256(raw).hexdigest() == catalog['metadata']['manifestSha256']
    manifest = json.loads(raw)
    files = {(int(rate['name']), edition['name']): {f['name'] for f in edition.get('contents', []) if f['type'] == 'file'}
             for root in manifest for rate in root.get('contents', []) if rate.get('name', '').isdigit()
             for edition in rate.get('contents', []) if edition.get('type') == 'directory'}
    expected = {f'{number}.mp3' for number in range(1, 6237)}
    entries = catalog['reciters']
    assert len(entries) == len({r['id'] for r in entries})
    for reciter in entries:
        assert files[(reciter['bitrateKbps'], reciter['id'])] == expected
        assert bool(reciter['timingFolder']) == reciter['supportsWordSync']
        if reciter['supportsWordSync']:
            assert reciter['id'] == 'ar.alafasy' and reciter['timingFolder'] == 'alafasy'
    print(f'Verified {len(entries)} ayah reciters, each with all 6,236 exact source filenames.', flush=True)
    return entries


def sample(request):
    try:
        with urllib.request.urlopen(urllib.request.Request(request['url'], headers={'User-Agent': 'Nuraya-verse-audio-validation/1.1'}), timeout=45) as response:
            assert response.status == 200 and response.geturl().startswith('https://')
            data = response.read(12 * 1024 * 1024 + 1)
            assert 0 < len(data) <= 12 * 1024 * 1024
            if response.headers.get('Content-Length') is not None:
                assert len(data) == int(response.headers['Content-Length'])
        with tempfile.TemporaryDirectory(prefix='nuraya-verse-check-') as temporary:
            path = Path(temporary) / 'source.mp3'
            path.write_bytes(data)
            result = subprocess.run(['ffprobe', '-v', 'error', '-select_streams', 'a', '-show_entries',
                'format=duration:stream=codec_name,sample_rate,channels', '-of', 'json', str(path)],
                check=True, capture_output=True, text=True, timeout=30)
            probe = json.loads(result.stdout)
            assert probe['streams'] and all(s['codec_name'] == 'mp3' for s in probe['streams'])
            duration = float(probe['format']['duration'])
            assert 0.2 < duration < 600
            if request.get('lastClipMs'):
                assert request['lastClipMs'] <= duration * 1000 + 100
            subprocess.run(['ffmpeg', '-nostdin', '-v', 'error', '-xerror', '-i', str(path), '-map', '0:a:0', '-f', 'null', '-'],
                check=True, capture_output=True, timeout=60)
        return {**request, 'httpStatus': 200, 'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest(),
                'durationSeconds': duration, 'fullHostDecode': 'passed'}
    except Exception as error:
        detail = getattr(error, 'stderr', None) or str(error)
        if isinstance(detail, bytes):
            detail = detail.decode('utf-8', 'replace')
        return {**request, 'error': detail[:2000]}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--online', action='store_true')
    args = parser.parse_args()
    entries = verify_catalog()
    if not args.online:
        return
    requests = [{'kind': 'individual-ayah', 'reciter': r['id'], 'reference': '1:1',
                 'url': f"https://cdn.islamic.network/quran/audio/{r['bitrateKbps']}/{r['id']}/1.mp3"} for r in entries]
    for surah in (1, 112, 114):
        chapter = json.loads((ROOT / f'app/src/main/assets/audio_timing/alafasy/{surah}.json').read_text())
        requests.append({'kind': 'paired-timed-chapter', 'reciter': 'ar.alafasy', 'surah': surah,
                         'url': chapter['audioUrl'], 'lastClipMs': max(v['clipTo'] for v in chapter['verses'])})
    quran = json.loads((ROOT / 'app/src/main/assets/quran.json').read_text())
    for surah, ayah in ((14, 17), (37, 130)):
        global_number = sum(s['ayahCount'] for s in quran['surahs'] if s['id'] < surah) + ayah
        requests.append({'kind': 'timing-fallback-ayah', 'reciter': 'ar.alafasy', 'reference': f'{surah}:{ayah}',
                         'url': f'https://cdn.islamic.network/quran/audio/128/ar.alafasy/{global_number}.mp3'})
    with ThreadPoolExecutor(max_workers=4) as pool:
        results = list(pool.map(sample, requests))
    failed = [r for r in results if 'error' in r]
    report = {'checkedAt': datetime.now(timezone.utc).isoformat(), 'reciterCount': len(entries), 'samples': results,
              'failedSamples': failed, 'scope': 'Complete HTTPS downloads and MP3 decode on host; not Android playback or every recording.'}
    destination = ROOT / 'artifacts/validation/verse-audio-1.1.0.json'
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(report, indent=2) + '\n')
    print(f'{len(results) - len(failed)} passed, {len(failed)} failed; report: {destination}')
    for result in failed:
        print(result['url'], result['error'])
    if failed:
        raise SystemExit(1)


if __name__ == '__main__':
    main()
