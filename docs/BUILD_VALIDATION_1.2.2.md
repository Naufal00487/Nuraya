# Build dan validasi Nuraya 1.2.2

Diperiksa 27 Oktober 2026. APK pengembangan `com.nuraya.app`, versionCode **6**, minimum Android 8/API 26, target API 35. Pembaruan ini mengganti seluruh katalog video sesuai tujuh tautan berbahasa Indonesia yang diberikan pengguna. Laporan sebelumnya dipertahankan di [1.2.1](BUILD_VALIDATION_1.2.1.md).

## Perubahan

- Seluruh 12 rujukan lama diganti dengan **4 video dan 3 playlist** dari rekomendasi pengguna. Tidak ada tautan tambahan yang ditebak. Nama kanal video tiga maqam yang tidak disebutkan pengguna tidak diatribusikan secara palsu.
- Jalur belajar pemula → menengah → lanjutan mengutamakan tajwid, pengucapan, kelancaran, lalu maqam. Dua playlist tersedia pada filter menengah dan lanjutan tanpa menggandakan URL.
- Pencarian mengenali sifat huruf, kelancaran, nama maqam dan variasi ejaannya, serta permintaan bahasa audio. Nama maqam tetap menjadi syarat hasil: mencari Rost tidak otomatis menampilkan semua materi maqam.
- Daftar baru tidak berisi Shorts. Shorts lama dihapus dan keadaan kosong tidak mengembalikan sumber lama ketika filter dibersihkan. Komponen feed tetap dipertahankan dan diuji dengan data uji terpisah, bukan dengan menyebut video panjang sebagai Shorts.
- Koleksi menyembunyikan entri video tersimpan/riwayat yang tidak lagi ada dalam katalog. Data bacaan, audio, unduhan dan penanda lainnya tetap dipertahankan.
- Metadata mencatat rekomendasi pengguna secara jujur: `audioLanguage: id`, `metadataSource: user-provided`, tanggal penambahan, dan tanggal verifikasi kosong. Label tanggal pemeriksaan tidak ditampilkan jika belum ada pemeriksaan. Bentuk ketujuh URL valid untuk parser embed; hal ini tidak membuktikan izin atau keberhasilan pemutaran.

## Pengujian yang dijalankan

```sh
bash scripts/gradle.sh assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue
python3 scripts/verify-native-optimization.py
```

**BUILD SUCCESSFUL. 115 tes JVM lulus, 0 gagal/error/dilewati.** Lint: 21 peringatan, tanpa error/fatal. APK instrumentasi berhasil dibangun; tes perangkat tidak dijalankan untuk versi ini. Bukti: [ringkasan](validation/1.2.2/summary.json), [log Gradle](validation/1.2.2/gradle-build.log), [lint](validation/1.2.2/lint.txt).

Pemeriksaan mencakup tujuh URL persis sesuai permintaan, 4 video/3 playlist, metadata Indonesia dan asal rekomendasi, filter lanjutan, parser embed, pencarian maqam/sinonim/bahasa, serta penolakan permintaan di luar konteks. Pengujian UI memeriksa hasil pencarian pemula, tombol menonton dalam aplikasi, dan keadaan Shorts kosong setelah perubahan filter. Perilaku navigasi/disposal feed memakai pemutar dan entri uji terkontrol; tidak mengklaim streaming YouTube.

Cuplikan tampilan host: [pencarian pemula](validation/1.2.2/video-smart-search.png), [detail playlist Indonesia](validation/1.2.2/video-embed-ready.png). Cuplikan ini berasal dari pengujian Compose/Robolectric, bukan tangkapan HP fisik.

## Integritas build

APK lolos `apksigner verify --verbose --print-certs` dan `zipalign -c -P 16 4`. Sertifikat distribusi tetap `e2aa60317632bc6aea65f83c2247e54111941d0e0725875a2c9db3ffb93ac264`, sehingga versi ini dapat dipasang sebagai pembaruan. [Tanda tangan](validation/1.2.2/apk-signature.txt), [metadata APK](validation/1.2.2/apk-metadata.txt).

Guard compiler memeriksa 25 unit kompilasi native yang teroptimasi pada masing-masing arm64-v8a, armeabi-v7a dan x86_64: [bukti](validation/1.2.2/native-compile-optimization.json). Perbaikan AI/mikrofon dan embed pada 1.2.1 dipertahankan. Model, data Quran, tafsir, tajwid dan audio tidak diubah. Benchmark model dan browser terkontrol pada [laporan 1.2.1](BUILD_VALIDATION_1.2.1.md) merupakan bukti terdahulu, tidak diklaim dijalankan ulang untuk penggantian katalog ini.

`scripts/package-artifacts.py` memeriksa bahwa APK lebih baru dari input build, semua aset APK sama persis dengan sumber, native teroptimasi, dan ZIP sumber utuh. Keystore, cache build serta bobot model tidak masuk ZIP. Metadata distribusi memuat ukuran dan SHA-256; unduhan publik diperiksa penuh setelah unggah. Versi lama dipertahankan.

## Batas verifikasi

Judul, bahasa, kanal dan perkiraan durasi mengikuti daftar pengguna; akses YouTube dari cloud mendapat HTTP 403 termasuk percobaan dengan izin tambahan. Isi video, metadata YouTube terkini, ketersediaan setiap playlist dan izin embed belum diperiksa secara independen. Katalog tetap memakai pemutar resmi YouTube dan tidak melewati pembatasan penerbit.

Tidak ada pengujian HP fisik untuk pemutaran video, mikrofon atau latensi AI pada build ini. Percobaan emulator tanpa akselerasi pada 1.2.1 mengalami startup ANR sebelum tes selesai; tidak dihitung sebagai tes perangkat lulus. AI masih pengenal/perbandingan kata yang bisa keliru, bukan penilaian tajwid atau makhraj tervalidasi dan bukan jaminan koreksi seketika.
