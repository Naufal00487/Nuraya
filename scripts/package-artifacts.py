#!/usr/bin/env python3
"""Package the actual Gradle APK and reviewable sources; never manufacture an APK."""
from __future__ import annotations

import hashlib
import argparse
import json
from pathlib import Path
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "artifacts"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--build-type", choices=("debug", "release"), default="debug")
    build_type = parser.parse_args().build_type
    apk = ROOT / f"app/build/outputs/apk/{build_type}/app-{build_type}.apk"
    if not apk.is_file() or apk.stat().st_size < 100_000:
        raise SystemExit(f"Build the signed APK first: bash scripts/gradle.sh assemble{build_type.title()}")
    metadata = json.loads((apk.parent / "output-metadata.json").read_text())
    element, = metadata["elements"]
    version_name = element["versionName"]
    version_code = element["versionCode"]
    if not version_name or any(c not in "0123456789.-" for c in version_name):
        raise SystemExit("Unexpected APK version name")
    source_names = subprocess.check_output(
        ["git", "ls-files", "--cached", "--others", "--exclude-standard", "-z"], cwd=ROOT,
    ).decode().split("\0")
    files = sorted({name for name in source_names if name and (ROOT / name).is_file()})
    build_inputs = [ROOT / name for name in files if name.startswith("app/src/main/") or name.endswith(".gradle.kts") or name == "gradle.properties"]
    newer = [str(path.relative_to(ROOT)) for path in build_inputs if path.stat().st_mtime > apk.stat().st_mtime]
    if newer:
        raise SystemExit("APK predates source changes. Rebuild before packaging: " + ", ".join(newer))
    # A debug-signed delivery still needs optimized native speech kernels.
    subprocess.run(["python3", str(ROOT / "scripts/verify-native-optimization.py"), "--build-type", build_type], cwd=ROOT, check=True)
    with zipfile.ZipFile(apk) as archive:
        names = set(archive.namelist())
        if not {"AndroidManifest.xml", "classes.dex", "assets/quran.json", "assets/reciters.json"}.issubset(names):
            raise SystemExit("Unexpected or incomplete Android APK")
        # AAPT shortens release resource paths. Verify the font bytes rather
        # than assuming the debug-only res/font/... entry name is retained.
        font = (ROOT / "app/src/main/res/font/amiri_quran.ttf").read_bytes()
        if not any(archive.read(name) == font for name in names if name.endswith(".ttf")):
            raise SystemExit("Canonical Quran font is missing or differs from source")
        for asset in sorted((ROOT / "app/src/main/assets").rglob("*")):
            if asset.is_file():
                entry = "assets/" + asset.relative_to(ROOT / "app/src/main/assets").as_posix()
                if entry not in names or archive.read(entry) != asset.read_bytes():
                    raise SystemExit(f"APK asset differs from the source tree: {entry}")
        if "assets/alafasy1.mp3" in names:
            raise SystemExit("Instrumentation fixture must not be packaged in the app")
    OUTPUT.mkdir(exist_ok=True)
    result_apk = OUTPUT / f"nuraya-{version_name}-{build_type}.apk"
    source_zip = OUTPUT / f"nuraya-{version_name}-source.zip"
    shutil.copy2(apk, result_apk)
    source_hashes = {}
    with zipfile.ZipFile(source_zip, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name in files:
            path = ROOT / name
            if not path.resolve().is_relative_to(ROOT):
                raise SystemExit(f"Source path leaves checkout: {name}")
            if any(part in {".git", ".gradle", ".kotlin", "build", "__pycache__", "artifacts"} for part in path.relative_to(ROOT).parts):
                continue
            archive.write(path, "Nuraya/" + name)
            source_hashes[name] = sha256(path)
    with zipfile.ZipFile(source_zip) as archive:
        if archive.testzip() is not None:
            raise SystemExit("Source ZIP integrity check failed")
    info = {
        "applicationId": metadata["applicationId"], "versionName": version_name, "versionCode": version_code,
        "buildType": build_type, "minimumAndroidApi": 26, "targetAndroidApi": 35,
        "apk": {"file": result_apk.name, "bytes": result_apk.stat().st_size, "sha256": sha256(result_apk)},
        "sources": {"file": source_zip.name, "bytes": source_zip.stat().st_size, "sha256": sha256(source_zip)},
        "sourceFiles": source_hashes,
        "validationReport": "docs/BUILD_VALIDATION.md",
    }
    (OUTPUT / "build-info.json").write_text(json.dumps(info, indent=2) + "\n")
    (OUTPUT / "SHA256SUMS").write_text("".join(f"{sha256(p)}  {p.name}\n" for p in [result_apk, source_zip, OUTPUT / "build-info.json"]))
    for path in [result_apk, source_zip]:
        print(f"{path}: {path.stat().st_size:,} bytes; SHA-256 {sha256(path)}")


if __name__ == "__main__":
    main()
