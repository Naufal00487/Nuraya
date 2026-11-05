#!/usr/bin/env python3
"""Import source stop annotations only when the full verse and word boundary align.
The canonical Quran asset is never changed. Small high seen inside a word is
not saktah; only the source's separate boundary mark can enter this index.
"""
from pathlib import Path
import argparse, hashlib, json, re, unicodedata, urllib.request

ROOT = Path(__file__).resolve().parents[1]
SOURCE = 'https://api.alquran.cloud/v1/quran/quran-uthmani'
MARKS = 'ۖۗۘۙۚۛۜ'

def normalized(text):
    # Compare rasm across vocalization/hamza-carrier display variants, without
    # changing either original text. Verse IDs and complete rasm must agree.
    return ''.join(c for c in unicodedata.normalize('NFD', text.replace('ٱ', 'ا'))
                   if unicodedata.category(c)[0] not in 'MZC'
                   and c not in MARKS + 'ـ۞۩ءۥۦ')

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source-file', type=Path)
    args = parser.parse_args()
    raw = args.source_file.read_bytes() if args.source_file else urllib.request.urlopen(SOURCE, timeout=30).read()
    data = json.loads(raw)['data']['surahs']
    canonical_path = ROOT / 'app/src/main/assets/quran.json'
    canonical_bytes = canonical_path.read_bytes()
    corpus = json.loads(canonical_bytes)
    canonical = {(v['surah'], v['ayah']): v for v in corpus['verses']}
    records, omitted, matched, source_count = {}, [], 0, 0
    for surah in data:
        for ayah in surah['ayahs']:
            source_count += 1
            verse = canonical[surah['number'], ayah['numberInSurah']]
            source, target = ayah['text'], verse['arabic']
            if normalized(source) != normalized(target):
                omitted.append(f"{verse['surah']}:{verse['ayah']}")
                continue
            matched += 1
            # App's QuranOpeningPolicy separates this unnumbered four-word prefix.
            prefix = 4 if verse['surah'] not in (1, 9) and verse['ayah'] == 1 else 0
            if prefix:
                assert normalized(' '.join(target.split()[:4])) == normalized('بسم الله الرحمن الرحيم')
            boundaries = {len(normalized(target[:m.end()])): i + 1 - prefix
                          for i, m in enumerate(re.finditer(r'\S+', target))}
            stops = []
            for offset, character in enumerate(source):
                if character not in MARKS:
                    continue
                after_word = boundaries.get(len(normalized(source[:offset])))
                if after_word is None or after_word <= 0:
                    # This includes small high seen inside 2:245 and 7:69.
                    continue
                stops.append({'afterWord': after_word, 'mark': character})
            if stops:
                records[str(verse['number'])] = stops
    assert source_count == len(canonical) == 6236
    assert matched >= 6200 and len(records) > 2000
    result = {'schemaVersion': 1, 'source': SOURCE,
              'sourceSha256': hashlib.sha256(raw).hexdigest(),
              'canonicalSha256': hashlib.sha256(canonical_bytes).hexdigest(),
              'attribution': 'Islamic Network · Quran Uthmani / Tanzil · CC BY 3.0',
              'matchedVerses': matched, 'omittedReferences': omitted, 'verses': records}
    output = ROOT / 'app/src/main/assets/waqaf.json'
    output.write_text(json.dumps(result, ensure_ascii=False, separators=(',', ':')) + '\n')
    print(json.dumps({'matchedVerses': matched, 'omittedReferences': omitted,
                      'annotatedVerses': len(records), 'marks': sum(map(len, records.values())),
                      'assetBytes': output.stat().st_size, 'sourceSha256': result['sourceSha256']}, indent=2))

if __name__ == '__main__': main()
