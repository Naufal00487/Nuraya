# Build dan validasi Nuraya 1.1.0

Diperiksa pada 25 Oktober 2026 (Asia/Jakarta), checkout `/workspace/Nuraya`. Ini adalah APK pengembangan Android native, application ID `com.nuraya.app`, versionCode **3**, minimum Android 8.0/API 26, target/compile API 35. Laporan versi lama dipertahankan di [BUILD_VALIDATION_1.0.1.md](BUILD_VALIDATION_1.0.1.md).

## Hasil build dan pengujian

```sh
bash scripts/gradle.sh assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue
python3 scripts/import_quran.py --verify
python3 scripts/import_study.py --verify
python3 scripts/import_audio_timing.py --verify
python3 scripts/verify_audio_catalog.py
python3 -m unittest discover -s scripts/tests -v
python3 -m unittest discover -s scripts -p 'test_import_study.py' -v
python3 -m unittest discover -s backend/tests -v
```

Gradle **BUILD SUCCESSFUL**. **73 test JVM lulus; 0 gagal, 0 error, 0 dilewati**. Test Python **21 lulus**: 8 pembuka Quran, 5 pemetaan data belajar, dan 8 backend opsional. Semua verifier offline di atas lulus. Lint selesai tanpa error atau fatal; peringatan nonfatal dicatat dalam [laporan lint](validation/1.1.0/lint.txt). APK instrumentasi berhasil dibangun, tetapi test perangkat belum dijalankan.

| Suite JVM | Test lulus |
| --- | ---: |
| ContentPolicyTest | 2 |
| LearningValidationTest | 6 |
| VideoCatalogueTest | 3 |
| DownloadSafetyTest | 5 |
| VerseAudioCatalogTest | 6 |
| QaLocalPersistenceTest | 4 |
| QaQuranRepositoryTest | 6 |
| QuranOpeningPolicyTest | 6 |
| QuranReaderPresentationTest | 5 |
| QuranRecitationPresentationTest | 4 |
| StudyFeaturePresentationTest | 3 |
| StudyIntegrationTest | 7 |
| RecitationMatcherTest | 11 |
| PracticeRepositoryTest | 5 |

Bukti mesin dan log tersedia di [validation/1.1.0/summary.json](validation/1.1.0/summary.json) serta [gradle-build.log](validation/1.1.0/gradle-build.log). UI diuji dengan Robolectric API 28/native graphics. Render pembaca, hafalan, dan filter Shorts diperiksa secara visual; ini render host, bukan screenshot APK pada HP. Tes regresi mencakup pergantian qari pada ayat yang sama, pembuka surah, preferensi warna/follow, pemetaan kata/tajwid, penyimpanan progres, filter video, dan batas pencocokan suara.

## Data Al-Qur’an dan materi belajar

- Korpus tetap **114 surah, 6.236 ayat, 30 juz**, dengan teks Arab dan terjemahan Indonesia/Inggris sejajar. SHA-256 `quran.json` tetap `f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c`.
- Basmalah pembuka pada **112 surah** tampil terpisah tanpa nomor. Al-Fatihah 1:1 tetap basmalah; At-Taubah tidak mempunyai pembuka; An-Naml 27:30 tetap utuh. Semua 114 pembuka dan seluruh ayat diperiksa; nomor/terjemahan tidak digeser.
- Font Utsmani Amiri Quran tersedia offline. Warna tajwid aktif secara default dan dapat dimatikan di Pengaturan. Hanya anotasi yang sesuai rentang teks kanonis digunakan.
- **77.429 kata** mempunyai arti Indonesia/Inggris; **77.428** dapat dipetakan tepat ke rentang teks. Kata 11:13 posisi 3 tetap mempunyai arti, tetapi tidak diberi sorotan tebakan. **60.023 anotasi tajwid** pada 6.173 ayat lolos pemetaan; 34 anotasi sumber yang tidak cocok dihilangkan.
- **6.236 tafsir Kemenag Indonesia** dibundel melalui EQuran.id. Ini satu edisi tafsir lengkap, bukan semua kitab tafsir. Sumber, checksum dan batas hak data dicatat di [STUDY_DATA.md](STUDY_DATA.md).
- Hafalan rentang/pengulangan, penilaian sendiri, jadwal tinjauan, target harian, 8 materi tajwid dan 10 soal kuis tersedia dengan progres lokal. Penilaian hafalan bukan penilaian otomatis ketepatan bacaan.

## Audio dan sinkronisasi

