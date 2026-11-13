#!/usr/bin/env python3
"""Import unchanged printed pages and their attributed verse geometry.

Uses a local, hash-audited checkout/download of the pinned public source. The
reference APK is only needed to establish its two factual page indexes once;
no APK code, credentials, or Arabic text is copied into the application.
"""
import argparse
import hashlib
import json
import sqlite3
import struct
import tempfile
from collections import defaultdict
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'app/src/main/assets/printed_mushaf'
COMMIT = 'df1ca931ca6e83f8ff6b2d3a9308406e11994d00'
REPOSITORY = 'https://github.com/agusibrahim/quran-image'
RAW = 'https://raw.githubusercontent.com/agusibrahim/quran-image/' + COMMIT
APK_SHA = 'c992c27d56897c62df6f515313303b9f1efdda1c8bca36c332d5c6ab16e202cc'
BUNDLED = (1, 2, 3, 7, 8, 604)
EDITIONS = {
    'id': ('indonesia', 'id'), 'id-tajwid': ('indonesia', 'id'),
    'md': ('madinah', 'md'), 'md-tajwid': ('madinah', 'md'),
    'kata-tajwid': ('indonesia', 'kata-tajwid'),
    'latin-tajwid': ('indonesia', 'kata-tajwid'),
    'hafalan': ('indonesia', None),
}


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, separators=(',', ':')) + '\n')


def blob_sha(data):
    return hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest()


def reference_index(apk):
    path = ROOT / 'docs/sources/printed-mushaf-reference-index.json'
    if apk:
        assert hashlib.sha256(apk.read_bytes()).hexdigest() == APK_SHA
        with ZipFile(apk) as z, tempfile.TemporaryDirectory() as directory:
            database = Path(directory) / 'reference.db'
            database.write_bytes(z.read('assets/db/quranbelajar.db'))
            with sqlite3.connect(f'file:{database}?mode=ro', uri=True) as db:
                rows = db.execute('SELECT surah_id,ayah,page,page_md,juz FROM quran_ayah ORDER BY id').fetchall()
        write(path, {'source': 'User-supplied Islampedia Quran Belajar 1.9.10',
                     'apkSha256': APK_SHA, 'columns': ['surah', 'ayah', 'indonesiaPage', 'madinahPage', 'juz'], 'verses': rows})
    return json.loads(path.read_text())['verses']


def normalized_map(name, data, refs):
    rows = list(data['data'])
    # Missing regions were inspected against the unchanged source images. Only
    # coordinates are supplied; no lettering or verse numbering is edited.
    if name == 'kata-tajwid':
        assert not any(r['surah'] == 10 and r['ayah'] == 95 for r in rows)
        rows += [dict(surah=10, ayah=95, blok=1, top=1572, left=21, width=221, height=136),
                 dict(surah=10, ayah=95, blok=2, top=1708, left=21, width=1002, height=136)]
    elif name == 'md':
        assert not any(r['surah'] == 69 and r['ayah'] == 35 for r in rows)
        rows += [dict(surah=69, ayah=35, blok=1, top=1556, left=12, width=457, height=128)]
    width, height = data['viewportWidth'], data['viewportHeight']
    grouped = defaultdict(lambda: defaultdict(list))
    clipped = []
    for row in rows:
        reference = (row['surah'], row['ayah'])
        assert reference in refs
        box = [row['left'], row['top'], row['left'] + row['width'], row['top'] + row['height']]
        bounded = [max(0, box[0]), max(0, box[1]), min(width, box[2]), min(height, box[3])]
        assert bounded[2] > bounded[0] and bounded[3] > bounded[1]
        if bounded != box:
            clipped.append({'surah': reference[0], 'ayah': reference[1], 'source': box, 'clipped': bounded})
        grouped[refs[reference]][reference].append(bounded)
    assert {ref for page in grouped.values() for ref in page} == set(refs)
    return {'width': width, 'height': height, 'pages': [
        {'page': page, 'verses': [{'surah': s, 'ayah': a, 'rects': rectangles}
                                for (s, a), rectangles in sorted(grouped[page].items())]}
        for page in range(1, 605)]}, clipped


