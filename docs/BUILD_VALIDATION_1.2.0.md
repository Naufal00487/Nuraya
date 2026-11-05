# Build dan validasi Nuraya 1.2.0

Diperiksa pada 26 Oktober 2026 (Asia/Jakarta), checkout `/workspace/Nuraya`. APK pengembangan Android native: `com.nuraya.app`, versionCode **4**, minimum Android 8.0/API 26, target/compile API 35. Riwayat data/audio dan pengujian terdahulu dipertahankan dalam [laporan 1.1.0](BUILD_VALIDATION_1.1.0.md).

## Hasil build dan pengujian

```sh
bash scripts/gradle.sh assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue
python3 scripts/import_quran.py --verify
python3 scripts/import_study.py --verify
python3 scripts/import_audio_timing.py --verify
python3 scripts/import_learning_index.py --verify
python3 -m unittest discover -s scripts/tests -v
python3 -m unittest discover -s scripts -p 'test_import_study.py' -v
python3 -m unittest discover -s backend/tests -v
```

Gradle **BUILD SUCCESSFUL**; **105 test JVM lulus**, tanpa gagal, error, atau test dilewati. **21 test Python lulus** (8 pembuka Quran, 5 pemetaan belajar, 8 backend opsional). Empat verifier offline lulus. Lint selesai dengan **21 peringatan, tanpa error/fatal**. APK instrumentasi berhasil dibangun, tetapi test perangkat belum dijalankan.

Bukti: [ringkasan mesin](validation/1.2.0/summary.json), [log Gradle](validation/1.2.0/gradle-build.log), [lint](validation/1.2.0/lint.txt), dan [verifikasi offline/Python](validation/1.2.0/offline-validation.log). Suite mencakup pembuka surah, data/progres, pencocokan kata, unduhan model, pembatalan/lifecycle sesi AI, pencarian video, aturan embed, kutipan sumber lokal, dan UI Compose. Render UI menggunakan Robolectric API 28/native graphics pada host, bukan screenshot aplikasi di HP.

## AI bacaan

Mesin native `whisper.cpp` v1.7.6 dikompilasi dengan NDK 27.2.12479018/CMake 3.22.1 untuk **arm64-v8a, armeabi-v7a, x86_64**. Model diunduh sekali dan diverifikasi panjang/SHA-256, lalu inferensi berjalan lokal. Audio mikrofon berada di memori; rekaman/transkrip tidak disimpan atau dikirim ke server. Pengenal Android tersedia sebagai pilihan terpisah; pemrosesan online melalui penyedia Android membutuhkan persetujuan di UI.

