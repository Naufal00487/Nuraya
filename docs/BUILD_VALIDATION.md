# Build dan validasi Nuraya 1.7.0

Paket `com.nuraya.app`, versionCode **19**, Android 8/API 26+, target API 35. Diperiksa `2026-11-16T07:09:22.957494+07:00`. [Perubahan](UPDATE_1.7.0.md).

## Hasil

**277 tes JVM/Compose lulus; 0 gagal, error atau dilewati.** Lint debug/release: **0 error/fatal**, masing-masing 79 / 79 peringatan. APK release dan APK instrumentasi berhasil dibangun; instrumentasi belum dijalankan pada HP.

```sh
bash scripts/gradle.sh testDebugUnitTest lintDebug assembleRelease lintRelease assembleDebugAndroidTest
python3 scripts/import_quran.py --verify
python3 scripts/import_printed_mushaf.py --verify
python3 scripts/import_semantic_index.py --verify
python3 scripts/verify-native-optimization.py --build-type release
python3 scripts/package-artifacts.py --build-type release
```

Bukti: [ringkasan](validation/1.7.0/summary.json), [JUnit](validation/1.7.0/junit), [log build](validation/1.7.0/gradle-build.log), [lint debug](validation/1.7.0/lint-debug.txt), [lint release](validation/1.7.0/lint-release.txt).

## Pemeriksaan perubahan

| Bagian | Yang diperiksa |
| --- | --- |
| Materi | 25 pelajaran; kedua jenis memiliki ketiga level tanpa persyaratan progres. 48 pertanyaan editorial. Referensi ayat valid. Pilihan ayat, urutan dan hafalan memakai teks kanonis; pilihan dan urutan diverifikasi pada 26 seed per pelajaran. Setiap ayat surah tercakup dalam pemeriksaan mandiri. |
| Penyimpanan | Pembukaan kembali DataStore mempertahankan ID sesi, seed, jawaban diperiksa dan antrean ulang. Data v1 lama tetap tersimpan tanpa XP rekaan. Ketukan duplikat/stale tidak menggandakan jawaban atau XP. |
| Penilaian | Kesalahan diulang sekali, tanpa penalti atau syarat buka level. Skor percobaan pertama terpisah dari rating hafalan. 20 XP awal dan 5 XP pengulangan, tanggal penyelesaian lokal, jeda belajar, lencana dan target default mati diuji. |
| Interaksi | Buka level lanjutan sejak awal; mulai, jawab keliru, jeda, lanjut, pasangan, urutan, tampilkan ayat, rating, hasil dan progres. Tombol periksa terkunci sampai pilihan lengkap; rating hafalan memerlukan tampilkan teks. Target diaktifkan/diubah/dimatikan. |
| Tampilan | Render native tema biru/gelap pada 411×891 dan 320×640 dengan font 1,6×. Tombol utama tetap bisa dijangkau. Hafalan rentang bebas masih mempertahankan rentang/teks saat beralih ke Progres. |
| Regresi | Tes Quran/mushaf, pencarian Indonesia, 30 qari, hadis, AI, profil, pengingat Quran/salat, izin lokasi dan pemisahan audio pembaca/Murottal tetap lulus. |

Render native Robolectric, bukan foto HP: [jalur belajar](validation/1.7.0/path-basic.png), [pratinjau](validation/1.7.0/advanced-preview.png), [soal](validation/1.7.0/question-blue.png), [pembahasan](validation/1.7.0/answer-explanation.png), [pasangan](validation/1.7.0/match-exercise.png), [urutan ayat](validation/1.7.0/verse-order.png), [hafalan](validation/1.7.0/recall-self-check.png), [hasil](validation/1.7.0/completed-lesson.png), [font besar](validation/1.7.0/large-font-feedback.png), [progres gelap](validation/1.7.0/progress-dark.png).

## Integritas dan distribusi

Teks kanonis Quran tidak berubah: SHA-256 `f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c`. Audit memverifikasi 114 surah/6.236 ayat, tujuh edisi 604 halaman, enam peta ayat, 42 PNG bundel serta 115 hash korpus indeks pencarian. Data Quran, gambar mushaf, qari dan timing sama dengan 1.6.4. Audit streaming qari tidak diulang karena pemetaannya tidak berubah. Aset pembelajaran baru hanya `learning_course.json`.

APK **103,090,468 byte**, SHA-256 `eb744088783c1b967e5a64d366673df8ddf7c8258bd3ddabb4e52ff1dde2a50c`. `apksigner verify` dan `zipalign -c -P 16 4` lulus; sertifikat distribusi `e2aa60317632bc6aea65f83c2247e54111941d0e0725875a2c9db3ffb93ac264` tetap sama. Native tiga ABI teroptimasi. Pengemasan membandingkan semua aset APK dengan source secara byte. Hanya APK terbaru disimpan pada tree GitHub utama.

[Tanda tangan](validation/1.7.0/apk-signature.txt), [alignment](validation/1.7.0/apk-alignment.txt), [metadata](validation/1.7.0/apk-metadata.txt), [native](validation/1.7.0/native-optimization.json).

## Batas pemeriksaan

Tes interaksi menggunakan Android host Robolectric. Instalasi/pembaruan, suara qari, mikrofon, GPS, kompas, azan dan pengiriman notifikasi saat layar mati belum diuji pada HP fisik dalam rilis ini. APK referensi Duolingo 6.105.2 diperiksa statis, tidak dijalankan; tidak ada aset/kode dari APK itu yang dimasukkan ke Nuraya. Materi tajwid belum ditinjau independen oleh pengajar. Rating hafalan adalah penilaian pengguna; AI bacaan yang sudah ada tetap memperkirakan kata, bukan penilai tajwid/makhraj tervalidasi. [Panduan pembelajaran](LEARNING.md).
