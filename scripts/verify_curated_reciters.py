#!/usr/bin/env python3
"""Verify the 28 active person/provider mappings against the original 30-person snapshot; optionally recheck remote directories."""
from pathlib import Path
import argparse, json, re, urllib.request

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets'

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--live', action='store_true', help='Recheck complete EveryAyah directory listings')
    args = parser.parse_args()
    source = json.loads((ROOT / 'docs/sources/curated-reciters.json').read_text())
    chapter = {r['id']: r for r in json.loads((ASSETS / 'reciters.json').read_text())['qaris']}
    verse = {r['id']: r for r in json.loads((ASSETS / 'verse_reciters.json').read_text())['reciters']}
    quran = json.loads((ASSETS / 'quran.json').read_text())
    expected = {f"{v['surah']:03}{v['ayah']:03}.mp3" for v in quran['verses']}
    assert len(expected) == 6236
    kotlin = (ROOT / 'app/src/main/java/com/nuraya/app/media/CuratedReciters.kt').read_text()
    active = set(re.findall(r'Selection\("([^"]+)"', kotlin))
    assert len(active) == 28
    assert not active & {'ar.alijaber', 'ar.abdullahalmatrood'}
    rows = [row for row in source['reciters'] if row['chapterId'] in active]
    assert len(rows) == 28
    for key in ['name', 'chapterId', 'verseId', 'mp3QuranId']:
        assert len({r[key] for r in rows}) == 28, key
    for row in rows:
        c = chapter[row['chapterId']]; v = verse[row['verseId']]
        assert c['name'] == v['name'] == row['name']
        assert c['baseUrl'].rstrip('/') == row['moshaf']['server'].rstrip('/')
        assert c['numberPadding'] == 3
        assert set(map(int, row['moshaf']['surah_list'].split(','))) == set(range(1, 115))
        assert f'Selection("{c["id"]}", "{v["id"]}", "{row["name"]}")' in kotlin
        if v.get('ayahBaseUrl'):
            assert v['ayahBaseUrl'].rstrip('/') == 'https://everyayah.com/data/' + row['everyAyah']['subfolder']
            assert not v['supportsWordSync'] and v.get('timingFolder') is None
        evidence = row['individualVerseDirectoryEvidence']
        assert evidence['canonicalFiles'] == 6236
        if args.live:
            with urllib.request.urlopen(evidence['url'], timeout=45) as response:
                content = response.read().decode()
            files = set(re.findall(r'\d{6}\.mp3', content))
            assert expected <= files, (row['name'], len(expected - files))
    probes = json.loads((ROOT / 'docs/sources/reciter-audio-probes.json').read_text())
    assert len(probes) == 120 and all(p.get('audioDecodePassed') for p in probes)
    assert len({p['sha256'] for p in probes if p['kind'] == 'chapter_1'}) == 30
    actual = json.loads((ROOT / 'docs/sources/reciter-actual-verse-probes.json').read_text())
    assert len(actual) == 45 and all(p.get('audioDecodePassed') for p in actual)
    print('Verified 28 active unique people against historical 30-reciter evidence, chapter/verse mappings, 114-surah manifests, 6236-verse directory evidence and 120 directory/chapter samples plus 45 actual Islamic Network clips decoded.')

if __name__ == '__main__':
    main()