- Katalog akhir berisi **138 pilihan rekaman lengkap**, termasuk edisi berbeda dari qari yang sama. Manifest setiap pilihan mempunyai 114 surah. Seluruh 138 sampel Al-Fatihah yang dipertahankan telah diunduh penuh dan didekode sebagai audio oleh ffmpeg pada host. Tujuh pilihan yang gagal juga pada pemeriksaan ulang dikeluarkan. Laporan akhir diturunkan dari hasil pemeriksaan tersebut, bukan klaim pengunduhan ulang semua katalog.
- Katalog per ayat berisi **17 qari**, masing-masing dengan manifest 6.236 ayat. Sampel Al-Fatihah 1:1 untuk 17 qari dan 2 ayat fallback telah berhasil diunduh penuh/didekode pada host.
- Sorotan kata Alafasy memakai rekaman QuranicAudio pasangan timestamp Quran.com. Seluruh 114 berkas timing/6.236 ayat diperiksa, dengan **6.142 ayat** aman untuk sorotan kata dan **94 ayat** memakai audio/sorotan per ayat. Tidak ada timing buatan dari panjang tulisan. Qari lain memakai sorotan per ayat.
- Rekaman pasangan Alafasy surah 1 dan 114 berhasil diunduh penuh/didekode. Ini bukan klaim bahwa seluruh 114 rekaman atau seluruh audio 138 pilihan sudah didengarkan/diunduh.

Bukti: [katalog 138](validation/1.1.0/audio-catalog-final-1.1.0.json), [pemeriksaan ulang yang gagal](validation/1.1.0/audio-catalog-recheck-1.1.0.json), [17 qari per ayat](validation/1.1.0/verse-audio-1.1.0.json), [rekaman pasangan Alafasy](validation/1.1.0/paired-audio-final-1.1.0.json). Rincian di [AUDIO_CATALOG.md](AUDIO_CATALOG.md). Validasi unduhan/decode host bukan pengujian pemutaran Android.

## Video dan latihan mikrofon

Katalog memuat **12 rujukan** (8 seri/kursus, 1 lokakarya, 3 Shorts), filter tingkat/format/topik dan pencarian terbatas pada katalog Al-Qur’an. Semua tautan berasal dari rujukan publik yang dipin; tidak ada ID buatan, stream palsu, atau klaim izin redistribusi. **YouTube langsung dibatasi HTTP 403 dari cloud**, sehingga pemutaran, judul terkini, dan keseluruhan isi video belum diverifikasi. Bahasa yang belum dikonfirmasi ditandai di aplikasi. Belum ada materi berbahasa Indonesia yang terverifikasi dalam katalog ini. Lihat [VIDEO_SOURCES.md](VIDEO_SOURCES.md).

Latihan mikrofon menggunakan Android SpeechRecognizer untuk **perkiraan kecocokan kata**, mengutamakan mode di perangkat dan meminta persetujuan sebelum menggunakan penyedia yang dapat memproses online. Izin mikrofon hanya diminta saat memulai; sesi berhenti saat layar ditinggalkan. Rekaman/transkrip tidak disimpan. Hasil sementara ditandai sementara dan tidak dianggap kesalahan final.

**Fitur ini belum mengoreksi tajwid, makhraj, harakat, atau panjang mad.** Test matcher bukan uji kualitas pengenalan suara sebenarnya. Ketersediaan bahasa Arab, hasil parsial, dan kemampuan offline bergantung pada perangkat/penyedia. Tidak ada layanan AI berbayar atau endpoint gratis yang diklaim telah tersedia tanpa bukti. Asisten tanya-jawab terpisah tetap tidak aktif tanpa server. Riset model gratis dan batas fitur dijelaskan di [RECITATION_ASSISTANT.md](RECITATION_ASSISTANT.md).

## Paket dan pembaruan

APK final lolos `apksigner verify --verbose --print-certs` dan `zipalign -c -p 4`. Sertifikat SHA-256 tetap `e2aa60317632bc6aea65f83c2247e54111941d0e0725875a2c9db3ffb93ac264`, sama dengan APK terdahulu. VersionCode meningkat menjadi 3; paket ditujukan untuk dipasang sebagai pembaruan agar data lokal dipertahankan. Keystore pribadi tidak didistribusikan.

`scripts/package-artifacts.py` memeriksa bahwa APK berasal dari build terkini, seluruh asset sama persis dengan source, dan ZIP sumber utuh. Ukuran/hash final dicatat di `build-info.json`/`SHA256SUMS` pada distribusi. Publikasi memakai cabang unduhan terpisah sesuai izin pengguna; verifikasi unduhan HTTPS setelah unggah dicatat dalam kuitansi distribusi. Tidak ada reset atau penggantian checkout sumber asli.

## Batas pengujian perangkat

**Belum ada hasil uji 1.1.0 pada HP fisik.** Mesin cloud tidak memiliki `/dev/kvm`; percobaan emulator software terdahulu mengalami System UI ANR. Lihat laporan historis [1.0.0](BUILD_VALIDATION_1.0.0.md). Pengujian host dan kompilasi instrumentasi tidak diklaim sebagai pengujian perangkat yang lulus.

Pemasangan sebagai pembaruan, speaker/Bluetooth, layanan audio latar belakang, WorkManager, dialog navigasi perangkat, mikrofon, dan pengenal suara Android perlu diuji pada HP/emulator berakselerasi. Tidak ada klaim bebas crash atau ketepatan bacaan agama otomatis. Aplikasi tetap gratis, tanpa iklan, akun wajib, atau keuntungan finansial bagi penyusun.
