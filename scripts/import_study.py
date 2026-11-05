#!/usr/bin/env python3
"""Build and verify attributed study assets from frozen, checksummed source snapshots.

No network is needed. Source Arabic is never modified. Tajwid annotations are
copied only across identical text ranges; changed ranges are deliberately omitted.
"""
from __future__ import annotations
import argparse
from collections import Counter
import difflib
import gzip
import hashlib
import html
import json
from pathlib import Path
import re
import unicodedata
from import_quran import split_opening_basmalah

ROOT = Path(__file__).resolve().parents[1]
SOURCES = ROOT / 'docs/sources/study'
ASSETS = ROOT / 'app/src/main/assets/study'
QURAN_HASH = 'f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c'
TAJWID_SOURCE = 'Colin P. Fair · quran-tajweed · Hafs · CC BY 4.0'
WORDS_SOURCE = 'Quran.com · Indonesian / English word-by-word · 24 October 2026'
TAFSIR_SOURCE = 'Tafsir Kementerian Agama RI · EQuran.id API v2 · 24 Oktober 2026'


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def utf16(text: str) -> int:
    return len(text.encode('utf-16-le')) // 2


def normalized_word(text: str) -> str:
    """Comparison only: never written back to the Quran or word display text."""
    return ''.join(c for c in unicodedata.normalize('NFKD', text)
                   if c != 'ـ' and unicodedata.category(c).startswith('L')).replace('ٱ', 'ا').replace('ى', 'ي')


def plain_meaning(text: str) -> str:
    return html.unescape(re.sub(r'<[^>]+>', '', text)).strip()


def read_snapshot(name: str, manifest: dict) -> bytes:
    data = gzip.decompress((SOURCES / (name + '.gz')).read_bytes())
    expected = manifest['files'][name]
    assert len(data) == expected['bytes'] and sha(data) == expected['sha256'], f'Source changed: {name}'
    return data


def map_tajwid(old: str, new: str, annotations: list, prefix: int, stats: Counter) -> list:
    blocks = difflib.SequenceMatcher(a=old, b=new, autojunk=False).get_matching_blocks()
    result = []
    for span in annotations:
        start, end = span['start'] - prefix, span['end'] - prefix
        if start < 0 or end <= 0:
            stats['openingSpansExcluded'] += 1
            continue
        assert 0 <= start < end <= len(old)
        block = next((b for b in blocks if b.a <= start and end <= b.a + b.size), None)
        if block is None:
            stats['changedTextSpansOmitted'] += 1
            continue
        mapped_start = block.b + start - block.a
        mapped_end = block.b + end - block.a
        assert old[start:end] == new[mapped_start:mapped_end]
        result.append({'start': utf16(new[:mapped_start]), 'end': utf16(new[:mapped_end]), 'rule': span['rule']})
    stats['tajwidSpans'] += len(result)
    return result


def map_words(body: str, source_id: list, source_en: list, stats: Counter) -> list:
    words_id = [w for w in source_id if w['char_type_name'] == 'word']
    words_en = [w for w in source_en if w['char_type_name'] == 'word']
    assert [w['position'] for w in words_id] == [w['position'] for w in words_en]
    tokens = [m for m in re.finditer(r'\S+', body) if normalized_word(m.group())]
    cursor = 0
    result = []
    for source, english in zip(words_id, words_en):
        assert source['text_uthmani'] == english['text_uthmani']
        target = normalized_word(source['text_uthmani'])
        # Four API words intentionally group two orthographic tokens, e.g. بعد ما.
        end_cursor = cursor
        combined = ''
        while end_cursor < len(tokens):
            combined += normalized_word(tokens[end_cursor].group())
            end_cursor += 1
            if combined == target or len(combined) >= len(target):
                break
        mapped = combined == target
        # An explicitly unmapped different spelling remains visible, but cannot
        # drive word highlighting. Do not silently normalize Quran text itself.
        if not mapped:
            stats['wordsWithoutExactMapping'] += 1
            end_cursor = cursor + 1
        start = tokens[cursor].start() if mapped else None
        end = tokens[end_cursor - 1].end() if mapped else None
        result.append({
            'position': source['position'],
            'arabic': body[start:end] if mapped else source['text_uthmani'],
            'indonesian': plain_meaning(source['translation']['text']) if source['translation']['language_name'] == 'indonesian' else None,
            'english': plain_meaning(english['translation']['text']) if english['translation']['language_name'] == 'english' else None,
            'start': utf16(body[:start]) if mapped else None,
            'end': utf16(body[:end]) if mapped else None,
        })
        cursor = end_cursor
    assert cursor == len(tokens), (cursor, len(tokens))
    stats['words'] += len(result)
    stats['mappedWords'] += sum(w['start'] is not None for w in result)
    assert all(w['indonesian'] and w['english'] for w in result), 'Unexpected missing word translation'
    return result


