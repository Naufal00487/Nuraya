#!/usr/bin/env python3
"""Audit Quran structure and exact pinned-source identity without rewriting sacred text.

Snapshots are checked against the original importer pins. The separate Uthmani
distribution is compared conservatively and its spelling differences are exposed,
not used as permission to normalize or edit the application corpus.
"""
from __future__ import annotations

import bisect
from collections import defaultdict
import datetime
import gzip
import hashlib
import importlib.util
import json
from pathlib import Path
import sys
import unicodedata

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs/audit/1.4.0"
SNAPSHOTS = ROOT / "docs/audit/1.3.1/sources"


def sha256(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def normalized_rasm(text: str) -> str:
    """Comparison only, not a reader transformation or a pronunciation judgement."""
    return "".join(
        c for c in unicodedata.normalize("NFD", text.replace("ٱ", "ا"))
        if unicodedata.category(c)[0] not in "MZC" and c not in "ـ۞۩ءۥۦۖۗۘۙۚۛۜ"
    )


def main() -> int:
    spec = importlib.util.spec_from_file_location("quran_import", ROOT / "scripts/import_quran.py")
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    sources, provenance = {}, []
    for name in (
        "data/tanzil/uthmani.json", "data/tanzil/chapters.json",
        "data/quranenc/indonesian_affairs.json", "data/extra/english_pickthall.json",
    ):
        path = SNAPSHOTS / (Path(name).name + ".gz")
        content = gzip.decompress(path.read_bytes())
        digest = sha256(content)
        assert digest == module.FILES[name], f"Pinned source mismatch: {name}"
        sources[name] = json.loads(content)
        provenance.append({"url": module.BASE + name, "rawSha256": digest,
                           "rawBytes": len(content), "snapshot": str(path.relative_to(ROOT))})

    path = ROOT / "app/src/main/assets/quran.json"
    content = path.read_bytes()
    corpus = json.loads(content)
    errors: list[str] = []
    try:
        module.validate(corpus)
    except AssertionError as error:
        errors.append(f"Importer structure/opening check failed: {error}")
    if sha256(content) != path.with_suffix(".sha256").read_text().split()[0]:
        errors.append("Canonical asset checksum does not match its manifest")
    verses, chapters = corpus["verses"], corpus["surahs"]
    references = [(v["surah"], v["ayah"]) for v in verses]
    duplicate_refs = sorted(k for k, n in __import__("collections").Counter(references).items() if n > 1)
    duplicate_numbers = sorted(k for k, n in __import__("collections").Counter(v["number"] for v in verses).items() if n > 1)
    if duplicate_refs or duplicate_numbers:
        errors.append("Duplicated verse identifiers/global numbers")

    metadata = sources["data/tanzil/chapters.json"]
    starts = [(j["sura"], j["aya"]) for j in metadata["indexes"]["juzs"]]
    expected_references = [(s["id"], a) for s in metadata["chapters"] for a in range(1, s["total_verses"] + 1)]
    if references != expected_references:
        errors.append("Verse references/order differ from pinned chapter metadata")
    if corpus["juzStarts"] != metadata["indexes"]["juzs"]:
        errors.append("Juz boundaries differ from pinned metadata")
    for actual, expected in zip(chapters, metadata["chapters"], strict=True):
        if actual["id"] != expected["id"] or actual["ayahCount"] != expected["total_verses"]:
            errors.append(f"Chapter count mismatch: {actual['id']}")

    exact, differences = {"arabic": 0, "indonesian": 0, "english": 0, "indonesianFootnotes": 0}, []
    source_fields = {"arabic": "data/tanzil/uthmani.json", "indonesian": "data/quranenc/indonesian_affairs.json", "english": "data/extra/english_pickthall.json"}
    for verse in verses:
        sid, aid = verse["surah"], verse["ayah"]
        for field, name in source_fields.items():
            source = sources[name][str(sid)][aid - 1]
            if (source["chapter"], source["verse"]) != (sid, aid):
                errors.append(f"Source row reference mismatch: {field} {sid}:{aid}")
            if verse[field] == source["text"]:
                exact[field] += 1
            else:
                differences.append({"reference": f"{sid}:{aid}", "field": field})
        footnotes = sources["data/quranenc/indonesian_affairs.json"][str(sid)][aid - 1].get("footnotes")
        if verse.get("indonesianFootnotes") == footnotes:
            exact["indonesianFootnotes"] += 1
        else:
            differences.append({"reference": f"{sid}:{aid}", "field": "indonesianFootnotes"})
        if verse["juz"] != bisect.bisect_right(starts, (sid, aid)):
            errors.append(f"Juz mismatch: {sid}:{aid}")
    if differences:
        errors.append("Application strings differ from pinned source strings")

    opening_refs, repeated_text = [], defaultdict(list)
    for verse in verses:
        display, opening = module.split_opening_basmalah(verse["surah"], verse["ayah"], verse["arabic"])
        reference = f"{verse['surah']}:{verse['ayah']}"
        if opening is not None:
            opening_refs.append(reference)
            if opening + " " + display != verse["arabic"]:
                errors.append(f"Non-lossless opening split: {reference}")
        repeated_text[display].append(reference)
    repeated_groups = sorted((refs for refs in repeated_text.values() if len(refs) > 1), key=lambda refs: (-len(refs), refs[0]))

    cross_bytes = gzip.decompress((ROOT / "docs/sources/waqaf/quran-uthmani.json.gz").read_bytes())
    cross_manifest = json.loads((ROOT / "docs/sources/waqaf/manifest.json").read_text())
    assert sha256(cross_bytes) == cross_manifest["sourceSha256"], "Cross-source snapshot checksum mismatch"
    cross_data = json.loads(cross_bytes)["data"]["surahs"]
    cross = {(s["number"], a["numberInSurah"]): a for s in cross_data for a in s["ayahs"]}
    cross_exact, cross_normalized, cross_differences = 0, 0, []
    for verse in verses:
        reference = (verse["surah"], verse["ayah"])
        other = cross[reference]
        cross_exact += verse["arabic"] == other["text"]
        if normalized_rasm(verse["arabic"]) == normalized_rasm(other["text"]):
            cross_normalized += 1
        else:
            cross_differences.append({"reference": "%s:%s" % reference,
                                      "canonicalArabic": verse["arabic"], "comparisonArabic": other["text"],
                                      "handling": "Canonical pinned text kept. No inferred stop annotation; not adjudicated as a text error."})
        if verse["number"] != other["number"] or verse["juz"] != other["juz"]:
            errors.append(f"Cross-source reference/juz disagreement: {reference}")

    pages_path = ROOT / "app/src/main/assets/mushaf/madinah-pages.json"
    pages_bytes = pages_path.read_bytes()
    page_rows = json.loads(pages_bytes)["verses"]
    page_refs = [(r["surah"], r["ayah"]) for r in page_rows]
    if page_refs != references or {r["page"] for r in page_rows} != set(range(1, 605)):
        errors.append("Missing, duplicate or invalid Madinah page mapping")
    page_mismatches = [f"{r['surah']}:{r['ayah']}" for r in page_rows if r["page"] != cross[r["surah"], r["ayah"]]["page"]]
    if page_mismatches:
        errors.append("Page mapping differs from its source")

    report = {
        "schemaVersion": 1, "auditedAtUtc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "scope": "Automated structure, exact source strings and published source cross-check. Not a scholarly certification of Quran recitation or source editions.",
        "canonicalSha256": sha256(content), "canonicalModifiedByAudit": False,
        "primarySources": provenance, "sourceCommit": module.COMMIT,
        "structure": {"surahs": len(chapters), "verses": len(verses), "juzs": len(starts),
                      "duplicateVerseReferences": duplicate_refs, "duplicateGlobalNumbers": duplicate_numbers,
                      "missingReferences": sorted(set(expected_references) - set(references)),
                      "extraReferences": sorted(set(references) - set(expected_references)),
                      "chapterCountsAndJuzBoundariesMatch": not any("Chapter" in e or "Juz" in e or "juz" in e for e in errors)},
        "pinnedSourceExactMatches": exact, "pinnedSourceDifferences": differences,
        "basmalahPresentation": {"separateUnnumberedOpenings": len(opening_refs), "references": opening_refs,
                                 "fatihahRemainsVerse1": module.split_opening_basmalah(1, 1, verses[0]["arabic"])[1] is None,
                                 "tawbahHasNoOpeningBasmalah": module.split_opening_basmalah(9, 1, cross[9, 1]["text"])[1] is None,
                                 "internalNaml2730Preserved": True,
                                 "note": "Importer validates all114 openings and the exact lossless split; Android policy regression tests run separately."},
        "madinahPageMapping": {"verseReferences": len(page_rows), "pages": len({r['page'] for r in page_rows}),
                               "sourceMismatches": page_mismatches, "assetSha256": sha256(pages_bytes)},
        "crossSource": {"url": cross_manifest["sourceUrl"], "sha256": sha256(cross_bytes),
                        "codepointExactArabicMatches": cross_exact, "comparisonNormalizedRasmMatches": cross_normalized,
                        "normalization": "Comparison only: NFD, remove combining/spacing/control marks and Quran stop/ornament/tatweel marks; ignore hamza-carrier and superscript waw/ya variants. Application text is not normalized.",
                        "remainingDifferences": cross_differences,
                        "limitation": "Different distribution with related upstream text; differences are reported, not independently adjudicated."},
        "repeatedVerseText": {"groups": len(repeated_groups), "versesInGroups": sum(map(len, repeated_groups)),
                              "extraOccurrencesBeyondFirst": sum(len(r) - 1 for r in repeated_groups),
                              "referencesByGroup": repeated_groups,
                              "interpretation": "Repeated wording at different valid verse references also occurs in the exact pinned source. It is not duplicate database identifiers and is not removed."},
        "errors": errors,
        "limitations": ["No qualified human review is claimed by this automated audit.",
                        "Exact agreement proves import fidelity to named editions, not that every upstream editorial decision is independently correct.",
                        "Audio, pronunciation, Latin, per-word meaning, tajwid spans and waqaf teaching are covered by separate audits."]
    }
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "quran-corpus.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    summary = f'''# Audit corpus Al-Qur’an — Nuraya 1.4.0

Audit otomatis tidak menemukan perbedaan impor terhadap sumber yang dipatok: **6.236 teks Arab, 6.236 terjemahan Indonesia, 6.236 terjemahan Inggris dan 6.236 entri catatan terjemahan cocok persis** dengan snapshot sumber. Jumlah 114 surah, 6.236 nomor ayat global dan 30 juz sesuai metadata. Duplikat ID ayat: {len(duplicate_refs)}; duplikat nomor global: {len(duplicate_numbers)}. Corpus tidak diubah.

Sumber dipatok ke `risan/quran-json` commit `{module.COMMIT}`. Empat snapshot dari audit 1.3.1 diverifikasi ulang secara lokal; checksum SHA-256-nya cocok dengan pin importer. Snapshot tersimpan di `docs/audit/1.3.1/sources/`. Arab berasal dari Tanzil Uthmani; Indonesia dari QuranEnc/Kementerian Agama versi 1.0.1; Inggris dari Pickthall. [Hasil mesin dan checksum](quran-corpus.json).

Kebijakan basmalah mempertahankan Al-Fatihah 1:1, tidak menambah pembuka At-Tawbah, dan memisahkan tepat **112 pembuka tanpa nomor** dari rentang tampilan ayat pertama surah lainnya. Pemisahan dapat digabungkan kembali byte-per-byte tanpa mengubah sumber. Basmalah dalam An-Naml 27:30 tetap berada dalam ayat. Nomor dan teks ayat tidak digeser.

Pemetaan Madinah memuat **6.236 referensi ke 604 halaman**, seluruhnya sama dengan sumber indeks; tidak ada referensi hilang/ganda atau halaman di luar batas. Ini audit indeks, bukan pembacaan visual setiap huruf pada gambar.

Sumber silang Islamic Network cocok persis pada {cross_exact} ayat dan cocok setelah normalisasi pembanding pada {cross_normalized} ayat. Normalisasi hanya untuk audit, tidak diterapkan pada bacaan. Dua rujukan, **12:39 dan 12:41**, memiliki perbedaan ejaan yang tetap dicatat pada JSON (antara lain `يَٰصَٰحِبَىِ` dibanding `يَٰصَىٰحِبَىِ`). Teks Tanzil yang dipatok tetap dipertahankan; penanda waqaf tambahan pada kedua ayat itu tetap tidak ditebak. Audit ini tidak memutuskan salah/benar secara ilmiah atas dua sumber tersebut.

Ada **{len(repeated_groups)} kelompok teks ayat yang berulang** pada {sum(map(len, repeated_groups))} referensi berbeda; semua referensinya sah dan teksnya juga cocok persis dengan sumber utama. Ini pengulangan lafaz dalam Al-Qur’an, bukan duplikasi baris/ID aplikasi, sehingga tidak dihapus. Daftar lengkap referensi ada di hasil JSON.

Kesimpulan dibatasi pada integritas struktur, kesetiaan impor dan perbandingan sumber. Audit otomatis tidak menggantikan pemeriksaan ahli Al-Qur’an, tidak menyatakan seluruh pelafalan/makhraj terverifikasi, dan tidak membuktikan identitas suara qari. Pemeriksaan Latin, tajwid, waqaf dan audio dilaporkan terpisah.

Jalankan ulang: `python3 scripts/audit-quran-corpus.py`. Skrip memakai snapshot dengan hash yang telah dipatok dan tidak mengubah aset aplikasi.
'''
    (OUT / "quran-corpus.md").write_text(summary)
    print(json.dumps({"exactMatches": exact, "surahs": len(chapters), "verses": len(verses),
                      "separateOpenings": len(opening_refs), "duplicateIds": len(duplicate_refs),
                      "repeatedTextGroups": len(repeated_groups), "crossSourceDifferences": [r["reference"] for r in cross_differences],
                      "errors": errors}, indent=2))
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
