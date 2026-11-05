#!/usr/bin/env python3
"""Verify Nuraya's reciter catalogue against its pinned provider manifest.

The default check is offline. --online additionally downloads the live manifest
and Al-Fatihah from each reciter, verifies the HTTP response and decodes each MP3
with ffmpeg. Recordings are temporary test files, never bundled into the app.
"""
from __future__ import annotations

import argparse
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "app/src/main/assets/reciters.json"
SNAPSHOT = ROOT / "docs/sources/islamic-network-surah-manifest.json"
SOURCE_URL = "https://cdn.islamic.network/quran/info/by-surah/info.json"
MAX_SAMPLE_BYTES = 12 * 1024 * 1024


def validate(catalog: dict, manifest_bytes: bytes, *, pinned: bool = True) -> None:
    assert catalog["schemaVersion"] == 1
    metadata = catalog["metadata"]
    assert metadata["manifestUrl"] == SOURCE_URL
    assert metadata["bitrateKbps"] == 128
    if pinned:
        assert hashlib.sha256(manifest_bytes).hexdigest() == metadata["manifestSha256"], "Provider snapshot checksum changed"
    manifest = json.loads(manifest_bytes)
    entries = {
        reciter["name"]: reciter
        for root in manifest
        for bitrate in root.get("contents", [])
        if bitrate["name"] == "128"
        for reciter in bitrate.get("contents", [])
    }
    qaris = catalog["qaris"]
    assert len(qaris) >= 12, "Expanded catalogue is incomplete"
    ids = [qari["id"] for qari in qaris]
    assert len(ids) == len(set(ids)), "Duplicate reciter IDs"
    # Existing favorites and downloaded recordings continue to resolve.
    assert {"ar.alafasy", "ar.abdulbasitmurattal", "ar.saudalshuraim"}.issubset(ids)
    for qari in qaris:
        identifier = qari["id"]
        assert re.fullmatch(r"ar\.[a-z0-9]+", identifier)
        assert qari["name"].strip() and qari["description"].strip()
        assert qari["sourceUrl"] == "https://alquran.cloud/cdn"
        assert qari["baseUrl"] == f"https://cdn.islamic.network/quran/audio-surah/128/{identifier}/"
        assert qari["manifestSurahCount"] == 114
        files = {item["name"] for item in entries[identifier]["contents"] if item["type"] == "file"}
        assert files == {f"{number}.mp3" for number in range(1, 115)}, f"Incomplete recording catalogue: {identifier}"


def fetch(url: str, limit: int) -> tuple[bytes, str]:
    request = urllib.request.Request(url, headers={"User-Agent": "Nuraya-audio-validation/1.0"})
    with urllib.request.urlopen(request, timeout=60) as response:
        assert response.status == 200, f"HTTP {response.status}: {url}"
        assert response.geturl().startswith("https://"), "HTTPS required"
        data = response.read(limit + 1)
        assert 0 < len(data) <= limit, f"Empty or unexpectedly large response: {url}"
        expected_length = response.headers.get("Content-Length")
        if expected_length is not None:
            assert len(data) == int(expected_length), f"Incomplete response: {url}"
        return data, response.headers.get_content_type()


def validate_sample(qari: dict) -> dict:
    url = qari["baseUrl"] + "1.mp3"
    data, content_type = fetch(url, MAX_SAMPLE_BYTES)
    assert content_type in {"audio/mpeg", "audio/mp3", "application/octet-stream"}, f"Unexpected audio content type: {content_type}"
    with tempfile.TemporaryDirectory(prefix="nuraya-audio-check-") as temporary:
        path = Path(temporary) / "al-fatihah.mp3"
        path.write_bytes(data)
        probe = subprocess.run(
            ["ffprobe", "-v", "error", "-select_streams", "a", "-show_entries", "format=duration:stream=codec_name,sample_rate,channels", "-of", "json", str(path)],
            check=True, capture_output=True, text=True, timeout=30,
        )
        info = json.loads(probe.stdout)
        assert info["streams"] and all(stream["codec_name"] == "mp3" for stream in info["streams"]), f"Missing MP3 audio stream: {qari['id']}"
        duration = float(info["format"]["duration"])
        assert 10 < duration < 600, f"Unexpected Al-Fatihah duration for {qari['id']}"
        subprocess.run(
            ["ffmpeg", "-nostdin", "-v", "error", "-xerror", "-i", str(path), "-map", "0:a:0", "-f", "null", "-"],
            check=True, capture_output=True, timeout=60,
        )
    return {
        "id": qari["id"], "surah": 1, "url": url, "httpStatus": 200,
        "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest(),
        "contentType": content_type, "durationSeconds": duration,
        "streams": info["streams"], "fullHostDecode": "passed",
    }


def validate_sample_result(qari: dict) -> dict:
    try:
        return validate_sample(qari)
    except Exception as error:
        detail = getattr(error, 'stderr', None) or str(error)
        if isinstance(detail, bytes): detail = detail.decode('utf-8', 'replace')
        return {'id': qari['id'], 'surah': 1, 'url': qari['baseUrl'] + '1.mp3', 'error': detail[:2000]}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--online", action="store_true")
    parser.add_argument("--report", type=Path, default=ROOT / "artifacts/validation/audio-catalog.json")
    arguments = parser.parse_args()
    catalog = json.loads(CATALOG.read_text())
    validate(catalog, SNAPSHOT.read_bytes())
    print(f"Verified offline: {len(catalog['qaris'])} reciters, 114 exact surah filenames each, pinned manifest SHA-256", flush=True)
    if not arguments.online:
        return
    assert shutil.which("ffprobe") and shutil.which("ffmpeg"), "The online check needs ffprobe and ffmpeg"
    live_manifest, _ = fetch(SOURCE_URL, 4 * 1024 * 1024)
    validate(catalog, live_manifest, pinned=False)
    with ThreadPoolExecutor(max_workers=4) as executor:
        samples = list(executor.map(validate_sample_result, catalog["qaris"]))
    report = {
        "checkedAt": datetime.now(timezone.utc).isoformat(),
        "manifestUrl": SOURCE_URL,
        "liveManifestSha256": hashlib.sha256(live_manifest).hexdigest(),
        "pinnedManifestSha256": catalog["metadata"]["manifestSha256"],
        "reciterCount": len(catalog["qaris"]), "surahsPerReciterInManifest": 114,
        "samples": samples,
        "failedSamples": [sample for sample in samples if "error" in sample],
        "scope": "All selected reciters: provider manifest validation plus complete Al-Fatihah HTTP download and host MP3 decode. This is not Android playback validation and does not claim every recording was downloaded.",
    }
    arguments.report.parent.mkdir(parents=True, exist_ok=True)
    arguments.report.write_text(json.dumps(report, indent=2) + "\n")
    failures = [sample for sample in samples if "error" in sample]
    print(f"Online samples: {len(samples) - len(failures)} passed, {len(failures)} failed; report: {arguments.report}")
    for failure in failures: print(failure["id"], failure["error"])
    if failures: raise SystemExit(1)


if __name__ == "__main__":
    main()
