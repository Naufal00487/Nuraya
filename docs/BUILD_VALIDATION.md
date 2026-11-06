# Build dan validasi Nuraya 1.3.1

Diperiksa 5 November 2026. Paket `com.nuraya.app`, versionCode **8**, minimum Android 8/API 26, target API 35. [Perubahan](UPDATE_1.3.1.md), [audit](AUDIT_1.3.1.md).

## Hasil akhir

**BUILD SUCCESSFUL. 152 tes JVM/Compose lulus, 0 gagal/error/dilewati.** Lint menghasilkan 31 peringatan, tanpa error/fatal. APK aplikasi dan APK instrumentasi berhasil dibangun. APK instrumentasi tidak dijalankan pada perangkat.

```sh
bash scripts/gradle.sh assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue
python3 scripts/audit-quran-corpus.py
python3 scripts/audit-reading-guidance.py
python3 scripts/audit-audio-identity.py
python3 scripts/verify_audio_catalog.py
python3 scripts/verify-native-optimization.py
```

Bukti: [ringkasan semua suite](validation/1.3.1/summary.json), [XML JUnit](validation/1.3.1/junit), [log build final](validation/1.3.1/gradle-build.log), [lint](validation/1.3.1/lint.txt). Satu percobaan sebelumnya mengalami timeout saat memeriksa tampilan belajar yang dimuat asinkron. Tes disinkronkan dengan munculnya kontrol data belajar, tetap memeriksa teks Arab dan jumlah rentang warna persis; seluruh suite kemudian lulus.

## Perilaku yang diperiksa

- Pembukaan pertama menampilkan penjelasan lokasi, meminta izin fine/coarse melalui Android, dan tetap menyediakan akses membaca bila ditunda. Koordinat belum diisi sebelum ada hasil lokasi.
- Izin ditolak, GPS mati, pilihan manual, cache lama/tidak valid, hanya izin perkiraan, posisi valid berikutnya setelah posisi buruk, timeout, pembatalan dan penghapusan listener. Penyimpanan lokasi mempertahankan koreksi Hijriah/menit salat. Lokasi manual lama tidak ditimpa otomatis.
- Qari tidak dikenal ditolak tanpa memilih Alafasy; persiapan audio sebelumnya dibatalkan. Alias Alafasy hanya satu kartu pada daftar, sementara kedua ID tetap dapat dipakai oleh data lama dan ragam rekaman lain tetap terpisah.
- Riwayat per ayat menyimpan dan membuka qari/surah/ayat yang tepat, menolak kunci rusak atau bertentangan, memperbarui satu entri saat disimpan 20 kali, bertahan setelah database dibuka ulang, dan tidak menghapus penanda/favorit ketika riwayat dibersihkan.
- Suite regresi Quran, basmalah, tajwid/tafsir, Latin, hafalan/Tikrar, Madinah, Bahriyah lokal, foto qari, jadwal/kiblat, koleksi, AI lokal, izin dan sesi mikrofon tetap dijalankan. Tes host ini tidak membuktikan hasil mikrofon atau sensor HP nyata.

Cuplikan Compose/Robolectric: [dialog lokasi pertama](validation/1.3.1/first-launch-location.png), [beranda](validation/1.3.1/senior-home.png), [jadwal](validation/1.3.1/prayer-schedule.png), [qari](validation/1.3.1/qari-portraits.png), [tajwid/per kata](validation/1.3.1/reader-tajwid-word-meanings.png), [AI](validation/1.3.1/ai-recitation-first-use.png).

## Audit data dan audio

6.236 ayat Arab, terjemahan Indonesia/Inggris dan catatan Indonesia cocok persis sumber yang dipatok; tidak ada ID atau nomor ayat global ganda. Latin dan tafsir mencakup 6.236 ayat. Pengecualian ejaan lintas sumber, kata tanpa pemetaan dan rentang warna yang sengaja tidak ditebak dijelaskan dalam [laporan audit](AUDIT_1.3.1.md).

138 sampel Al-Fatihah lengkap berhasil diunduh/dekode pada 4 November; 22 sampel audio ayat/timing/fallback lulus pada 5 November. Manifest seluruh ID dan nama berkas serta checksum foto diverifikasi. Ini bukan pemeriksaan setiap rekaman atau autentikasi seluruh suara oleh manusia. [Bukti unduhan 604 gambar](validation/reference/madinah/mushaf-all-pages-check.json) berasal dari pemeriksaan menjelang rilis 29 Oktober 2026; tidak diklaim diunduh ulang semuanya dalam rilis ini.

## Integritas paket

`apksigner verify --verbose --print-certs` dan `zipalign -c -v -P 16 4` lulus. Sertifikat tetap `e2aa60317632bc6aea65f83c2247e54111941d0e0725875a2c9db3ffb93ac264`, sehingga paket mendukung pembaruan dari versi distribusi sebelumnya. [Tanda tangan](validation/1.3.1/apk-signature.txt), [alignment](validation/1.3.1/apk-alignment.txt), [metadata](validation/1.3.1/apk-metadata.txt).

Guard native memeriksa 75 unit kompilasi teroptimasi pada arm64-v8a, armeabi-v7a dan x86_64: [bukti](validation/1.3.1/native-compile-optimization.json). Teks kanonis tetap ber-SHA-256 `f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c`.

`scripts/package-artifacts.py` memeriksa umur APK terhadap input build, kesamaan semua aset dengan sumber, optimasi native dan integritas ZIP. Keystore, cache build dan bobot model tidak masuk ZIP sumber. Ukuran dan SHA-256 APK/ZIP dicatat dalam metadata distribusi dan diperiksa kembali setelah unggah.

## Batas validasi

Izin dan lifecycle lokasi diuji pada host; belum dilakukan perjalanan/GPS/kompas/mikrofon pada HP fisik. Jadwal adalah perhitungan astronomis dari lokasi dan zona waktu, bukan unduhan jadwal resmi Kemenag. AI masih memperkirakan kata, dapat keliru, dan belum menjadi penilai tajwid/makhraj tervalidasi.

Bahriyah masih memakai PDF pengguna; edisi bawaan belum tersedia. Latin tidak diberi warna dugaan, gambar Madinah asli tetap tanpa pewarnaan ulang, dan foto hanya tersedia untuk delapan qari. Pembaruan ini tidak mengubah batas fitur tersebut.
