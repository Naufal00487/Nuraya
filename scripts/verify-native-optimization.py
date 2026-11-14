#!/usr/bin/env python3
"""Check the actual Android compiler commands, not just the requested CMake flags."""
from __future__ import annotations

import json
import argparse
from pathlib import Path
import shlex

ROOT = Path(__file__).resolve().parents[1]
ABIS = ("arm64-v8a", "armeabi-v7a", "x86_64")


def verify(build_type: str = "debug") -> dict:
    results = []
    for abi in ABIS:
        configuration = "Debug" if build_type == "debug" else "RelWithDebInfo"
        candidates = list((ROOT / f"app/.cxx/{configuration}").glob(f"*/{abi}/compile_commands.json"))
        if not candidates:
            raise SystemExit(f"Build the native Android engine first; missing compiler commands for {abi}")
        path = max(candidates, key=lambda entry: entry.stat().st_mtime)
        commands = json.loads(path.read_text())
        runtime = [row for row in commands if "/src/main/cpp/" in row["file"]]
        if not runtime or not any(row["file"].endswith("quran_whisper_jni.cpp") for row in runtime):
            raise SystemExit(f"Missing Quran runtime compiler commands for {abi}")
        for row in runtime:
            flags = row.get("arguments") or shlex.split(row["command"])
            optimization = [flag for flag in flags if flag in {"-O0", "-O1", "-O2", "-O3", "-Os", "-Oz", "-Og", "-Ofast"}]
            if not optimization or optimization[-1] not in {"-O2", "-O3"}:
                raise SystemExit(f"Unoptimized inference code for {abi}: {row['file']}")
            if "-ffast-math" in flags or "-Ofast" in flags:
                raise SystemExit(f"Unexpected floating point semantics for {abi}: {row['file']}")
        results.append({"abi": abi, "optimizedTranslationUnits": len(runtime), "compileCommands": str(path.relative_to(ROOT))})
    return {"optimizedAndroidRuntimeVerified": True, "abis": results}


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--build-type", choices=("debug", "release"), default="debug")
    print(json.dumps(verify(parser.parse_args().build_type), indent=2))
