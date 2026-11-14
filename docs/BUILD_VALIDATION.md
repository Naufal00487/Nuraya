# Build dan validasi Nuraya 1.6.1

Paket `com.nuraya.app`, versionCode **15**, minimum Android 8/API 26, target API 35. Diperiksa 14 November 2026 (Asia/Jakarta). [Perubahan](UPDATE_1.6.1.md).

## Hasil

**212 tes JVM/Compose lulus; 0 gagal, error, atau dilewati.** Suite lengkap pada varian debug menguji kode aplikasi yang sama. APK distribusi kemudian dibangun sebagai release tanpa komponen debug UI; tidak ada perubahan kode fitur setelah suite lulus. Lint: **0 error/fatal, 61 peringatan**. APK aplikasi dan APK instrumentasi berhasil dibangun; instrumentasi belum dijalankan pada perangkat.

```sh
bash scripts/gradle.sh testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
bash scripts/gradle.sh assembleRelease lintRelease
python3 scripts/import_semantic_index.py --verify
python3 scripts/import_quran.py --verify
python3 scripts/import_printed_mushaf.py --verify
python3 scripts/verify-native-optimization.py --build-type release
python3 scripts/package-artifacts.py --build-type release
```

Bukti: [ringkasan](validation/1.6.1/summary.json), [JUnit lengkap](validation/1.6.1/junit), [build](validation/1.6.1/gradle-build.log), [lint debug](validation/1.6.1/lint.txt), [build release](validation/1.6.1/release-build.log), [lint release](validation/1.6.1/lint-release.txt). Lint release: 0 error dan 61 peringatan.

## Perubahan yang diperiksa

| Bagian | Hasil |
| --- | --- |
| Aset dokumen | Sepuluh PNG sampul, dua PNG banner dan satu JPEG avatar sama persis byte/hash dengan dokumen. Madinah & Tajwid memakai sampul Madinah sesuai konfirmasi pengguna; tidak memasukkan Bahriyah. Gambar ditampilkan proporsional. |
| Banner | Kedua gambar tampil utuh dengan rasio asli 5300:1331. Ketuk banner belajar membuka pencarian; banner fitur membuka Quran; kembali berfungsi. |
| Profil | Kelima avatar tersedia; menyimpan avatar baru mempertahankan nama panggilan dan penyimpanan profil lokal. |
| Warna | Status/navigation bar transparan, kontras otomatis Android dimatikan, ikon menyesuaikan tema/rute. Mushaf cetak tetap krem pada tema gelap. Tombol dan panel mushaf memakai permukaan solid. |
| Pencarian makna | Tujuh topik bahasa sehari-hari menghasilkan rujukan yang relevan dalam lima hasil; setiap ayat dan kutipan tafsir sama dengan database. Rujukan salah, kueri tidak dikenal dan kueri di luar lingkup tidak menghasilkan jawaban rekaan. Model serta 115 hash sumber diverifikasi. |
| Pencarian suara | Potongan Arab di tengah 2:255, 94:5, 112:1 dan frasa berulang Ar-Rahman ditemukan; frasa sangat pendek/tidak sesuai ditolak. Panel suara terhubung dari Quran; tanpa model/mesin siap tombol rekam nonaktif; menutup panel dan membuka sumber 1:3 kembali ke layar asal dengan benar. |
| Regresi membaca | Seluruh pengujian sebelumnya tetap lulus: 114 surah, halaman jauh An-Nas, viewport layar penuh, tekan lama/sorotan ayat, panel arti, audio lintas surah, pengulangan, Hafalan, Tikrar, Waqaf, identitas qari, serta timing. |
| Fitur lain | AI/mikrofon simulasi dan lifecycle, izin lokasi, profil wajib, navigasi, pengaturan, kuis, progres dan penyimpanan tetap lulus. |

Render Compose/Robolectric yang diperiksa (bukan foto HP): [banner belajar](validation/1.6.1/home-banner-learn.png), [banner fitur](validation/1.6.1/home-banner-explore.png), [sampul Madinah](validation/1.6.1/madinah-covers.png), [avatar](validation/1.6.1/profile-avatars.png), [pencarian suara tanpa mesin host](validation/1.6.1/voice-search.png), [kontrol krem](validation/1.6.1/madinah-tajwid-8-controls.png), [panel arti](validation/1.6.1/per-kata-translation.png), [pengaturan audio](validation/1.6.1/audio-cross-surah.png).

## Integritas data dan APK

Tidak ada perubahan pada teks Quran, tafsir, terjemahan, latin, anotasi tajwid/waqaf, gambar halaman mushaf, indeks ayat/surah, rekaman/ID qari atau timing. Teks kanonis tetap SHA-256 `f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c`. Verifikasi mencakup 114 surah, 6.236 ayat, 112 pembukaan terpisah, tujuh indeks edisi 604 halaman, enam peta ayat lengkap, dan 42 PNG bundel yang tidak berubah. Pemeriksaan penuh sumber 4.228 gambar dari 1.6.0 tetap berlaku karena manifest dan aset tersebut tidak diubah.

APK **94.653.395 byte**; SHA-256 `b23c8441d5fcab50d342623b689d2a064dc22548e9033e7507ae59e03405af8c`. `apksigner verify` dan `zipalign -c -P 16 4` lulus. Sertifikat sama dengan rilis sebelumnya: `e2aa60317632bc6aea65f83c2247e54111941d0e0725875a2c9db3ffb93ac264`. Native ketiga ABI memakai optimasi yang diperlukan. Pengemasan membandingkan setiap aset APK dengan source secara byte. APK distribusi menggunakan build release (debuggable=false), dengan sertifikat distribusi yang sudah digunakan sebelumnya. Seluruh gambar/data tetap utuh; APK terbaru tersedia langsung di folder artifacts repo.

[Tanda tangan](validation/1.6.1/apk-signature.txt), [alignment](validation/1.6.1/apk-alignment.txt), [metadata](validation/1.6.1/apk-metadata.txt), [optimasi native](validation/1.6.1/native-optimization.json).

## Batas pengujian

Belum diuji pada HP fisik untuk mikrofon/pengenalan suara, streaming audio, GPS, kompas, azan atau unduhan 604 halaman melalui UI Android. Tes suara memeriksa matcher pada transkrip contoh dan siklus capture melalui simulasi, bukan akurasi bacaan manusia pada semua HP. Model LSA merupakan pencarian statistik, bukan LLM atau penafsir agama; tujuh kueri uji bukan jaminan relevansi semua pertanyaan. AI bacaan tetap belum menilai tajwid/makhraj secara tervalidasi. Backend banner tidak diubah atau di-deploy.
