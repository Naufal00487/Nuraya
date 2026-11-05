# Android test fixture only

`alafasy1.mp3` is the real Al-Fatihah recitation by Mishary Rashid Alafasy.

- Source: https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/1.mp3
- Retrieved over verified HTTPS: 2026-10-23 (Asia/Jakarta).
- SHA-256: `7c2e67cbde506d25f7e181c3ce876c2a9943a5529ee24a753b2856075a4567fa`
- Size: 1,420,155 bytes. MP3, 44.1 kHz, stereo; duration about 51.913 seconds.
- Source/usage terms: https://alquran.cloud/license (noncommercial audio use).
- Full source attribution and license details are recorded in `docs/DATA_SOURCES.md`.

This file belongs only to the **instrumentation test APK**. It is not included in
the Nuraya application APK and must not be described as a preinstalled user download.
The device test places it in private app storage to exercise the production offline
playback path, then restores any prior state and removes its test-only files.
