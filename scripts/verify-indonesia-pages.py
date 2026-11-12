#!/usr/bin/env python3
"""Check downloaded original pages; optionally create strips for visual boundary review.

Usage: python3 scripts/verify-indonesia-pages.py /path/to/001.png-directory
       --output /path/to/report.json --review-dir /path/to/review-images
Requires Pillow. Does not modify source pages or automatically certify their Arabic text.
"""
import argparse
import hashlib
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("pages", type=Path)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--review-dir", type=Path)
    args = parser.parse_args()
    manifest = json.loads((ASSETS / "mushaf/indonesia-images.json").read_text())
    refs = json.loads((ASSETS / "mushaf/indonesia-pages.json").read_text())["verses"]
    canonical = json.loads((ASSETS / "quran.json").read_text())["verses"]
    assert [(r["surah"], r["ayah"]) for r in refs] == [(v["surah"], v["ayah"]) for v in canonical]
    assert len(refs) == 6236
    assert sorted(set(r["page"] for r in refs)) == list(range(1, 605))
    assert [r["page"] for r in refs] == sorted(r["page"] for r in refs)
    assert [r["page"] for r in manifest["pages"]] == list(range(1, 605))
    assert len(set(r["url"] for r in manifest["pages"])) == 604
    rows = []
    for row in manifest["pages"]:
        number = row["page"]
        path = args.pages / f"{number:03d}.png"
        data = path.read_bytes()
        assert hashlib.sha1(f"blob {len(data)}\0".encode() + data).hexdigest() == row["gitBlobSha1"], number
        with Image.open(path) as image:
            image.load()
            assert image.size == (641, 917), number
        page_refs = [r for r in refs if r["page"] == number]
        rows.append({**row, "sha256": hashlib.sha256(data).hexdigest(), "bytes": len(data),
                     "dimensions": [641, 917], "decode": "passed",
                     "indexedFirstVerse": page_refs[0], "indexedLastVerse": page_refs[-1]})
    assert len(set(r["sha256"] for r in rows)) == 604, "Duplicate page files"
    for number in [1, 2, 3, 604]:
        assert (ASSETS / f"mushaf/indonesia/{number:03d}.png").read_bytes() == (args.pages / f"{number:03d}.png").read_bytes()
    result = {"sourceRepository": manifest["sourceRepository"], "commit": manifest["commit"],
              "pagesChecked": len(rows), "referenceCount": len(refs), "duplicates": 0,
              "canonicalCorpusSha256": hashlib.sha256((ASSETS / "quran.json").read_bytes()).hexdigest(),
              "scope": "Byte integrity, decoding and reference structure only. Visual review is separate.", "pages": rows}
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(result, indent=2) + "\n")
    if args.review_dir:
        args.review_dir.mkdir(parents=True, exist_ok=True)
        font = ImageFont.load_default(size=17)
        for index, start in enumerate(range(3, 605, 24), 1):
            numbers = list(range(start, min(start + 24, 605)))
            sheet = Image.new("RGB", (1282, 145 * ((len(numbers) + 1) // 2)), "white")
            draw = ImageDraw.Draw(sheet)
            for i, number in enumerate(numbers):
                last = rows[number - 1]["indexedLastVerse"]
                x, y = (i % 2) * 641, (i // 2) * 145
                draw.text((x + 5, y + 1), f"Page {number} | expected last ayah {last['surah']}:{last['ayah']}", fill="black", font=font)
                with Image.open(args.pages / f"{number:03d}.png") as image:
                    sheet.paste(image.crop((0, 735, 641, 852)), (x, y + 25))
            sheet.save(args.review_dir / f"sheet-{index:02d}.png")
    print(f"Verified {len(rows)} distinct source images, 6,236 ordered references and four bundled originals.")


if __name__ == "__main__":
    main()
