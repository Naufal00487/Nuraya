# Build dan validasi Nuraya 1.2.1

Diperiksa 27 Oktober 2026 (Asia/Jakarta). APK pengembangan `com.nuraya.app`, versionCode **5**, minimum Android 8/API 26, target API 35. Perbaikan menindaklanjuti laporan pengguna 1.2.0: video/Shorts kosong dan layar AI terlihat merekam tanpa hasil. Laporan terdahulu dipertahankan dalam [1.2.0](BUILD_VALIDATION_1.2.0.md).

## Perubahan yang diperiksa

- **AI native**: kernel Android 1.2.0 ternyata dikompilasi tanpa optimasi, sementara pengujian host sebelumnya memakai Release. Build APK sekarang mengoptimalkan seluruh kernel/JNI dengan `-O3`, tanpa fast-math atau instruksi CPU di luar ABI dasar. [Perintah compiler aktual](validation/1.2.1/native-compile-optimization.json) menunjukkan 25 unit kompilasi teroptimasi pada masing-masing arm64-v8a, armeabi-v7a dan x86_64. Guard baru memeriksanya sebelum pengemasan.
- **Mikrofon**: indikator aktif mengikuti keberhasilan AudioRecord, bukan hanya awal permintaan. Meter suara, hitungan sampel/waktu, tes mikrofon lima detik tanpa model, penanganan input dibisukan/tanpa frame, konversi 44,1/48 kHz, pembatalan, dan timeout inferensi ditambahkan. Sesi tidak menyimpan rekaman/transkrip. Input hening dibedakan dari perangkat yang tidak mengirim data.
- **Pemutar video**: iframe mempunyai ukuran viewport eksplisit; playlist/video di-cue secara eksplisit setelah API tersambung. `onReady` tidak langsung dianggap video siap. Tombol Putar/Muat ulang dan status tertahan/error tampil dalam aplikasi. Pembatasan publisher tidak dilewati.
- **Shorts**: feed vertikal, pencarian/filter, tombol berikutnya/sebelumnya, bookmark dan pengembalian posisi. Hanya halaman aktif mempunyai pemutar; meninggalkan halaman melepasnya. Pemutaran otomatis dicoba setelah pengguna membuka Shorts; jika ditolak, Putar tetap tersedia. Video yang tidak tersedia dapat dilewati di dalam aplikasi.

## Hasil pengujian

```sh
bash scripts/gradle.sh assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue
python3 scripts/verify-native-optimization.py
python3 scripts/test-youtube-wrapper.py
```

**BUILD SUCCESSFUL. 115 tes JVM lulus; 0 gagal, error atau dilewati.** Lint: 21 peringatan, tanpa error/fatal. APK instrumentasi berhasil dibangun. Bukti: [ringkasan](validation/1.2.1/summary.json), [log Gradle](validation/1.2.1/gradle-build.log), [lint](validation/1.2.1/lint.txt).

Tes baru memeriksa sesi perekaman memakai PCM terkontrol: suara terdeteksi tanpa model, perangkat nol-frame, input dibisukan, hening, hasil akhir, proses lambat/timeout, pembatalan dan penghapusan sampel. Resampling diperiksa pada 16/44,1/48 kHz dan batas antarpotongan. Tes UI memeriksa akses Cek mikrofon sebelum model tersedia, navigasi/geser Shorts, satu pemutar aktif, filter kosong dan pengembalian posisi. Pemutar pada tes feed host adalah komponen terkontrol; tes ini tidak membuktikan interaksi sentuh WebView asli.

Tiga skenario HTML aplikasi (**video, playlist, Shorts**) berhasil dijalankan dalam Chromium 154 melalui Playwright dengan IFrame API lokal terkontrol, masing-masing 14 pemeriksaan: ukuran iframe 480×900 sesuai viewport, cue yang benar, pembedaan API siap/media siap, intent autoplay, putar, jeda saat nonaktif, penolakan autoplay/error publisher. [Bukti browser](validation/1.2.1/browser-wrapper.json). Percobaan awal CLI dump-DOM timeout; pengujian berhasil setelah memakai kontrol browser Playwright. **Ini bukan pemutaran YouTube nyata.**