def build() -> dict:
    manifest = json.loads((SOURCES / 'manifest.json').read_text())
    corpus_bytes = (ROOT / 'app/src/main/assets/quran.json').read_bytes()
    assert sha(corpus_bytes) == QURAN_HASH
    corpus = json.loads(corpus_bytes)
    old = {}
    for line in read_snapshot('tajwid-text-2017.txt', manifest).decode().splitlines():
        parts = line.split('|', 2)
        if len(parts) == 3 and parts[0].isdigit():
            old[(int(parts[0]), int(parts[1]))] = parts[2]
    annotations = {(v['surah'], v['ayah']): v['annotations'] for v in json.loads(read_snapshot('tajwid.json', manifest))}
    assert len(old) == len(annotations) == 6236
    grouped = {s: [v for v in corpus['verses'] if v['surah'] == s] for s in range(1, 115)}
    stats = Counter()
    ASSETS.mkdir(exist_ok=True, parents=True)
    asset_hashes = {}
    for surah, verses in grouped.items():
        source_id = json.loads(read_snapshot(f'id-{surah}.json', manifest))
        source_en = json.loads(read_snapshot(f'en-{surah}.json', manifest))
        source_tafsir = json.loads(read_snapshot(f'tafsir-{surah}.json', manifest))['data']
        assert source_id['pagination']['next_page'] is None and source_en['pagination']['next_page'] is None
        assert source_tafsir['nomor'] == surah
        assert len(verses) == len(source_id['verses']) == len(source_en['verses']) == len(source_tafsir['tafsir'])
        tafsirs = {t['ayat']: t['teks'] for t in source_tafsir['tafsir']}
        output = []
        for v, vid, ven in zip(verses, source_id['verses'], source_en['verses']):
            ayah = v['ayah']
            assert vid['verse_key'] == ven['verse_key'] == f'{surah}:{ayah}'
            body = split_opening_basmalah(surah, ayah, v['arabic'])[0]
            old_raw = old[(surah, ayah)]
            old_body = split_opening_basmalah(surah, ayah, old_raw)[0]
            spans = map_tajwid(old_body, body, annotations[(surah, ayah)], len(old_raw) - len(old_body), stats)
            words = map_words(body, vid['words'], ven['words'], stats)
            tafsir = tafsirs[ayah]
            assert isinstance(tafsir, str) and tafsir.strip()
            output.append({'ayah': ayah, 'arabic': body, 'tajwidSpans': spans, 'words': words,
                           'tafsir': tafsir, 'tafsirSource': TAFSIR_SOURCE, 'tafsirLanguage': 'Indonesia'})
            stats['verses'] += 1
            stats['tafsirVerses'] += 1
            stats['versesWithTajwid'] += bool(spans)
        raw = (json.dumps({'surah': surah, 'wordsSource': WORDS_SOURCE, 'tajwidSource': TAJWID_SOURCE, 'verses': output}, ensure_ascii=False, separators=(',', ':')) + '\n').encode()
        (ASSETS / f'{surah}.json').write_bytes(raw)
        asset_hashes[f'{surah}.json'] = sha(raw)
    index = {'schemaVersion': 1, 'snapshotDate': manifest['retrieved'], 'quranSha256': QURAN_HASH,
             'sources': [TAJWID_SOURCE, WORDS_SOURCE, TAFSIR_SOURCE],
             'sourceUrls': ['https://github.com/cpfair/quran-tajweed', 'https://api.quran.com/api/v4/', 'https://equran.id/apidev/v2'],
             'stats': dict(stats), 'files': asset_hashes}
    (ASSETS / 'index.json').write_text(json.dumps(index, ensure_ascii=False, indent=2) + '\n')
    return verify()


def verify() -> dict:
    corpus_bytes = (ROOT / 'app/src/main/assets/quran.json').read_bytes()
    assert sha(corpus_bytes) == QURAN_HASH, 'Canonical Quran asset changed'
    index = json.loads((ASSETS / 'index.json').read_text())
    original = {(v['surah'], v['ayah']): split_opening_basmalah(v['surah'], v['ayah'], v['arabic'])[0]
                for v in json.loads(corpus_bytes)['verses']}
    stats = Counter()
    for surah in range(1, 115):
        raw = (ASSETS / f'{surah}.json').read_bytes()
        assert sha(raw) == index['files'][f'{surah}.json']
        chapter = json.loads(raw)
        assert chapter['surah'] == surah
        for verse in chapter['verses']:
            body = verse['arabic']
            assert body == original[(surah, verse['ayah'])]
            for span in verse['tajwidSpans']:
                assert 0 <= span['start'] < span['end'] <= utf16(body)
            positions = [w['position'] for w in verse['words']]
            assert positions == list(range(1, len(positions) + 1))
            previous_end = 0
            for word in verse['words']:
                assert word['indonesian'] and word['english']
                start, end = word['start'], word['end']
                assert (start is None) == (end is None)
                if start is not None:
                    assert previous_end <= start < end <= utf16(body)
                    assert body.encode('utf-16-le')[start * 2:end * 2].decode('utf-16-le') == word['arabic']
                    previous_end = end
                    stats['mappedWords'] += 1
                else:
                    stats['wordsWithoutExactMapping'] += 1
            assert verse['tafsir'] and verse['tafsirSource'] == TAFSIR_SOURCE and verse['tafsirLanguage'] == 'Indonesia'
            stats['verses'] += 1
            stats['words'] += len(verse['words'])
            stats['tajwidSpans'] += len(verse['tajwidSpans'])
            stats['versesWithTajwid'] += bool(verse['tajwidSpans'])
            stats['tafsirVerses'] += 1
    assert stats['verses'] == stats['tafsirVerses'] == 6236
    for key, count in stats.items():
        assert index['stats'][key] == count
    return dict(stats)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--verify', action='store_true', help='Validate existing assets without rebuilding')
    args = parser.parse_args()
    print(json.dumps(verify() if args.verify else build(), indent=2))
