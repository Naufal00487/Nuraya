#!/usr/bin/env python3
"""Build/verify the bounded offline Quran/tafsir lookup index, without network access."""
from pathlib import Path
import argparse
import collections
import hashlib
import io
import json
import re
import struct
import unicodedata

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets'
INDEX = ASSETS / 'learning_index.bin'
META = ASSETS / 'learning_index_sources.json'


def compile_index():
    quran_path = ASSETS / 'quran.json'
    quran = json.loads(quran_path.read_text())
    chapters = {}
    sources = {'quran.json': hashlib.sha256(quran_path.read_bytes()).hexdigest()}
    for surah in range(1, 115):
        path = ASSETS / f'study/{surah}.json'
        chapters[surah] = json.loads(path.read_text())['verses']
        sources[f'study/{surah}.json'] = hashlib.sha256(path.read_bytes()).hexdigest()
    postings = collections.defaultdict(list)
    lengths = []
    for verse in quran['verses']:
        study = chapters[verse['surah']][verse['ayah'] - 1]
        assert study['ayah'] == verse['ayah']
        text = ' '.join([verse['indonesian'], verse['english'], study['tafsir'] or ''])
        normalized = ''.join(c for c in unicodedata.normalize('NFD', text.lower()) if not unicodedata.combining(c))
        terms = re.findall(r'[^\W\d_]{2,}', normalized, flags=re.UNICODE)
        lengths.append(min(len(terms), 65535))
        for term, count in collections.Counter(terms).items():
            postings[term].append((verse['number'], min(count, 255)))
    out = io.BytesIO()
    out.write(b'NLI1')
    out.write(struct.pack('>HI', len(lengths), len(postings)))
    for length in lengths:
        out.write(struct.pack('>H', length))
    for term, records in sorted(postings.items()):
        encoded = term.encode('utf-8')
        out.write(struct.pack('>H', len(encoded)))
        out.write(encoded)
        out.write(struct.pack('>H', len(records)))
        for number, count in records:
            out.write(struct.pack('>HB', number, count))
    data = out.getvalue()
    metadata = {'schemaVersion': 1, 'scope': 'Search index of Indonesian/English verse meanings and full Indonesian Kemenag tafsir. Original displayed text is loaded from attributed source assets.',
                'verseCount': len(lengths), 'terms': len(postings), 'indexSha256': hashlib.sha256(data).hexdigest(), 'sources': sources}
    return data, json.dumps(metadata, ensure_ascii=False, indent=2) + '\n'


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--verify', action='store_true')
    args = parser.parse_args()
    data, metadata = compile_index()
    if args.verify:
        assert INDEX.read_bytes() == data, 'Learning index differs from bundled Quran/tafsir'
        assert META.read_text() == metadata, 'Learning index source hashes differ'
        print('Verified local learning index: 6236 verse/tafsir sources, deterministic binary and SHA-256')
    else:
        INDEX.write_bytes(data)
        META.write_text(metadata)
        print(f'Wrote {INDEX.name}: {len(data):,} bytes')


if __name__ == '__main__':
    main()
