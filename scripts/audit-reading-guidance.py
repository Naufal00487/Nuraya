#!/usr/bin/env python3
"""Read-only reproducibility/offset audit of attributed reading aids, not a tajwid certification."""
from __future__ import annotations
from collections import Counter
from datetime import datetime, timezone
import gzip
import hashlib
import json
from pathlib import Path
import re
import import_study
from import_quran import split_opening_basmalah
from import_waqaf import normalized, MARKS

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/audit/1.4.0'

def sha(raw): return hashlib.sha256(raw).hexdigest()
def read_json(path): return json.loads(path.read_text())

def main():
    original_bytes = (ROOT / 'app/src/main/assets/quran.json').read_bytes()
    original_hash = sha(original_bytes)
    corpus = json.loads(original_bytes)
    canonical = {(v['surah'], v['ayah']): v for v in corpus['verses']}
    assert len(canonical) == 6236
    source_manifest = read_json(ROOT / 'docs/sources/study/manifest.json')
    stats = import_study.verify()
    old = {}
    for line in import_study.read_snapshot('tajwid-text-2017.txt', source_manifest).decode().splitlines():
        row = line.split('|', 2)
        if len(row) == 3 and row[0].isdigit(): old[int(row[0]), int(row[1])] = row[2]
    annotations = {(v['surah'], v['ayah']): v['annotations'] for v in json.loads(import_study.read_snapshot('tajwid.json', source_manifest))}
    assert len(old) == len(annotations) == 6236
    known_rules = {'ghunnah','ikhfa','ikhfa_shafawi','idgham','idghaam_ghunnah','idghaam_no_ghunnah','idghaam_mutajanisayn','idghaam_mutaqaribayn','idghaam_shafawi','lam_shamsiyyah','iqlab','qalqalah','madd','madd_2','madd_246','madd_6','madd_munfasil','madd_muttasil','hamzat_wasl','silent'}
    rules, rebuilt, warnings = Counter(), Counter(), []
    duplicate_spans = 0
    matched_source_chapters = 0
    # Rebuild only in memory; never call the asset-writing importer build().
    for surah in range(1, 115):
        chapter = read_json(ROOT / f'app/src/main/assets/study/{surah}.json')
        source_id = json.loads(import_study.read_snapshot(f'id-{surah}.json', source_manifest))
        source_en = json.loads(import_study.read_snapshot(f'en-{surah}.json', source_manifest))
        tafsir_rows = json.loads(import_study.read_snapshot(f'tafsir-{surah}.json', source_manifest))['data']['tafsir']
        tafsirs = {r['ayat']: r['teks'] for r in tafsir_rows}
        assert len(chapter['verses']) == len(source_id['verses']) == len(source_en['verses']) == len(tafsirs)
        for verse, id_row, en_row in zip(chapter['verses'], source_id['verses'], source_en['verses']):
            ref = surah, verse['ayah']
            assert id_row['verse_key'] == en_row['verse_key'] == f'{surah}:{verse["ayah"]}'
            body = split_opening_basmalah(*ref, canonical[ref]['arabic'])[0]
            assert verse['arabic'] == body
            old_body = split_opening_basmalah(*ref, old[ref])[0]
            mapped = import_study.map_tajwid(old_body, body, annotations[ref], len(old[ref])-len(old_body), rebuilt)
            assert mapped == verse['tajwidSpans'], f'Tajwid annotation source mismatch: {ref}'
            words = import_study.map_words(body, id_row['words'], en_row['words'], rebuilt)
            assert words == verse['words'], f'Word source mismatch: {ref}'
            assert verse['tafsir'] == tafsirs[verse['ayah']]
            spans = [(s['start'], s['end'], s['rule']) for s in verse['tajwidSpans']]
            duplicate_spans += len(spans)-len(set(spans))
            for span in verse['tajwidSpans']: rules[span['rule']] += 1
            for word in verse['words']:
                if word['start'] is None:
                    warnings.append({'reference':f'{surah}:{verse["ayah"]}', 'wordPosition':word['position'], 'sourceWord':word['arabic'], 'reason':'Source spelling has no exact mapping to canonical word; no guessed highlight or color is applied.'})
        matched_source_chapters += 1
    assert duplicate_spans == 0
    assert not (set(rules)-known_rules), set(rules)-known_rules

    latin_manifest = read_json(ROOT / 'docs/sources/latin/manifest.json')
    assert latin_manifest['canonicalQuranSha256'] == original_hash
    latin_refs = []
    for surah in range(1,115):
        raw = gzip.decompress((ROOT / f'docs/sources/latin/{surah}.json.gz').read_bytes())
        meta = latin_manifest['files'][str(surah)]
        assert len(raw) == meta['bytes'] and sha(raw) == meta['sha256']
        source = json.loads(raw)['data']; assert source['nomor'] == surah
        actual = read_json(ROOT / f'app/src/main/assets/latin/{surah}.json')
        assert actual['surah'] == surah
        expected = [{'ayah':v['nomorAyat'],'latin':v['teksLatin'].strip()} for v in source['ayat']]
        assert expected == actual['verses'] and all(v['latin'] for v in expected)
        latin_refs.extend((surah,v['ayah']) for v in actual['verses'])
    assert latin_refs == list(canonical)
    assert len(latin_refs) == len(set(latin_refs)) == 6236

    waqaf = read_json(ROOT / 'app/src/main/assets/waqaf.json')
    raw_waqaf = gzip.decompress((ROOT / 'docs/sources/waqaf/quran-uthmani.json.gz').read_bytes())
    assert sha(raw_waqaf) == waqaf['sourceSha256'] and waqaf['canonicalSha256'] == original_hash
    expected_stops, omitted, matched = {}, [], 0
    for surah in json.loads(raw_waqaf)['data']['surahs']:
        for row in surah['ayahs']:
            verse = canonical[surah['number'],row['numberInSurah']]
            source, target = row['text'], verse['arabic']
            if normalized(source) != normalized(target):
                omitted.append(f'{verse["surah"]}:{verse["ayah"]}'); continue
            matched += 1
            prefix = 4 if verse['surah'] not in (1,9) and verse['ayah'] == 1 else 0
            boundaries = {len(normalized(target[:m.end()])):i+1-prefix for i,m in enumerate(re.finditer(r'\S+',target))}
            stops = []
            for index, character in enumerate(source):
                if character not in MARKS: continue
                after_word = boundaries.get(len(normalized(source[:index])))
                if after_word is not None and after_word > 0: stops.append({'afterWord':after_word,'mark':character})
            if stops: expected_stops[str(verse['number'])] = stops
    assert expected_stops == waqaf['verses'] and omitted == waqaf['omittedReferences']
    assert matched == waqaf['matchedVerses']
    for ref in [(2,245),(7,69)]:
        assert all(s['mark'] != 'ۜ' for s in expected_stops.get(str(canonical[ref]['number']),[]))
    assert any(s['mark']=='ۜ' for s in expected_stops[str(canonical[75,27]['number'])])
    assert all(len(v)==len({(s['afterWord'],s['mark']) for s in v}) for v in expected_stops.values())
    assert sha((ROOT / 'app/src/main/assets/quran.json').read_bytes()) == original_hash
    report = {
        'schemaVersion':1,'auditedAtUtc':datetime.now(timezone.utc).isoformat(),
        'auditType':'Read-only snapshot reproducibility, reference coverage, UTF-16 bounds, duplicate and displayed-guidance review',
        'canonicalArabicUnchanged':True,'canonicalSha256':original_hash,
        'structuralErrors':[],
        'latin':{'source':'EQuran.id API v2 frozen responses','snapshotManifestSha256':sha((ROOT/'docs/sources/latin/manifest.json').read_bytes()),'chapters':114,'verses':len(latin_refs),'duplicateReferences':0,'exactPublishedTextMatches':6236,'missingOrEmptyTexts':0,'latinColorsApplied':False},
        'study':{**stats,'exactSourceChapters':matched_source_chapters,'duplicateTajwidSpans':duplicate_spans,'unknownColorRules':[],'changedSourceSpansDeliberatelyOmitted':rebuilt['changedTextSpansOmitted'],'rules':dict(sorted(rules.items())),'unmappedWords':warnings,'snapshotManifestSha256':sha((ROOT/'docs/sources/study/manifest.json').read_bytes())},
        'waqaf':{'source':waqaf['source'],'sourceSha256':waqaf['sourceSha256'],'matchedVerses':matched,'annotatedVerses':len(expected_stops),'annotations':sum(map(len,expected_stops.values())),'omittedReferences':omitted,'duplicateAnnotations':0,'insideWordSeenExcluded':['2:245','7:69'],'boundarySaktahIncluded':'75:27'},
        'guidanceCorrections':[
            {'file':'app/src/main/java/com/nuraya/app/ui/StudyVersePanel.kt','issue':'A shared Wasl / Silent label did not explain the initial-reading case.','correction':'Hamzat wasl is pronounced when beginning the word with its prescribed vowel, omitted when joining; gray also denotes source silent-letter annotations.'},
            {'file':'app/src/main/java/com/nuraya/app/quranreading/QuranReadingModes.kt','issue':'Muanaqah wording sounded as though a stop at one sign was compulsory.','correction':'Continuing past both signs is allowed; when stopping, stop at only one of the paired signs.'}],
        'limits':[
            'Source agreement and valid offsets do not certify tajwid or makhraj pedagogically.',
            'Latin is a pronunciation aid, not a substitute for hearing qualified recitation; exact source reproduction does not validate every transliteration convention.',
            'The 34 source tajwid spans crossing changed text are omitted rather than guessed; the single unmapped word and two waqaf exceptions remain explicit.',
            'AI compares estimated recognized words; no phonetic/tajwid expert validation or guarantee of real-time correction was performed.',
            'No manual review of every rendered glyph or human listening assessment of all recitations was performed.']}
    OUT.mkdir(parents=True,exist_ok=True)
    (OUT/'reading-guidance.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({'latinVerses':len(latin_refs),'study':stats,'waqaf':report['waqaf'],'guidanceCorrections':len(report['guidanceCorrections']),'structuralErrors':[]},ensure_ascii=False,indent=2))

if __name__=='__main__': main()
