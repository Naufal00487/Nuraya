# Build dan validasi Nuraya

Validasi dilakukan pada checkout `/workspace/Nuraya`, 23 Oktober 2026. APK berasal dari proyek ini, bukan berkas contoh atau paket kosong.

## Hasil build

Perintah berikut selesai dengan **BUILD SUCCESSFUL**:

```sh
bash scripts/gradle.sh assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue
```

- APK aplikasi: `app/build/outputs/apk/debug/app-debug.apk`.
- APK pengujian: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.
- Package: `com.nuraya.app`; versi `1.0.0` (kode 1).
- Minimum Android 8.0 / API 26; target dan compile SDK 35.
- ABI: `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`.
- JDK 17.0.14+7, Gradle 8.11.1, AGP 8.9.1, Kotlin 2.1.20.
- Android Lint selesai tanpa error. Peringatan nonfatal menyangkut saran KTX, pemeriksaan ruang penyimpanan, dan sumber daya; laporan lengkap tersedia di `app/build/reports/lint-results-debug.html`.
- `apksigner verify --verbose --print-certs` berhasil: tanda tangan APK v2 valid, sertifikat **Android Debug**.
- `zipalign -c -P 16 4` berhasil.

APK ini merupakan build pengembangan untuk pemasangan dan pengujian, bukan rilis Google Play. Tidak ada penerbitan toko, push Git, atau penggunaan kunci signing pribadi.

## Unit test dan data

**22 unit test lulus; 0 gagal, 0 error.**

| Suite | Lulus | Yang diuji |
| --- | ---: | --- |
| `QaQuranRepositoryTest` | 5 | Korpus lengkap, urutan, 30 juz, referensi valid/tidak valid, sumber qari HTTPS |
| `QaLocalPersistenceTest` | 4 | Room ditutup/dibuka kembali, favorit, penanda, daftar putar, status unduhan, riwayat, DataStore |
| `LearningValidationTest` | 6 | Kutipan diambil dari korpus, penolakan referensi palsu, batas respons, keadaan tidak dikonfigurasi |
| `ContentPolicyTest` | 2 | Kebijakan URL dan izin katalog video |
| `DownloadSafetyTest` | 5 | Identifier, URL, pengalihan, progres dan batas unduhan |

Robolectric memakai Android API 28 untuk pengujian lokal. Folder home khusus proses JVM pengujian berada di `app/build/test-home`; ini memperbaiki kegagalan awal akibat folder home mesin yang hanya bisa dibaca. Variabel shell `HOME` tidak diubah, dan verifikasi TLS tetap aktif.

`python3 scripts/import_quran.py --verify` lulus: **114 surah, 6.236 ayat Arab/Indonesia/Inggris yang sejajar, 30 juz**, dan SHA-256 aset sesuai sumber yang dipin.

`python3 -m unittest discover -s backend/tests -v` lulus: **7 test**. Pemeriksaan ini mencakup HTTP health/503 tanpa konfigurasi, pengambilan sumber lokal, validasi rujukan, dan respons penyedia tiruan yang berlabel sebagai fixture pengujian. Tidak ada permintaan AI berbayar atau kredensial sungguhan yang dipakai.

## Pengujian Android

APK aplikasi dan APK pengujian berhasil dipasang (`adb install`: **Success**) pada emulator Android 15 / API 35. Mesin cloud tidak menyediakan `/dev/kvm`; emulator menggunakan software emulation. System UI pada emulator tersebut mengalami ANR, dan satu peluncuran `am start -W` melewati timeout walaupun log Android kemudian mencatat Activity ditampilkan. Ini belum membuktikan bahwa seluruh navigasi atau pemutaran di perangkat lulus.

Percobaan kedua memakai Android 9 / API 28 x86, resolusi 480×800, dua inti dan RAM 2 GB. Boot selesai, kedua APK terpasang, dan log Android mencatat Activity Nuraya ditampilkan setelah sekitar 27 detik. Namun, System UI emulator kembali mengalami ANR. Runner akhirnya berhenti dengan **`INSTRUMENTATION_ABORTED: System has crashed`**. Pada API 35, proses runner sebelumnya juga dihentikan ketika startup melewati batas waktu dalam emulator yang mengalami ANR.

**Tidak ada device test yang dinyatakan lulus.** Tiga tes navigasi dan dua tes audio telah dicoba melalui runner, tetapi eksekusi tidak menghasilkan hasil test yang selesai. Test live WorkManager/streaming tidak dijalankan setelah sistem emulator gagal. Pemasangan paket terbukti berhasil; kestabilan UI, kontrol layar kunci, pemutaran audio Android, dan unduhan jaringan ujung-ke-ujung masih perlu diperiksa pada HP Android atau emulator dengan akselerasi. Tidak ditemukan exception aplikasi yang dapat diisolasi dari gangguan sistem emulator dalam percobaan ini, tetapi hal itu bukan jaminan bebas crash.

Log diagnostik disertakan di `artifacts/validation/`. Gambar dalam `artifacts/screenshots/` merekam kegagalan emulator dan **bukan** tangkapan layar aplikasi yang berfungsi. Menyusun APK pengujian atau status install berhasil tidak dianggap sebagai pengujian UI/audio yang lulus.

## Audio dan layanan eksternal

- Rekaman Al-Fatihah oleh Mishary Rashid Alafasy berhasil diambil melalui HTTPS dengan verifikasi TLS: 1.420.155 byte, MP3 44,1 kHz stereo, durasi sekitar 51,913 detik. SHA-256 dan sumber tercatat di `app/src/androidTest/assets/README.md`.
- Fixture rekaman hanya berada dalam APK pengujian, **tidak** dibundel sebagai unduhan pengguna pada APK aplikasi.
- Test perangkat offline memeriksa jalur produksi pemilihan berkas privat dan Media3. Test online terpisah memerlukan argumen `nuraya.online=true` serta koneksi HTTPS yang bekerja, dan memeriksa WorkManager → unduhan lengkap → pemutaran → penghapusan.
- Kedua test tersebut tersedia sebagai source. Test offline dicoba tetapi tertahan kegagalan sistem emulator; test online belum dijalankan. Jangan mengubah status ini menjadi lulus berdasarkan validasi MP3 di mesin host.
- Belum ada klaim bahwa seluruh 342 rekaman telah diunduh atau diputar.
- AI belum dikonfigurasi; katalog video belum berisi materi berizin yang ditinjau. Pemutaran video nyata dan kualitas jawaban model live belum diuji.

## Berkas penyerahan

`python3 scripts/package-artifacts.py` memeriksa struktur APK, kesamaan korpus dalam APK dan source, serta waktu build terhadap input source; lalu menghasilkan APK, ZIP source, manifest hash, dan `SHA256SUMS` di `artifacts/`. Ukuran dan SHA-256 final dicatat dalam `artifacts/build-info.json`.

Instruksi setup dan startup cloud disimpan sebagai draft `install_script` dan `start_skill`. Penyimpanan draft tidak sama dengan publikasi lingkungan; pengguna dapat meninjau, menyimpan, lalu memublikasikannya melalui pengaturan lingkungan untuk pemakaian berikutnya.

`bash scripts/setup-android.sh` juga telah dijalankan ulang dan berhasil tanpa menimpa source atau lockfile. Jalur pemakaian ulang emulator yang telah menyala pada `scripts/start-emulator.sh` diuji sampai pemeriksaan boot berhasil; hasil tersebut tidak menyatakan emulator stabil untuk tes aplikasi.
