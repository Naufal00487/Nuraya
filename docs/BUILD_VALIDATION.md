# Build dan validasi Nuraya 1.5.2

Diperiksa 12 November 2026. Paket `com.nuraya.app`, versionCode **12**, minimum Android 8/API 26, target API 35. [Perubahan](UPDATE_1.5.2.md).

## Hasil

**BUILD SUCCESSFUL. 185 tes JVM/Compose lulus, 0 gagal/error/dilewati.** Lint: 0 error/fatal, 44 peringatan. APK aplikasi dan APK instrumentasi berhasil dibangun. Instrumentasi belum dijalankan pada HP atau emulator.

```sh
bash scripts/gradle.sh assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue
python3 scripts/verify-native-optimization.py
```

Bukti: [ringkasan](validation/1.5.2/summary.json), [JUnit](validation/1.5.2/junit), [build](validation/1.5.2/gradle-build.log), [lint](validation/1.5.2/lint.txt). Tes AI, audio, lokasi/salat, hafalan, profil dan navigasi yang sudah ada ikut lulus. Backend banner tidak berubah dan tidak diuji ulang dalam pembaruan ini.

## Audit seluruh jalur pembaca

| Jalur | Hasil pemeriksaan |
| --- | --- |
| Per ayat, per kata/tajwid, Latin/tajwid, Indonesia, Indonesia/tajwid, Madinah, Madinah/tajwid, hafalan 8 blok, Tikrar, waqaf/ibtida | Kesepuluh pilihan dibuka dari menu aplikasi dalam tes navigasi. Masing-masing memuat gambar halaman; tidak langsung membuka daftar ayat. Toolbar/menu baca dan navigasi utama tidak muncul pada pembukaan awal. |
| Terjemah tematik | Rujukan 112:1 membuka halaman Indonesia 604, lalu kembali ke tema yang dipilih. |
| Lanjutkan bacaan di beranda | Setelah berpindah ke halaman 2, riwayat memulihkan halaman kitab tersebut dengan menu tersembunyi. |
| Ukuran halaman dan menu | Batas viewport sama dengan seluruh layar komponen; sebelum/sesudah menu dibuka ukurannya identik. Geser kanan 1→2, kiri 2→1, serta batas halaman pertama diuji. |
| Latihan/per ayat → Buka tampilan kitab | Kembali menuju daftar surah tanpa berputar terus antara kitab dan latihan. |
| Audio yang sudah dipilih | Pemutar mini muncul pada layar biasa, tetapi tidak mengambil ruang pembaca kitab ataupun Bahriyah. Tes ini memeriksa state/tampilan; tidak mengklaim keluaran suara fisik. |
| Bahriyah | Tidak diganti dengan edisi lain. Tes pilihan berkas tersimpan dan kegagalan akses tetap lulus. Alur presentasi dokumen tersimpan diuji memakai fixture bitmap hasil render untuk memeriksa layar penuh dan overlay. Robolectric API 28 tidak menyediakan renderer PDF native, sehingga decoding PDF Android belum diuji pada perangkat. |
| Bantuan baca | Panel arti/tafsir, pencarian qari, pembesaran, font sistem 1,6×, lompat halaman, permintaan lanskap dan gulir lanskap lulus. |

[Halaman Indonesia tanpa menu](validation/1.5.2/indonesia-fullscreen.png), [halaman 2](validation/1.5.2/indonesia-fullscreen-page-two.png), [menu sebagai overlay](validation/1.5.2/indonesia-overlay.png), [Madinah](validation/1.5.2/madinah-page.png), dan [lanskap dengan menu](validation/1.5.2/madinah-landscape.png) diperiksa secara visual. Ini render Compose/Robolectric di host, bukan tangkapan HP fisik. Rasio asli halaman dipertahankan; layar yang lebih tinggi dapat menyisakan ruang putih. Pembesaran tersedia melalui menu.

## Data dan gambar

Tidak ada perubahan berkas dalam `app/src/main/assets` pada 1.5.2. Gambar, manifest/hash, indeks ayat, teks Arab, terjemahan, Latin, waqaf, tajwid, qari, foto dan timing audio sama dengan 1.5.1. SHA-256 `quran.json` tetap `f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c`. Pengemasan membandingkan setiap asset APK dengan source secara byte.

Audit sumber 1.5.1 tetap relevan: [604 berkas gambar unik dan checksum](audit/1.5.1/mushaf-indonesia-files.json), [pemeriksaan batas halaman](audit/1.5.1/mushaf-indonesia.json), serta [audit corpus](audit/1.5.0/quran-corpus.json). Sumber tidak diunduh ulang untuk pembaruan UI ini. Pemeriksaan tidak mengklaim semua huruf/harakat telah ditashih oleh ahli.

Gambar berbingkai **bukan** edisi Islampedia dengan arti per kata tercetak. Arti per kata, terjemahan, Latin opsional, tajwid, tafsir dan sorotan audio tetap berada di panel pendamping. Sumber dan batas tiap edisi dijelaskan di [MUSHAF_IMAGES.md](MUSHAF_IMAGES.md). Sorotan kata tersedia untuk 12 dari 17 qari; lima lainnya per ayat.

## APK

Ukuran **66.522.456 byte**. SHA-256: `01faa4e920cddc7bbf9b4a396dcf11345f859e4e2b321963968cb07d6eff9b3c`.

`apksigner verify --verbose --print-certs` dan `zipalign -c -v -P 16 4` lulus. Sertifikat distribusi tetap `e2aa60317632bc6aea65f83c2247e54111941d0e0725875a2c9db3ffb93ac264`. Optimasi native tiga ABI lulus. [Tanda tangan](validation/1.5.2/apk-signature.txt), [alignment](validation/1.5.2/apk-alignment.txt), [metadata](validation/1.5.2/apk-metadata.txt), [optimasi native](validation/1.5.2/native-optimization.json).

Pengujian pada HP fisik, GPS, mikrofon, kompas, suara azan dan streaming audio tidak dilakukan dalam pembaruan ini. AI lokal tetap perkiraan kata, bukan penilaian tajwid/makhraj tersertifikasi. Bot banner belum di-deploy dan tidak ada pesan Telegram yang dikirim.