## Bukti performa model

[Probe pembanding](validation/1.2.1/native-optimization-benchmark.json) memakai host, model Q5_1, JNI, empat thread dan klip basmalah 6,025 detik yang sama. Build tanpa optimasi membutuhkan **77,058 detik**, build teroptimasi **15,665 detik** (sekitar 4,92× lebih cepat pada probe ini); keduanya menghasilkan empat kata basmalah yang sama. Ini bukan benchmark HP atau jaminan koreksi real-time. Model publik/ukuran/checksum tidak berubah dari 1.2.0. Bukti model dan tujuh pemeriksaan native sebelumnya tetap tersedia dalam [laporan 1.2.0](BUILD_VALIDATION_1.2.0.md); tidak diklaim dijalankan ulang untuk versi ini.

AI tetap pengenal kata yang bisa keliru, bukan penilai tajwid, makhraj, harakat atau mad. Hasil kosong/timeout tidak dianggap bacaan benar. Kecepatan dan memori bergantung perangkat.

## Percobaan Android dan batas verifikasi

API35 x86_64 software emulator tanpa KVM berhasil boot; APK 1.2.1 dan APK tes berhasil dipasang sebagai pembaruan 1.2.0. Namun proses instrumentasi dihentikan Android karena **startup ANR**, sebelum test selesai. Hasilnya **0 test perangkat selesai/lulus**, bukan pass berdasarkan exit-code adb. [Log instrumentasi](validation/1.2.1/android-media-runtime.log), [diagnosis startup](validation/1.2.1/android-startup-diagnosis.log). Percobaan membuka 1.2.0 pada emulator yang sama juga mengalami System UI/startup ANR.

Test Android untuk WebView nyata (layanan IFrame terkontrol) dan pemuatan model/JNI disertakan untuk perangkat/emulator berakselerasi; belum dinyatakan lulus. Pengembang belum memverifikasi mikrofon fisik, latensi AI, streaming YouTube, swipe tepat di atas WebView, fullscreen atau perilaku vendor Samsung. Tombol native berikutnya/sebelumnya tetap tersedia jika WebView mengambil gestur. YouTube dari cloud tetap dibatasi HTTP 403 meskipun pemeriksaan diulang dengan izin tambahan; ketersediaan dan izin embed tiap publisher belum terkonfirmasi.

## Integritas dan distribusi

APK lolos `apksigner verify --verbose --print-certs` serta `zipalign -c -P 16 4`. Sertifikat tetap `e2aa60317632bc6aea65f83c2247e54111941d0e0725875a2c9db3ffb93ac264` agar dapat dipasang tanpa menghapus data versi terdahulu. [Bukti tanda tangan](validation/1.2.1/apk-signature.txt).

Data Quran, ayat, tafsir, tajwid, qari, katalog 12 video dan model tidak diubah. Pemeriksaan data/Python pada [1.2.0](BUILD_VALIDATION_1.2.0.md) tetap menjadi bukti versi data tersebut; tes aplikasi penuh di atas memeriksa regresi integrasinya. Video tidak diekstrak atau diunduh dari YouTube. Nuraya tetap tidak memasang iklan sendiri; platform YouTube dapat menampilkan iklan.

Paket dibuat dengan `scripts/package-artifacts.py`: APK harus terkini, seluruh asset sama persis dengan sumber, native teroptimasi, ZIP utuh, keystore/cache tidak disertakan. Hash/ukuran tercantum di metadata distribusi; unduhan publik diperiksa penuh setelah unggah. Publikasi dilakukan di cabang unduhan baru tanpa mengganti tautan versi terdahulu.
