#!/usr/bin/env python3
"""Freeze EQuran.id Indonesian transliterations and verify exact reference coverage.

`--refresh` downloads the 114 published API responses (four workers). Normal runs
rebuild solely from checked snapshots; `--verify` compares without changing files.
Canonical Arabic, verse numbering and translations are never written by this tool.
"""
from __future__ import annotations
import argparse
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import gzip
import hashlib
import json
from pathlib import Path
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
SOURCES = ROOT / 'docs/sources/latin'
ASSETS = ROOT / 'app/src/main/assets/latin'
QURAN = ROOT / 'app/src/main/assets/quran.json'
SOURCE = 'Transliterasi Latin Indonesia · EQuran.id API v2'
BASE = 'https://equran.id/api/v2/surat/'


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def encoded(data: object) -> bytes:
    return (json.dumps(data, ensure_ascii=False, separators=(',', ':')) + '\n').encode('utf-8')


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--refresh', action='store_true')
    parser.add_argument('--verify', action='store_true')
    args = parser.parse_args()
    if args.refresh and args.verify:
        parser.error('Use --refresh or --verify, not both')
    quran_bytes = QURAN.read_bytes()
    quran = json.loads(quran_bytes)
    counts = {row['id']: row['ayahCount'] for row in quran['surahs']}
    assert len(counts) == 114 and sum(counts.values()) == 6236
    before = sha(quran_bytes)
    if args.refresh:
        SOURCES.mkdir(parents=True, exist_ok=True)
        def fetch(surah: int) -> tuple[str, dict]:
            url = BASE + str(surah)
            request = urllib.request.Request(url, headers={'User-Agent': 'Nuraya-attributed-transliteration-import/1.3'})
            with urllib.request.urlopen(request, timeout=60) as response:
                assert response.status == 200
                raw = response.read(4 * 1024 * 1024 + 1)
            assert len(raw) <= 4 * 1024 * 1024
            parsed = json.loads(raw)
            assert parsed['code'] == 200 and parsed['data']['nomor'] == surah
            path = SOURCES / f'{surah}.json.gz'
            path.write_bytes(gzip.compress(raw, mtime=0))
            return str(surah), {'url': url, 'bytes': len(raw), 'sha256': sha(raw)}
        with ThreadPoolExecutor(max_workers=4) as pool:
            files = dict(pool.map(fetch, range(1, 115)))
        manifest = {'schemaVersion': 1, 'source': SOURCE, 'fetchedAt': datetime.now(timezone.utc).isoformat(),
                    'canonicalQuranSha256': before, 'files': files}
        (SOURCES / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
    manifest = json.loads((SOURCES / 'manifest.json').read_text())
    assert manifest['canonicalQuranSha256'] == before, 'Canonical corpus changed; review references before reimport'
    generated = {}
    references = []
    for surah in range(1, 115):
        raw = gzip.decompress((SOURCES / f'{surah}.json.gz').read_bytes())
        meta = manifest['files'][str(surah)]
        assert len(raw) == meta['bytes'] and sha(raw) == meta['sha256'], f'Source checksum mismatch for {surah}'
        response = json.loads(raw)
        assert response['code'] == 200
        data = response['data']
        assert data['nomor'] == surah and data['jumlahAyat'] == counts[surah]
        rows = data['ayat']
        assert [v['nomorAyat'] for v in rows] == list(range(1, counts[surah] + 1)), f'Incomplete or duplicate references: {surah}'
        verses = []
        for row in rows:
            text = row['teksLatin'].strip()
            assert isinstance(text, str) and text and len(text) < 20000 and '<' not in text, (surah, row['nomorAyat'])
            assert row['teksArab'].strip()
            verses.append({'ayah': row['nomorAyat'], 'latin': text})
            references.append((surah, row['nomorAyat']))
        generated[f'{surah}.json'] = encoded({'surah': surah, 'source': SOURCE, 'sourceUrl': meta['url'], 'verses': verses})
    assert references == [(v['surah'], v['ayah']) for v in quran['verses']], 'Latin references must match every canonical verse once'
    generated['index.json'] = encoded({'schemaVersion': 1, 'source': SOURCE, 'sourceUrl': 'https://equran.id/apidev/v2',
        'surahs': 114, 'verses': len(references), 'fetchedAt': manifest['fetchedAt'], 'canonicalQuranSha256': before})
    if not args.verify: ASSETS.mkdir(parents=True, exist_ok=True)
    for name, expected in generated.items():
        path = ASSETS / name
        if args.verify: assert path.read_bytes() == expected, f'Asset differs from attributed source: {name}'
        else: path.write_bytes(expected)
    assert sha(QURAN.read_bytes()) == before, 'Importer must not change canonical Quran'
    print(json.dumps({'verified': True, 'surahs': 114, 'verses': len(references), 'assetBytes': sum(map(len, generated.values())), 'canonicalQuranSha256': before}, indent=2))


if __name__ == '__main__':
    main()
