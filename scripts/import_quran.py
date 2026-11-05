#!/usr/bin/env python3
"""Rebuild the bundled Quran from pinned, checksum-verified upstream snapshots.

Python standard library only; TLS certificate checking is always enabled.
Use --source-dir PATH to read a checkout of the pinned source without network.
Use --verify to validate the bundled asset without changing it or using network.
Text, punctuation, QuranEnc notes and verse numbering are preserved verbatim.
"""
from __future__ import annotations

import argparse
import bisect
import hashlib
import json
from pathlib import Path
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
ASSET = ROOT / "app/src/main/assets/quran.json"
COMMIT = "6523a9ad3b73bb7e8cb3a9cdf0ea774b8bab3123"
BASE = f"https://raw.githubusercontent.com/risan/quran-json/{COMMIT}/"
FILES = {
    "data/tanzil/uthmani.json": "adaebb377c60eba1bab6ef652959c7a0b4ccb4d0ca42145945c14ecdbeb4b761",
    "data/tanzil/chapters.json": "5b18adae945fcb6f9fbb865dd7dc596f89a63607a4e6617ca9e37f7f10d3386b",
    "data/quranenc/indonesian_affairs.json": "5990a1522965daef7fd721d21e4f9c3d7ac77404060cf1343a8fedc07cc72406",
    "data/extra/english_pickthall.json": "f93ecb2a50578c9cc4e5b89630e79fa9bcdd1d289b1a858f2aedf5aa55e8194a",
    "data/audio/islamic_network_by_surah.json": "f41562f8168182c83a9918cec7f7a509aee72a09414f18bf6fddfbab370506fb",
    "data/quranenc/catalogue.json": "3090f1ab9e1765deb8f92aaf2884f1bb123ab528b23c6aa1dfc184a0fdb67c16",
    "data/meta/sources.json": "117045528f8ab07e21f702d45437d1b7e8c9ed22ab580574df0b9b7db06c7723",
    "LICENSE.txt": "7abe19ec9bb73b36141b999b861d24ad855e808bafe0f81e84cce28556f6c297",
}
TANZIL_NOTICE = (
    "Tanzil Quran Text. Copyright (C) 2007-2021 Tanzil Project. License: Creative Commons "
    "Attribution 3.0. This copy of the Quran text is carefully produced, highly verified and "
    "continuously monitored by a group of specialists in Tanzil Project. TERMS OF USE: "
    "Permission is granted to copy and distribute verbatim copies of this text, but CHANGING "
    "IT IS NOT ALLOWED. This Quran text can be used in any website or application, provided "
    "that its source (Tanzil Project) is clearly indicated, and a link is made to tanzil.net "
    "to enable users to keep track of changes. This copyright notice shall be included in all "
    "verbatim copies of the text, and shall be reproduced appropriately in all files derived "
    "from or containing substantial portion of this text. Please check updates at: "
    "http://tanzil.net/updates/"
)
BASMALAH = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
BASMALAH_WITH_SHADDA = "بِّسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"


def split_opening_basmalah(surah: int, ayah: int, raw_arabic: str) -> tuple[str, str | None]:
    """Return numbered ayah text and optional unnumbered opening, losslessly.

    The pinned Tanzil export embeds an opening in 112 first-ayah strings.
    This is a presentation boundary, not a change to the canonical source asset.
    Al-Fatihah 1:1, At-Tawbah, internal basmalahs and unknown input stay intact.
    Keep this behavior aligned with data/QuranOpeningPolicy.kt.
    """
    if not 2 <= surah <= 114 or surah == 9 or ayah != 1:
        return raw_arabic, None
    for prefix in (BASMALAH, BASMALAH_WITH_SHADDA):
        if raw_arabic.startswith(prefix + " "):
            body = raw_arabic[len(prefix) + 1:]
            if body.strip():
                return body, prefix
    return raw_arabic, None


