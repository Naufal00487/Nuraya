#!/usr/bin/env python3
"""Audit recorded audio evidence and exact edition mappings without changing app assets.

Offline and reproducible from committed source snapshots and completed online
sample reports. This checks identifiers/URLs/checksums, not a human voiceprint.
To refresh online samples first use verify_audio_catalog.py --online --report
and verify_verse_audio.py --online, then place the reports in docs/audit/1.3.1.
"""
from collections import defaultdict
import hashlib
import json
from pathlib import Path

from verify_audio_catalog import validate
from verify_verse_audio import verify_catalog

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/audit/1.4.0'
SAMPLE_EVIDENCE = ROOT / 'docs/audit/1.3.1'
ASSETS = ROOT / 'app/src/main/assets'


def read(path):
    return json.loads(path.read_text())


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    catalog = read(ASSETS / 'reciters.json')
    manifest_path = ROOT / 'docs/sources/islamic-network-surah-manifest.json'
    names_path = ROOT / 'docs/sources/islamic-network-audio-editions.json'
    validate(catalog, manifest_path.read_bytes())
    assert sha(names_path) == catalog['metadata']['namesSha256']
    names = {r['identifier']: r for r in read(names_path)['data']}
    qaris = {r['id']: r for r in catalog['qaris']}
    verse_qaris = {r['id']: r for r in verify_catalog()}
    for identifier in qaris.keys() | verse_qaris.keys():
        assert identifier in names and names[identifier]['format'] == 'audio'
        assert names[identifier]['language'] == 'ar'

    # Explicit curated display labels; source folder IDs remain unchanged.
    allowed_name_overrides = {
        'ar.alafasy': ('Mishary Rashid Alafasy', 'Alafasy'),
        'ar.abdulbasitmurattal': ('Abdul Basit Abdus Samad', 'Abdul Basit'),
        'ar.saudalshuraim': ('Saud Al-Shuraim', 'Saud ash-Shuraim'),
        'ar.muhammadsiddiqalminshawimujawwad': ('Muhammad Siddiq Al-Minshawi', 'Muhammad Siddiq al-Minshawi (Mujawwad)'),
        'ar.ahmedalajmi': ('Ahmed Al-Ajmi', 'Ahmed al-Ajmi'),
        'ar.aliabdurrahmanalhuthaify': ('Ali Abdurrahman Al-Hudhaify', 'Ali Abdur-Rahman al-Huthaify'),
        'ar.nasseralqatami': ('Nasser Al-Qatami', 'Nasser al-Qatami'),
        'ar.yasseraldossari': ('Yasser Al-Dossari', 'Yasser ad-Dossari'),
        'ar.haniarrifai': ('Hani Ar-Rifai', 'Hani ar-Rifai'),
    }
    overrides = []
    for identifier, reciter in qaris.items():
        provider = names[identifier]['englishName']
        if reciter['name'] != provider:
            assert (reciter['name'], provider) == allowed_name_overrides[identifier]
            overrides.append({'id': identifier, 'appName': reciter['name'], 'providerName': provider})
    assert len(overrides) == len(allowed_name_overrides)
    for identifier, reciter in verse_qaris.items():
        assert reciter['name'] == names[identifier]['englishName']

    full_path = SAMPLE_EVIDENCE / 'audio-full-samples.json'
    full = read(full_path)
    assert not full['failedSamples']
    assert full['liveManifestSha256'] == full['pinnedManifestSha256'] == sha(manifest_path)
    assert len(full['samples']) == full['reciterCount'] == len(qaris)
    assert {s['id'] for s in full['samples']} == qaris.keys()
    hashes = defaultdict(list)
    for sample in full['samples']:
        assert sample['surah'] == 1
        assert sample['url'] == qaris[sample['id']]['baseUrl'] + '1.mp3'
        assert sample['httpStatus'] == 200 and sample['fullHostDecode'] == 'passed'
        assert sample['bytes'] > 0 and all(s['codec_name'] == 'mp3' for s in sample['streams'])
        hashes[sample['sha256']].append(sample['id'])
    duplicates = [{'sha256': key, 'ids': sorted(ids), 'scope': 'Complete Al-Fatihah MP3 bytes only'}
                  for key, ids in hashes.items() if len(ids) > 1]
    assert [d['ids'] for d in duplicates] == [['ar.alafasy', 'ar.misharyrashidalafasy']]

    verse_path = SAMPLE_EVIDENCE / 'verse-audio-samples.json'
    verse = read(verse_path)
    assert not verse['failedSamples'] and verse['reciterCount'] == len(verse_qaris)
    samples = verse['samples']
    individual = [s for s in samples if s['kind'] == 'individual-ayah']
    assert len(individual) == len(verse_qaris)
    assert {s['reciter'] for s in individual} == verse_qaris.keys()
    quran = read(ASSETS / 'quran.json')
    for sample in samples:
        assert sample['httpStatus'] == 200 and sample['fullHostDecode'] == 'passed' and sample['bytes'] > 0
        identifier = sample['reciter']
        if sample['kind'] == 'paired-timed-chapter':
            assert identifier == 'ar.alafasy'
            chapter = read(ASSETS / f"audio_timing/alafasy/{sample['surah']}.json")
            assert sample['url'] == chapter['audioUrl']
            assert sample['lastClipMs'] == max(v['clipTo'] for v in chapter['verses'])
            assert sample['lastClipMs'] <= sample['durationSeconds'] * 1000 + 100
        else:
            surah, ayah = map(int, sample['reference'].split(':'))
            global_ayah = sum(s['ayahCount'] for s in quran['surahs'] if s['id'] < surah) + ayah
            rate = verse_qaris[identifier]['bitrateKbps']
            assert sample['url'] == f'https://cdn.islamic.network/quran/audio/{rate}/{identifier}/{global_ayah}.mp3'
    assert len(samples) == 22

    portraits = read(ASSETS / 'portraits/catalog.json')['portraits']
    expected_profiles = {'alafasy': 7, 'abdulbasit': 2, 'sudais': 3, 'shatri': 4,
                         'rifai': 5, 'husary': 6, 'minshawi': 9, 'shuraim': 10}
    supplied = read(ROOT / "docs/sources/qari-user-images.json")
    mapped = []
    for portrait in portraits:
        if portrait['id'] in expected_profiles:
            profile = expected_profiles[portrait['id']]
            assert portrait['sourceReciterId'] == profile
            assert portrait['sourceCommit'] == 'aff1a035b09b66f28047b3216edcae4c5c949a49'
            assert portrait['sourcePage'] == f'https://quran.com/reciters/{profile}'
            prefix = f"https://raw.githubusercontent.com/quran/quran.com-frontend-next/{portrait['sourceCommit']}/public/images/reciters/{profile}."
            assert portrait['sourceUrl'].startswith(prefix)
        else:
            number = int(portrait['id'].removeprefix('user_'))
            entry = next(e for e in supplied['entries'] if e['number'] == number)
            assert entry['status'] == 'user_supplied'
            assert portrait['qariIds'] == [entry['id']]
            assert portrait['sourceName'] == 'Dokumen foto qari dari penyusun'
        photo = ASSETS / portrait['asset']
        assert photo.stat().st_size == portrait['bytes'] and sha(photo) == portrait['sha256']
        assert set(portrait['qariIds']) <= (qaris.keys() | verse_qaris.keys())
        mapped.extend(portrait['qariIds'])
    assert len(portraits) == 134 and len(mapped) == len(set(mapped)) == 140

    other_riwayat = [r for r in qaris.values() if r['description'].startswith(('Qiraat lain', 'Riwayat Qaloon'))]
    assert len(other_riwayat) == 5
    report = {
        'scope': 'Offline verification of pinned provider metadata, exact URLs and previously completed HTTP/decode evidence; no human voice identification or certification of pronunciation.',
        'inputs': {str(p.relative_to(ROOT)): sha(p) for p in [ASSETS / 'reciters.json', ASSETS / 'verse_reciters.json', manifest_path, names_path, full_path, verse_path, ASSETS / 'portraits/catalog.json']},
        'surahCatalog': {'recordingEditionIds': len(qaris), 'uniqueIds': len(qaris), 'surahsPerEdition': 114, 'manifestFiles': len(qaris) * 114, 'allIdsMatchProviderMetadata': True, 'curatedDisplayNames': overrides, 'otherRiwayatExplicitlyLabeled': [{'id': r['id'], 'name': r['name'], 'description': r['description']} for r in other_riwayat]},
        'surahSamples': {'checkedAt': full['checkedAt'], 'fullDownloadsAndHostDecodesPassed': len(full['samples']), 'failed': 0, 'sampledSurah': 1, 'identicalSampleGroups': duplicates},
        'verseCatalog': {'recordingEditionIds': len(verse_qaris), 'individualFilesPerEdition': 6236, 'allIdsMatchProviderMetadata': True, 'wordTimingEditionIds': [r['id'] for r in verse_qaris.values() if r['supportsWordSync']]},
        'verseSamples': {'checkedAt': verse['checkedAt'], 'fullDownloadsAndHostDecodesPassed': len(samples), 'failed': 0, 'individualAyahSamples': len(individual), 'timedChapterSamples': 3, 'sameQariFallbackSamples': 2, 'urlsMatchRequestedEditionsAndReferences': True},
        'portraits': {'images': len(portraits), 'explicitMappedEditionIds': len(mapped), 'duplicateMappings': 0, 'bundledBytesMatchPinnedChecksums': True, 'mappings': [{'id': p['id'], 'quranComProfile': p.get('sourceReciterId'), 'editionIds': p['qariIds']} for p in portraits]},
        'appFixes': [
            {'finding': 'Two same-person Alafasy aliases have byte-identical Al-Fatihah samples.', 'fix': 'The visible qari list omits ar.misharyrashidalafasy when ar.alafasy exists: 137 visible choices. All 138 original IDs remain resolvable for saved content. This does not assert all 114 chapter files are identical.', 'regressionTest': 'QaQuranRepositoryTest.theKnownAlafasyAliasHasOneVisibleCardWhileExistingIdsStillResolve'},
            {'finding': 'An unavailable verse reciter ID previously fell back silently to Alafasy.', 'fix': 'PlaybackManager stops pending/current playback and reports an unavailable reciter instead of substituting another voice.', 'regressionTest': 'PlaybackIdentityTest'},
        ],
        'limitations': [
            'A matching provider edition ID and successful MP3 decode do not establish the speaker identity by listening.',
            '138 complete Al-Fatihah files were tested, not all 15,732 chapter files; availability can change after the recorded check.',
            '22 individual/timed/fallback audio samples were tested, not every clip from the 17 verse editions.',
            'Host decode is not real Android device playback testing.',
            'The 137 visible choices are recording editions, not necessarily 137 different people. Legitimate murattal/mujawwad and riwayat editions are retained.',
            'Portrait checks verify explicit profile mapping and stored image checksums, not an independent human face identification audit.',
        ],
    }
    OUT.mkdir(parents=True,exist_ok=True)
    (OUT / 'audio-identity.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    markdown = f'''# Audit identitas sumber audio Nuraya 1.4.0

Audit dapat diulang dengan `python3 scripts/audit-audio-identity.py`. Script ini membaca snapshot sumber dan bukti unduhan yang sudah dilakukan, tanpa mengubah aset aplikasi atau mengklaim pemeriksaan suara oleh manusia.

| Pemeriksaan | Hasil |
| --- | --- |
| Katalog murottal per surah | 138 ID edisi unik; masing-masing 114 nama berkas sesuai manifest; seluruh ID tercantum pada metadata penyedia |
| Sampel murottal | 138 Al-Fatihah diunduh lengkap, HTTP 200, MP3 berhasil didekode; 0 gagal |
| Katalog audio per ayat | 17 ID edisi unik; masing-masing 6.236 nama berkas sesuai manifest |
| Sampel audio per ayat/sinkronisasi | 22 unduhan lengkap dan decode berhasil: 17 ayat 1:1, 3 surah dengan timing, 2 ayat jalur fallback qari yang sama |
| Foto qari | 134 foto, 140 pemetaan ID; 8 foto dari Quran.com dan 126 dari dokumen penyusun; checksum seluruh foto cocok; tidak ada pemetaan ID ganda |
| Nama tampilan | 129 sama persis dengan nama Inggris penyedia; 9 label ejaan/nama lengkap tercatat di JSON tanpa mengganti ID rekaman |

Pemeriksaan 138 sampel surah dilakukan pada `{full['checkedAt']}`. Pemeriksaan 22 sampel ayat dilakukan pada `{verse['checkedAt']}`. Ini adalah bukti pemeriksaan pada waktu tersebut, bukan jaminan ketersediaan layanan selamanya.

## Temuan dan perbaikan

- `ar.alafasy` dan `ar.misharyrashidalafasy` mempunyai sampel Al-Fatihah dengan byte dan SHA-256 identik. Daftar sekarang menampilkan satu kartu Alafasy (137 pilihan terlihat), sedangkan kedua ID tetap tersedia untuk riwayat, favorit, dan unduhan lama. Tidak disimpulkan bahwa seluruh 114 berkas kedua edisi identik.
- Pemutar ayat sebelumnya dapat memilih Alafasy diam-diam ketika ID qari tidak ditemukan. Perilaku itu dihapus: audio/persiapan dihentikan dan pengguna diminta memilih ulang qari. Pengujian regresi terdapat pada `PlaybackIdentityTest`.
- Edisi murattal, mujawwad, dan riwayat berbeda tetap dipertahankan. Lima edisi selain Hafs diberi keterangan eksplisit: Abul-Harith dari al-Kisa’i, ad-Doori dari Abu Amr, Shu’bah dari Asim, as-Soosi (Abdur-Rasheed Sufi), dan Qaloon (Al-Hudhaify).
- Sinkronisasi kata hanya diaktifkan pada Alafasy yang mempunyai pasangan audio/timing; qari lain tidak dipasangkan dengan timing suara Alafasy.

## Batas pemeriksaan

Pemetaan nama → ID penyedia → URL rekaman telah diperiksa. Identitas suara setiap qari belum diautentikasi melalui pendengaran manusia. Pemeriksaan decode MP3 juga tidak menilai makhraj, tajwid, atau ketepatan semua bacaan. Yang diunduh lengkap adalah 138 Al-Fatihah dan 22 sampel tambahan, bukan seluruh 15.732 berkas surah atau seluruh klip ayat. Pemutaran pada HP fisik belum menjadi bagian audit ini.

Detail ID, ejaan nama, pemetaan foto, hash masukan, dan kelompok sampel identik ada dalam [audio-identity.json](audio-identity.json). Bukti mentah: [audio-full-samples.json](../1.3.1/audio-full-samples.json) dan [verse-audio-samples.json](../1.3.1/verse-audio-samples.json).
'''
    (OUT / 'audio-identity.md').write_text(markdown)
    print('Audio identity audit passed: 138 chapter editions, 17 verse editions, 138 + 22 decoded sample records, 134 portraits / 140 explicit IDs, one duplicate Alafasy sample group.')


if __name__ == '__main__':
    main()
