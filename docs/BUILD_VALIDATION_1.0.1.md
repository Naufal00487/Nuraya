# Build dan validasi Nuraya 1.0.1

Pemeriksaan dilakukan pada checkout `/workspace/Nuraya`, 24 Oktober 2026 (Asia/Jakarta). Paket pengembangan ini memperbaiki tampilan ayat pertama dan basmalah, membundel font Quran offline, memperluas qari, dan menambahkan pernyataan nonkomersial.

## Perubahan yang diverifikasi

- `QuranOpeningPolicy` memisahkan tepat 112 basmalah pembuka dari teks ayat pertama. Al-Fatihah 1:1 tetap basmalah; At-Taubah tidak mempunyai pembuka; An-Naml 27:30 tetap utuh. Seluruh nomor dan jumlah ayat tetap 6.236, terjemahan tidak bergeser, dan asset `quran.json` tetap byte-identik dengan snapshot sumber. Pembaca surah/juz, hasil pencarian, berbagi ayat, dan rujukan belajar menggunakan teks ayat yang sebenarnya.
- Teks Tanzil Utsmani dirender dengan font offline Amiri Quran. Font resmi berlisensi SIL OFL 1.1, seluruh karakter Quran yang dibundel tercakup. Arabic menggunakan arah kanan-ke-kiri dan rata kanan pada teks ayat; basmalah pembuka ditempatkan terpisah di tengah.
- Katalog aktif memuat 12 qari, masing-masing 114 surah pada manifest penyedia yang dipin. Ketiga ID qari lama dipertahankan.
- Pernyataan penyusun tidak mengambil keuntungan finansial tersedia dalam Bahasa Indonesia dan Inggris di Pengaturan dan beranda. Aplikasi gratis, tanpa iklan, langganan, atau pembelian dalam aplikasi.

## Build dan pengujian host

```sh
bash scripts/gradle.sh assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue
python3 scripts/import_quran.py --verify
python3 -m unittest discover -s scripts/tests -v
python3 -m unittest discover -s backend/tests -v
python3 scripts/verify_audio_catalog.py --online
```

Build Gradle selesai dengan **BUILD SUCCESSFUL**. **34 test JVM lulus, 0 gagal, 0 error**:

| Suite | Jumlah | Cakupan |
| --- | ---: | --- |
| QaQuranRepositoryTest | 6 | Korpus lengkap, juz, 12 qari, integrasi teks bernomor pada baca/pencarian/rujukan |
| QuranOpeningPolicyTest | 6 | Seluruh pembuka, semua ayat nonpembuka, varian sumber 95/97, Fatihah/Tawbah/Naml |
| QuranReaderPresentationTest | 5 | Komposisi UI sebenarnya: Al-Baqarah, Al-Fatihah, At-Taubah, Juz 30, posisi tersimpan 2:255; pemakaian font Quran |
| QaLocalPersistenceTest | 4 | Room, penanda, favorit, daftar putar, riwayat, unduhan dan DataStore |
| LearningValidationTest | 6 | Rujukan sumber, validasi respons dan layanan belum aktif |
| ContentPolicyTest | 2 | URL dan hak konten video |
| DownloadSafetyTest | 5 | Identifier, URL, redirect dan batas unduhan |

UI host memakai Robolectric API 28 dengan native graphics. Tangkapan yang dihasilkan dari komposisi pembaca nyata diperiksa untuk basmalah terpisah dan bentuk huruf Arab. Gambar ini merupakan hasil render host, bukan screenshot HP/emulator yang sedang menjalankan APK.

Validasi Python: **8 test pembuka Quran** dan **8 test backend** lulus. Corpus verifier memeriksa seluruh 114 pembuka, 112 rentang basmalah terpisah, 6.236 ayat sejajar pada tiga bahasa, 30 juz, dan checksum asset. Backend belajar opsional juga memakai kebijakan pemisahan yang sama; tidak ada permintaan model berbayar.

Lint selesai tanpa error. Peringatan nonfatal dan laporan penuh berada di `app/build/reports/lint-results-debug.html`. APK test berhasil disusun; ini bukan klaim test perangkat lulus.

## Audio

`verify_audio_catalog.py --online` memverifikasi manifest HTTPS dan **12 unduhan Al-Fatihah lengkap beserta decode MP3 oleh ffmpeg pada host**. Detail tiap qari tersedia di [AUDIO_CATALOG.md](AUDIO_CATALOG.md); laporan mesin berada di `artifacts/validation/audio-catalog.json`. Beberapa MP3 memiliki gambar sampul terlampir, sehingga validasi codec memeriksa stream audio secara khusus.

Ini bukan pengujian pemutaran Android dan bukan klaim bahwa seluruh 1.368 rekaman telah diunduh. Streaming membutuhkan internet; hanya unduhan yang selesai dapat digunakan offline.

## Paket dan pembaruan

- Application ID `com.nuraya.app`, versi **1.0.1**, versionCode **2**.
- Minimum Android 8.0 / API 26, target/compile API 35.
- APK pengembangan ditandatangani dengan sertifikat debug yang sama dengan 1.0.0, sehingga dapat dipasang sebagai pembaruan. Schema Room dan kunci preferensi tidak berubah.
- Verifikasi tanda tangan `apksigner` dan alignment `zipalign` dilakukan pada APK final. Ukuran/hash dicatat di `artifacts/build-info.json` dan `SHA256SUMS`.
- APK dan source ZIP diunggah ke cabang unduhan GitHub yang terpisah setelah izin publikasi pengguna. Tautan immutable diperiksa dengan mengunduh ulang dan membandingkan SHA-256.

## Batas pengujian perangkat

Mesin cloud ini tidak memiliki `/dev/kvm`. Percobaan emulator versi 1.0.0 sebelumnya mengalami System UI ANR sehingga test perangkat tidak selesai; rinciannya dipertahankan di [BUILD_VALIDATION_1.0.0.md](BUILD_VALIDATION_1.0.0.md). Pada pembaruan ini, test presentasi pembaca dijalankan melalui Robolectric sehingga teks, penomoran dan penggunaan font dapat diperiksa tanpa emulator tersebut.

Belum ada hasil test versi 1.0.1 pada HP fisik. Dialog lompat ayat mengalami masalah idling pada host Robolectric; pengujian interaksi dialog dipindahkan ke `androidTest` dan baru dikompilasi, belum dieksekusi pada perangkat. Posisi buka ayat tersimpan 2:255 telah lulus pada host. Kontrol media, speaker/Bluetooth, background playback, unduhan WorkManager dan pemasangan pembaruan masih perlu diuji di HP Android/emulator berakselerasi. Tidak ada klaim bebas crash.

AI dan akun masih belum aktif; katalog video masih kosong. Paket ini tetap edisi pengembangan.