def validate_openings(data: dict) -> None:
    """Check all source openings before an import or an offline verification."""
    first_verses = {v["surah"]: v for v in data["verses"] if v["ayah"] == 1}
    assert set(first_verses) == set(range(1, 115)), "Every surah needs verse 1"
    for surah, verse in first_verses.items():
        raw = verse["arabic"]
        body, opening = split_opening_basmalah(surah, 1, raw)
        if surah == 1:
            assert raw == BASMALAH and opening is None, "Al-Fatihah basmalah must remain 1:1"
        elif surah == 9:
            assert raw.startswith("بَرَآءَةٌ") and opening is None, "At-Tawbah must not acquire an opening basmalah"
        else:
            expected = BASMALAH_WITH_SHADDA if surah in (95, 97) else BASMALAH
            assert opening == expected, f"Missing or unexpected source basmalah at {surah}:1"
            assert opening + " " + body == raw, f"Source code points changed at {surah}:1"
            assert not body.startswith((BASMALAH, BASMALAH_WITH_SHADDA)), f"Duplicate basmalah at {surah}:1"
    naml = next(v for v in data["verses"] if (v["surah"], v["ayah"]) == (27, 30))
    assert BASMALAH in naml["arabic"], "An-Naml 27:30 must retain its internal basmalah"
    assert split_opening_basmalah(27, 30, naml["arabic"]) == (naml["arabic"], None)


def load_sources(source_dir: Path | None) -> dict[str, bytes]:
    sources = {}
    for path, expected in FILES.items():
        if source_dir:
            data = (source_dir / path).read_bytes()
        else:
            request = urllib.request.Request(BASE + path, headers={"User-Agent": "Nuraya-data-import/1.0"})
            with urllib.request.urlopen(request, timeout=90) as response:
                data = response.read()
        actual = hashlib.sha256(data).hexdigest()
        if actual != expected:
            raise ValueError(f"Checksum mismatch: {path}; stop and investigate upstream before updating pins")
        sources[path] = data
    return sources


def build(sources: dict[str, bytes]) -> dict:
    decoded = {p: json.loads(b) for p, b in sources.items() if p.endswith(".json")}
    meta = decoded["data/tanzil/chapters.json"]
    ar = decoded["data/tanzil/uthmani.json"]
    ind = decoded["data/quranenc/indonesian_affairs.json"]
    eng = decoded["data/extra/english_pickthall.json"]
    starts = [(j["sura"], j["aya"]) for j in meta["indexes"]["juzs"]]
    edition = next(e for e in decoded["data/quranenc/catalogue.json"]["translations"] if e["key"] == "indonesian_affairs")
    selected_paths = set(FILES) - {"data/meta/sources.json", "LICENSE.txt"}
    provenance = [s for s in decoded["data/meta/sources.json"] if s["path"] in selected_paths]
    for s in provenance:
        if s["license_status"] != "granted":
            raise ValueError(f"No redistribution permission for {s['path']}")
    surahs, verses = [], []
    for chapter in meta["chapters"]:
        sid = chapter["id"]
        surahs.append({
            "id": sid, "name": chapter["transliteration"], "arabicName": chapter["name"],
            "englishName": chapter["translation"], "ayahCount": chapter["total_verses"],
            "revelation": chapter["type"],
        })
        a, i, e = ar[str(sid)], ind[str(sid)], eng[str(sid)]
        if not (len(a) == len(i) == len(e) == chapter["total_verses"]):
            raise ValueError(f"Mismatched verse count for surah {sid}")
        for av, iv, ev in zip(a, i, e, strict=True):
            aid = av["verse"]
            if not (av["chapter"] == iv["chapter"] == ev["chapter"] == sid and aid == iv["verse"] == ev["verse"]):
                raise ValueError(f"Translation alignment mismatch: {sid}:{aid}")
            verses.append({
                "number": len(verses) + 1, "surah": sid, "ayah": aid,
                "arabic": av["text"], "indonesian": iv["text"], "english": ev["text"],
                "juz": bisect.bisect_right(starts, (sid, aid)),
                "indonesianFootnotes": iv.get("footnotes"),
            })
    qaris = []
    for identifier, name in [
        ("ar.alafasy", "Mishary Rashid Alafasy"),
        ("ar.abdulbasitmurattal", "Abdul Basit Abdus Samad"),
        ("ar.saudalshuraim", "Saud Al-Shuraim"),
    ]:
        assert decoded["data/audio/islamic_network_by_surah.json"]["bitrates"]["128"][identifier] == 114
        qaris.append({
            "id": identifier, "name": name, "description": "Hafs · Murattal · 128 kbps",
            "sourceUrl": "https://alquran.cloud/cdn", "baseUrl": f"https://cdn.islamic.network/quran/audio-surah/128/{identifier}/",
        })
    return {
        "schemaVersion": 1,
        "metadata": {
            "sourceRepository": "https://github.com/risan/quran-json", "sourceCommit": COMMIT,
            "datasetLicense": "CC BY-SA 4.0; individual source terms also apply",
            "notice": TANZIL_NOTICE,
            "indonesianEdition": edition,
            "englishEdition": "Marmaduke Pickthall, The Meaning of the Glorious Koran (1930); public domain",
            "provenance": provenance,
            "transliteration": "Not bundled: the available generated transliteration has not been reviewed by a qualified reader.",
            "tafsir": "Not bundled; online links identify their provider and author. Indonesian footnotes are translation notes, not a standalone tafsir.",
            "audio": "Streamed or explicitly downloaded by the user; not bundled. Noncommercial use under Islamic Network terms; copyright stays with reciters.",
        },
        "surahs": surahs, "verses": verses, "juzStarts": meta["indexes"]["juzs"], "qaris": qaris,
    }