def verify_assets():
    catalogue = json.loads((DEST / 'catalog.json').read_text())
    assert catalogue['commit'] == COMMIT
    assert catalogue['repository'] == REPOSITORY
    assert set(catalogue['bundledPages']) == set(BUNDLED)
    assert {e['id'] for e in catalogue['editions']} == set(EDITIONS)
    canonical_bytes = (ROOT / 'app/src/main/assets/quran.json').read_bytes()
    assert hashlib.sha256(canonical_bytes).hexdigest() == 'f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c'
    canonical = [(v['surah'], v['ayah']) for v in json.loads(canonical_bytes)['verses']]
    for edition in catalogue['editions']:
        index = json.loads((DEST / (edition['index'] + '-index.json')).read_text())
        refs = {(r['surah'], r['ayah']): r['page'] for r in index['verses']}
        assert list(refs) == canonical
        assert set(refs.values()) == set(range(1, 605))
        assert [p['page'] for p in edition['pages']] == list(range(1, 605))
        assert len({p['sha256'] for p in edition['pages']}) == 604
        for page in edition['pages']:
            relative = f"{edition['id']}/{page['page']:03d}.png"
            assert page['url'] == RAW + '/' + relative
            if page['page'] in BUNDLED:
                data = (DEST / relative).read_bytes()
                assert len(data) == page['bytes']
                assert blob_sha(data) == page['gitBlobSha1']
                assert hashlib.sha256(data).hexdigest() == page['sha256']
                assert data[:8] == b'\x89PNG\r\n\x1a\n'
                assert struct.unpack('>II', data[16:24]) == (edition['width'], edition['height'])
        if edition['mapping']:
            mapping = json.loads((DEST / (edition['mapping'] + '-map.json')).read_text())
            seen = []
            for page in mapping['pages']:
                for verse in page['verses']:
                    reference = (verse['surah'], verse['ayah'])
                    seen.append(reference)
                    assert refs[reference] == page['page']
                    assert verse['rects']
                    for left, top, right, bottom in verse['rects']:
                        assert 0 <= left < right <= edition['width'] and 0 <= top < bottom <= edition['height']
            assert seen == canonical
    print('Verified: seven 604-page editions, all indexes, six complete verse maps, 42 unchanged bundled PNGs, unchanged canonical Quran.')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--verify', action='store_true', help='Verify packaged assets without downloads or the reference APK')
    parser.add_argument('--cache', type=Path, help='Directory containing quran-image-tree.json, image-audit.json and quran-image/')
    parser.add_argument('--reference-apk', type=Path)
    args = parser.parse_args()
    if args.verify:
        verify_assets()
        return
    if not args.cache:
        parser.error('--cache is required for import')
    tree = json.loads((args.cache / 'quran-image-tree.json').read_text())
    assert tree['sha'] == COMMIT and not tree['truncated']
    files = {row['path']: row for row in tree['tree'] if row['type'] == 'blob'}
    audit = json.loads((args.cache / 'image-audit.json').read_text())
    assert audit['commit'] == COMMIT
    checked = {row['path']: row for row in audit['records']}
    assert len(checked) == 7 * 604 and all('error' not in row for row in checked.values())
    indexes = reference_index(args.reference_apk)
    corpus = json.loads((ROOT / 'app/src/main/assets/quran.json').read_text())['verses']
    assert [(r[0], r[1]) for r in indexes] == [(v['surah'], v['ayah']) for v in corpus]
    index_by_type = {}
    for name, column in [('indonesia', 2), ('madinah', 3)]:
        refs = [{'surah': row[0], 'ayah': row[1], 'page': row[column], 'juz': row[4]} for row in indexes]
        assert {r['page'] for r in refs} == set(range(1, 605))
        index_by_type[name] = {(r['surah'], r['ayah']): r['page'] for r in refs}
        write(DEST / f'{name}-index.json', {'schemaVersion': 1, 'sourceApkSha256': APK_SHA, 'verses': refs})
    map_report = {}
    for group, index_name in [('id', 'indonesia'), ('md', 'madinah'), ('kata-tajwid', 'indonesia')]:
        raw = (args.cache / 'quran-image' / group / 'map.json').read_bytes()
        assert blob_sha(raw) == files[group + '/map.json']['sha']
        mapping, clipped = normalized_map(group, json.loads(raw), index_by_type[index_name])
        write(DEST / f'{group}-map.json', mapping)
        map_report[group] = {'sourceUrl': RAW + '/' + group + '/map.json', 'sourceSha256': hashlib.sha256(raw).hexdigest(),
                             'clippedToImageBounds': clipped, 'mappedVerses': sum(len(p['verses']) for p in mapping['pages'])}
    catalogue = {'schemaVersion': 1, 'repository': REPOSITORY, 'commit': COMMIT, 'bundledPages': BUNDLED, 'editions': []}
    for edition, (index_name, group) in EDITIONS.items():
        page_rows = []
        for page in range(1, 605):
            relative = f'{edition}/{page:03d}.png'
            source = args.cache / 'quran-image' / relative
            data = source.read_bytes()
            assert blob_sha(data) == files[relative]['sha']
            assert hashlib.sha256(data).hexdigest() == checked[relative]['sha256']
            page_rows.append({'page': page, 'url': RAW + '/' + relative, 'bytes': len(data),
                              'gitBlobSha1': files[relative]['sha'], 'sha256': checked[relative]['sha256']})
            if page in BUNDLED:
                target = DEST / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(data)
        assert len({p['sha256'] for p in page_rows}) == 604
        height = 2202 if edition in {'kata-tajwid', 'latin-tajwid'} else 1745
        catalogue['editions'].append({'id': edition, 'index': index_name, 'mapping': group,
                                     'width': 1080, 'height': height, 'pages': page_rows})
    write(DEST / 'catalog.json', catalogue)
    write(ROOT / 'docs/sources/printed-mushaf-map-audit.json', map_report)
    verify_assets()
    print('Imported 7 editions × 604 verified pages; 42 bundled images; 3 complete 6,236-verse mappings.')
    print('Hafalan artwork is included. It has no source coordinate map; do not substitute another edition’s map.')


if __name__ == '__main__':
    main()
