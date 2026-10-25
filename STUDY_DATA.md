# Offline study data — Nuraya 1.1.0

The reader keeps its existing Tanzil Uthmani text verbatim. Study data is a separate,
attributed layer, loaded by chapter off the main thread and retained in a three-chapter
LRU cache. It requires no API key or connection while reading.

## Included editions and coverage

Snapshot date: **24 October 2026**.

- **6,236 verses / 114 surahs**, with exact reader text after the existing lossless
  opening-basmalah presentation split. The underlying `quran.json` remains SHA-256
  `f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c`.
- **77,429 word entries**: both Indonesian and English word meanings returned by the
  official Quran.com API. Word meanings are aids to study; they do not replace the
  existing whole-verse translation. No machine translation or inferred dictionary
  meaning is inserted.
- **60,023 tajwid annotations** covering 6,173 verses. Some verses naturally have none
  of the rules represented in this source. Colors are not a claim that every possible
  pronunciation, stopping or recitation rule has been covered.
- **Full Indonesian Kementerian Agama tafsir for all 6,236 verses**, from EQuran.id API
  v2. This means the complete supplied edition, not every tafsir ever written. The
  Indonesian edition remains labeled Indonesian when the app interface is English.
  Its text and paragraph breaks are preserved, including source wording and quirks.

## Source and terms

### Quran text

Tanzil Project Uthmani, as already documented in `DATA_SOURCES.md`. Its full notice
is retained in the existing bundled licenses. No letters or vowel marks in the
canonical reader text are changed by this importer.

### Tajwid

Colin P. Fair, **quran-tajweed**, pinned commit
`496f71cd191da00fa2a37ded79dbbddb033bb0ad`:

- https://github.com/cpfair/quran-tajweed
- https://github.com/cpfair/quran-tajweed/blob/496f71cd191da00fa2a37ded79dbbddb033bb0ad/output/tajweed.hafs.uthmani-pause-sajdah.json
- Source's exact 2017 Tanzil text:
  https://github.com/cpfair/quran-tajweed/files/7281388/quran-uthmani.txt
- Annotations: **Creative Commons Attribution 4.0 International**,
  https://creativecommons.org/licenses/by/4.0/ . Source README with the license
  declaration is preserved in the checked snapshots.

The source is a historical Hafs annotation dataset and is no longer actively
maintained. Nuraya adapts the **coordinates**, not the religious text or rules:

1. Resolve annotation positions against the author's exact 2017 text, not a guessed
   version of Tanzil.
2. Remove the unnumbered opening from the coordinate space using the same lossless
   rule as the reader (Al-Fatihah remains numbered; At-Taubah has no opening).
3. Map only ranges entirely contained in an exact, unchanged Unicode text block.
4. Recheck each old/new substring for literal equality, then convert offsets from
   Unicode code points to Compose/Kotlin UTF-16 indices.
5. Deliberately omit **34** annotations crossing changed text. They are not recreated
   using guessed rules. The underlying text remains readable in its normal color.

The renderer separately checks that the study text exactly equals the displayed
verse before accepting any annotation or word highlight.

### Word meanings

Official Quran.com v4 developer endpoint, one complete chapter per request:

`https://api.quran.com/api/v4/verses/by_chapter/{surah}?words=true&word_fields=text_uthmani&language={id|en}&per_page=300`

The snapshots include the returned language metadata and original word identifiers.
Only `char_type_name=word` entries are used. End-of-ayah ornaments are excluded.
Word ordinals remain those of Quran.com, matching its recording timestamps.

**77,428** entries map to exact substrings of Nuraya's frozen reader text. Four
Quran.com entries group two written tokens (for example, `بَعْدَ مَا`); those remain
one source position spanning both tokens. This avoids shifting all later words.
One spelling difference, **11:13, position 3**, cannot be mapped literally and keeps
null bounds. Its supplied meaning is available, but that word is never highlighted
against an unverified span.

Comparison normalization (vowel-mark/tatweel removal, Unicode decomposition and
alif-wasla/alif-maqsura matching) is used only to locate boundaries. It is never
written into the displayed Quran text. The displayed Arabic word, when mapped,
is the exact canonical reader substring. Meanings only have HTML markup/entities
decoded for plain text display; no translation is composed by Nuraya.

Quran.com content is **not labeled MIT, CC, or public domain**. It is credited to
Quran.com and supplied in this free, ad-free, noncommercial educational app through
its developer API. The current generic website/mobile-app terms are preserved with
the snapshot: https://quran.com/terms-and-conditions . Section 1.4 refers to copying
and distributing content for individual, noncommercial, informational purposes;
section 2.3 also describes personal noncommercial use. These generic end-user terms
are not represented as an unrestricted dataset license or a commercial redistribution
grant. A commercial release requires its own content-rights review.

### Tafsir

`https://equran.id/api/v2/tafsir/{surah}`

Provider documentation: https://equran.id/apidev/v2 . The documentation identifies
**Kementerian Agama RI (quran.kemenag.go.id)** as its data source. The snapshot uses
the `tafsir[].ayat` and complete `tafsir[].teks` fields. Nuraya attributes the edition
and provider and does not claim ownership or an invented open-source text license.
No tafsir passage is synthesized, shortened, or replaced with a whole-verse translation.

## Rebuild and verification

All 347 raw source/notice files are preserved, gzip-compressed deterministically, in
`docs/sources/study`. `manifest.json` records SHA-256 and uncompressed size for every
file. Frozen responses permit reproducible builds without repeated API requests.
The source archive is about 8.8 MB; generated study assets total about 23 MB before
APK compression.

```sh
python3 scripts/import_study.py
python3 scripts/import_study.py --verify
python3 -m unittest discover -s scripts -p 'test_import_study.py'
```

The importer checks source digests, pagination completion, chapter/verse keys,
6,236-verse coverage, source-language labels, full tafsir presence, canonical text
identity, ordered word positions, literal word-bound substrings, UTF-16 range bounds
and generated chapter hashes. `app/src/main/assets/study/index.json` records exact
counts and generated hashes. Regeneration must not silently accept changed sources.

This verifies provenance, mapping and data structure. It does not certify every
historical annotation or replace recitation instruction from a qualified teacher.