def validate(data: dict) -> None:
    assert data["schemaVersion"] == 1
    assert len(data["surahs"]) == 114
    assert len(data["verses"]) == 6236
    assert [s["id"] for s in data["surahs"]] == list(range(1, 115))
    assert [v["number"] for v in data["verses"]] == list(range(1, 6237))
    assert {v["juz"] for v in data["verses"]} == set(range(1, 31))
    assert len(data["juzStarts"]) == 30
    for s in data["surahs"]:
        selected = [v for v in data["verses"] if v["surah"] == s["id"]]
        assert len(selected) == s["ayahCount"]
        assert [v["ayah"] for v in selected] == list(range(1, s["ayahCount"] + 1))
    for v in data["verses"]:
        assert all(isinstance(v[k], str) and v[k].strip() for k in ("arabic", "indonesian", "english"))
    assert data["metadata"]["sourceCommit"] == COMMIT
    assert data["metadata"]["notice"] == TANZIL_NOTICE
    assert data["metadata"]["indonesianEdition"]["version"] == "1.0.1"
    validate_openings(data)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-dir", type=Path)
    parser.add_argument("--verify", action="store_true")
    args = parser.parse_args()
    if args.verify:
        content = ASSET.read_bytes()
        validate(json.loads(content))
        expected = ASSET.with_suffix(".sha256").read_text().split()[0]
        assert hashlib.sha256(content).hexdigest() == expected, "Bundled corpus checksum changed"
        print("Verified: 114 surahs, 6,236 aligned Arabic/Indonesian/English verses, 30 juz, "
              "all 114 first ayat and 112 separate opening spans, asset SHA-256")
        return
    sources = load_sources(args.source_dir)
    data = build(sources)
    validate(data)
    ASSET.parent.mkdir(parents=True, exist_ok=True)
    content = (json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n").encode("utf-8")
    ASSET.write_bytes(content)
    ASSET.with_suffix(".sha256").write_text(f"{hashlib.sha256(content).hexdigest()}  quran.json\n")
    license_dir = ASSET.parent / "licenses"
    license_dir.mkdir(exist_ok=True)
    (license_dir / "QURAN_DATA_CC_BY_SA_4.0.txt").write_bytes(sources["LICENSE.txt"])
    (license_dir / "TANZIL_NOTICE.txt").write_text(TANZIL_NOTICE + "\n\nhttps://tanzil.net\nhttps://tanzil.net/docs/text_license\n", encoding="utf-8")
    (ROOT / "docs/quran-source-checksums.json").write_text(json.dumps({"commit": COMMIT, "files": FILES}, indent=2) + "\n")
    print(f"Built {ASSET}: {len(content):,} bytes, 114 surahs, 6,236 verses, 30 juz")


if __name__ == "__main__":
    main()
