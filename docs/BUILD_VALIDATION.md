# Build dan validasi Nuraya 1.5.1

Diperiksa 12 November 2026. Paket `com.nuraya.app`, versionCode **11**, minimum Android 8/API 26, target API 35. [Perubahan](UPDATE_1.5.1.md).

## Hasil

**BUILD SUCCESSFUL. 180 tes JVM/Compose lulus, 0 gagal/error/dilewati.** Lint: 0 error/fatal, 44 peringatan. APK aplikasi dan APK instrumentasi berhasil dibangun. Instrumentasi belum dijalankan pada HP atau emulator.

```sh
bash scripts/gradle.sh assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue
python3 scripts/verify-indonesia-pages.py /path/to/downloaded/pages --output /path/to/check.json
python3 scripts/verify-native-optimization.py
```

Bukti: [ringkasan](validation/1.5.1/summary.json), [JUnit](validation/1.5.1/junit), [build](validation/1.5.1/gradle-build.log), [lint](validation/1.5.1/lint.txt). Tes mencakup pemisahan sumber/cache/simpanan Madinah dan Indonesia, penolakan gambar rusak, mapping seluruh referensi, navigasi halaman, panel arti, kembali ke pembaca per ayat dan menjaga mode yang dipilih. Tes AI, audio, lokasi/salat, hafalan, profil dan navigasi yang sudah ada ikut lulus. Backend banner tidak berubah dan tidak diuji ulang dalam pembaruan ini.

[Render halaman 2](validation/1.5.1/indonesia-page-two.png) dan [halaman 3](validation/1.5.1/indonesia-page-three.png) diperiksa. Gambar ini merupakan render komponen Compose/Robolectric di host, bukan tangkapan HP fisik.

## Data dan gambar

Semua **604 gambar** sumber berhasil diunduh dan didekode, cocok dengan hash Git blob pada commit yang dipatok, dan memiliki hash SHA-256 berbeda (tidak ada file halaman identik). Empat berkas yang dibundel (1, 2, 3, 604) cocok persis dengan sumber. [Bukti semua berkas](audit/1.5.1/mushaf-indonesia-files.json).

Penanda ayat terakhir pada semua 604 halaman dibandingkan secara visual dengan indeks 604 halaman; tidak ditemukan pergeseran batas halaman. [Cakupan pemeriksaan](audit/1.5.1/mushaf-indonesia.json). Pemeriksaan batas ini tidak menyatakan setiap huruf/harakat sudah ditashih oleh ahli. Tidak ada teks hasil OCR atau gambar Quran buatan yang dipasang.

Data Quran digital 6.236 ayat tetap byte-identik dengan rilis 1.5.0: SHA-256 `f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c`. [Audit corpus sebelumnya](audit/1.5.0/quran-corpus.json) tetap relevan terhadap berkas yang sama; tidak diklaim sebagai unduhan ulang corpus/audio. Pemetaan foto/qari, rekaman, tafsir, Latin, warna tajwid dan timing audio tidak diubah. Sorotan kata tetap tersedia untuk 12 dari 17 qari; lima lainnya per ayat.

Gambar berbingkai **bukan** edisi Islampedia dengan arti per kata tercetak. Arti per kata, terjemahan, Latin opsional, tajwid, tafsir dan sorotan audio tampil di panel pendamping. Sumber dan batas tiap edisi dijelaskan di [MUSHAF_IMAGES.md](MUSHAF_IMAGES.md).

## APK

Ukuran **66,506,072 byte**. SHA-256: `a352eb1ffab8132452917c27c6dc503bde158d6d5f190e0f37ce777c3f790766`.

`apksigner verify --verbose --print-certs` dan `zipalign -c -v -P 16 4` lulus. Sertifikat distribusi tetap `e2aa60317632bc6aea65f83c2247e54111941d0e0725875a2c9db3ffb93ac264`. Optimasi native tiga ABI lulus. [Tanda tangan](validation/1.5.1/apk-signature.txt), [alignment](validation/1.5.1/apk-alignment.txt), [metadata](validation/1.5.1/apk-metadata.txt).

Pengujian pada HP fisik, GPS, mikrofon, kompas, suara azan dan streaming audio tidak dilakukan dalam pembaruan ini. AI lokal tetap perkiraan kata, bukan penilaian tajwid/makhraj tersertifikasi. Bot banner belum di-deploy dan tidak ada pesan Telegram yang dikirim.
