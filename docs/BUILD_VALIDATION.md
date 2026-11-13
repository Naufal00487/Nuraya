# Build dan validasi Nuraya 1.6.0

Paket `com.nuraya.app`, versionCode **14**, minimum Android 8/API 26, target API 35. Penyelesaian diperiksa 14 November 2026 (Asia/Jakarta). [Perubahan](UPDATE_1.6.0.md).

## Hasil

**205 tes JVM/Compose berbeda lulus, 0 gagal/error/dilewati pada hasil akhir.** Lint: 0 error/fatal dan 61 peringatan. APK aplikasi serta APK instrumentasi berhasil dibangun; instrumentasi belum dijalankan pada perangkat.

Suite lengkap dijalankan, kemudian kelas yang terdampak dijalankan ulang setelah memperjelas selector tes Waqaf (judul ada pada header dan dialog) dan keterangan kelompok hafalan manual. Hasil JUnit menyimpan hasil terbaru setiap kelas, bukan mengklaim semua tes berjalan ulang pada satu perintah akhir.

```sh
bash scripts/gradle.sh testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
bash scripts/gradle.sh testDebugUnitTest --tests '*PrintedMushafPresentationTest' lintDebug assembleDebug assembleDebugAndroidTest
bash scripts/gradle.sh testDebugUnitTest --tests '*ReadingRegression153Test' lintDebug assembleDebug assembleDebugAndroidTest
python3 scripts/import_printed_mushaf.py --verify
python3 scripts/import_quran.py --verify
python3 scripts/verify-native-optimization.py
```

Bukti: [ringkasan](validation/1.6.0/summary.json), [JUnit](validation/1.6.0/junit), [build akhir](validation/1.6.0/gradle-build.log), [lint](validation/1.6.0/lint.txt).

## Yang diperiksa

| Bagian | Cakupan |
| --- | --- |
| Tujuh edisi | Setiap edisi tepat 604 URL pada commit tetap dan 604 hash unik. Seluruh 4.228 sumber PNG diunduh/didekode; 42 gambar bundel cocok byte/hash, ukuran dan transparansinya. |
| Indeks/sorotan | Seluruh 6.236 ayat dan 114 awal surah diperiksa per edisi. Enam edisi berpeta mencakup 6.236 ayat, koordinat dibatasi ukuran gambar. 10:95 dan 69:35 memakai koreksi visual yang terdokumentasi. Hafalan tidak memakai peta edisi lain. |
| Halaman dan sentuhan | Gambar Per Kata penuh, tekan lama 1:3 memilih ayat tepat, panel tidak memperkecil halaman, transformasi koordinat diuji dalam letterbox/lanskap/zoom, Madinah Tajwid p8 memakai gambar aslinya. |
| Surah jauh | An-Nas pada Latin Tajwid tetap p604 meskipun audio lama 2:6 tersimpan; tombol putar meminta 114:1. Regresi semua 114 surah dan pembaca lama tetap lulus. |
| Audio lintas surah | Rentang 8:74–9:2 dan pengulangan mempertahankan urutan kanonis tanpa ayat basmalah tambahan; seluruh 6.236 ayat tersusun tepat. Formulir menolak 114:7 dan menerima 113:4–114:6 dengan qari yang sama. Timing disimpan menurut surah rekaman. |
| Mengikuti audio | Perpindahan lintas surah hanya diikuti setelah pengguna memulai antrean dari pembaca; audio lama/jeda/galat atau pengaturan nonaktif tidak mengubah posisi. Pemilihan ayat pada dialog tidak ditimpa transisi audio. |
| Latihan | Halaman Hafalan dan sembunyikan/tampilkan, penghitung Tikrar per ayat, panduan Waqaf termasuk ayat tanpa tanda internal, serta penjelasan kelompok hafalan manual yang berbeda dari blok cetak. |
| Regresi fitur lama | Navigasi, profil wajib, Ibadah/Pengaturan pada huruf besar, AI/mikrofon simulasi, kuis, progres, penyimpanan, identitas qari dan timing yang sudah ada. |

Render host yang diperiksa: [Per Kata](validation/1.6.0/per-kata-full.png), [tindakan ayat](validation/1.6.0/per-kata-verse-actions.png), [panel arti](validation/1.6.0/per-kata-translation.png), [Latin An-Nas](validation/1.6.0/latin-an-nas-full.png), [Madinah Tajwid](validation/1.6.0/madinah-tajwid-8-controls.png), [Hafalan](validation/1.6.0/hafalan-full.png), [pengaturan audio](validation/1.6.0/audio-cross-surah.png), dan [Waqaf](validation/1.6.0/waqaf-guide.png). Ini render Compose/Robolectric, bukan tangkapan layar HP fisik.

## Data dan APK

Teks kanonis tetap SHA-256 `f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c`. Audit ulang menemukan 114 surah/6.236 ayat, 112 pembukaan terpisah dan tanpa ID duplikat. Teks berulang yang memang ada dalam Quran tidak dihapus. Latin, tafsir, tanda waqaf dan tajwid digital tetap memakai sumber sebelumnya. [Audit korpus](audit/1.6.0/quran-corpus.json), [bantuan baca](audit/1.6.0/reading-guidance.json), [4.228 gambar](audit/1.6.0/printed-pages.json), [sumber/batas mushaf](PRINTED_MUSHAF.md).

APK **75.126.455 byte**; SHA-256 `ddbff089cde81d1c454990cc245e4f394d0b08e265f4f5357f8aa0dd3ea21824`. `apksigner verify` dan `zipalign -c -v -P 16 4` lulus. Sertifikat sama dengan rilis sebelumnya: `e2aa60317632bc6aea65f83c2247e54111941d0e0725875a2c9db3ffb93ac264`. Optimasi native tiga ABI lulus. Pengemasan membandingkan setiap aset APK dengan source secara byte.

[Tanda tangan](validation/1.6.0/apk-signature.txt), [alignment](validation/1.6.0/apk-alignment.txt), [metadata](validation/1.6.0/apk-metadata.txt), [optimasi native](validation/1.6.0/native-optimization.json).

## Batas pengujian

Belum ada uji HP fisik untuk streaming audio, unduhan seluruh edisi melalui UI Android, GPS, mikrofon, kompas atau azan pada pembaruan ini. Integritas sumber unduhan diuji di host, bukan klaim unduhan 604 halaman berhasil dijalankan pada setiap HP. Pemeriksaan hash/dekode bukan pentashihan seluruh huruf/harakat oleh ahli. Sorotan gambar per ayat tersedia pada enam edisi; Hafalan belum mempunyai peta. AI tetap bantuan perbandingan kata, bukan penilai tajwid/makhraj tervalidasi. Backend banner tidak berubah dan tidak di-deploy.