Model Tarteel `whisper-base-ar-quran` dikonversi ke Q5_1 dari revisi/bobot sumber yang dipin; ukuran **59.707.625 byte**, SHA-256 `1de4498dc16fdee2045e67a97d73cda4624017640dbe5ddf809f3130da7c234a`. [Model immutable](https://raw.githubusercontent.com/Naufal00487/Nuraya/1d5a31aa2e60dbb9af70981e606a9c06b1b18c3e/nuraya-quran-whisper-base-q5_1.bin) telah diunduh penuh kembali, HTTP 200, ukuran dan hash cocok ([bukti](validation/1.2.0/model-download.json)). Atribusi/lisensi Apache-2.0 dan MIT disertakan; lihat [QURAN_AI_MODEL.md](QURAN_AI_MODEL.md).

**7 pemeriksaan JNI native lulus** menggunakan wrapper C++ aplikasi dan model sebenarnya pada host: hening/nonfinite, pembatalan antrean/inferensi, penggunaan ulang konteks, inferensi bacaan, dan penutupan aman. [Log](validation/1.2.0/native-ai-jni.log) dan [probe terkontrol](validation/1.2.0/model-inference.json) mencatat:

- Basmalah lengkap dikenali sebagai empat kata; inferensi host sekitar 11,7–11,9 detik.
- Klip dipotong sebelum kata terakhir menghasilkan tiga kata tanpa menambahkan kata terakhir.
- Saat interval Ar-Rahman dibisukan, model juga kehilangan Ar-Rahim yang masih terdengar. **Kesalahan pengenalan tetap terjadi.**
- Hening ditangani gerbang energi native; ini bukan pengukuran akurasi model.

Tidak ada prompt ayat target yang diberikan kepada ASR. Ini bukan benchmark akurasi dan **tidak membuktikan koreksi tajwid, makhraj, harakat, panjang mad, atau respons real-time di HP**. UI menampilkan perkiraan kecocokan kata dan membedakan hasil sementara/final. Penilaian guru tetap diperlukan.

## Video dan sumber belajar

Pencarian cerdas lokal merangking **12 rujukan Quran yang dikurasi**, memahami kebutuhan belajar/sinonim dan menerapkan filter tingkat/format/bahasa. Ini bukan pencarian langsung seluruh YouTube. Bila hanya materi terkait tersedia, UI menjelaskannya. Bahasa yang belum diverifikasi tetap ditandai; belum ada video bahasa Indonesia terverifikasi di katalog.

Sebelas URL mempunyai bentuk video/Shorts/playlist yang dapat memakai **YouTube IFrame resmi**; halaman kursus mempunyai tautan sumber asli. Pemutar dimuat setelah pengguna menekan tombol, tanpa autoplay, dengan kontrol YouTube dan fallback bila penerbit melarang embed. Percobaan muat ulang mengganti WebView yang tampil. Murottal dan video saling dijeda; riwayat dicatat setelah pemutar melaporkan pemutaran. YouTube dapat menampilkan iklan platform.

Pengujian mencakup parser URL, HTML embed, pencarian, dan layar sebelum pemutaran. **YouTube/oEmbed dari cloud memberi HTTP 403**, sehingga pemutaran aktual, izin embed tiap penerbit, dan judul terkini belum terverifikasi. Lihat [VIDEO_EMBED.md](VIDEO_EMBED.md), [VIDEO_SOURCES.md](VIDEO_SOURCES.md), dan [SMART_VIDEO_SEARCH.md](SMART_VIDEO_SEARCH.md).

Teman Belajar mencari **6.236 terjemahan dan tafsir Kemenag** beserta materi tajwid secara offline, mengembalikan kutipan sumber dengan tautan ayat. Ini bukan jawaban generatif atau layanan AI eksternal. Indeks deterministik lulus verifikasi. Backend opsional lama tidak dipanggil UI ini.

## Regresi Quran dan media

Teks kanonis tetap 114 surah/6.236 ayat; SHA-256 `quran.json` tetap `f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c`. Basmalah pada 112 pembuka terpisah tanpa nomor, Al-Fatihah 1:1 tetap basmalah, At-Taubah tanpa pembuka, An-Naml 27:30 tetap utuh.

Verifier mempertahankan 77.429 arti kata (77.428 rentang tepat), 60.023 anotasi tajwid, dan 6.236 tafsir. Timing Alafasy mempunyai 6.142 ayat aman untuk sorotan kata dan 94 fallback per ayat. Audio 138 pilihan rekaman lengkap dan 17 qari per ayat tidak diubah; bukti unduhan/decode terdahulu di [laporan 1.1.0](BUILD_VALIDATION_1.1.0.md), bukan klaim semua audio diunduh ulang pada versi ini.

## Paket dan batas perangkat

APK lolos `apksigner verify --verbose --print-certs` dan `zipalign -c -P 16 4`. Sertifikat SHA-256 tetap `e2aa60317632bc6aea65f83c2247e54111941d0e0725875a2c9db3ffb93ac264`, sama dengan versi terdahulu. VersionCode meningkat agar dapat dipasang sebagai pembaruan. Keystore pribadi tidak didistribusikan. Sembilan pustaka native pada tiga ABI mempunyai alignment segmen ELF 16 KB; pemeriksaan ZIP bukan pengujian perangkat 16 KB.

`scripts/package-artifacts.py` memastikan APK lebih baru dari sumber build, setiap asset sama persis, dan ZIP sumber utuh. Ukuran/hash tercatat pada `build-info.json`/`SHA256SUMS`. Distribusi memakai cabang unduhan GitHub terpisah yang telah diizinkan pengguna, mempertahankan tautan lama, dan diverifikasi dengan unduhan penuh/hash setelah unggah.

**Belum diuji pada HP fisik atau emulator berakselerasi.** Host tidak mempunyai `/dev/kvm`. Update-install, izin/mikrofon nyata, RAM/baterai/latensi model, audio latar belakang/Bluetooth, WebView/YouTube aktual dan layar penuh masih memerlukan uji perangkat. Hasil host tidak diklaim sebagai hasil perangkat. Nuraya gratis dan penyusun tidak mengambil keuntungan; Nuraya tidak memasang iklan sendiri, sedangkan YouTube dapat menampilkan iklan platform.
