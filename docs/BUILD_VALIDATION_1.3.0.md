# Build dan validasi Nuraya 1.3.0

Diperiksa 29 Oktober 2026. Paket `com.nuraya.app`, versionCode **7**, minimum Android 8/API 26, target API 35. Laporan sebelumnya: [1.2.2](BUILD_VALIDATION_1.2.2.md). Perubahan lengkap: [UPDATE_1.3.0.md](UPDATE_1.3.0.md).

## Hasil

**BUILD SUCCESSFUL. 133 tes JVM/Compose lulus, 0 gagal/error/dilewati.** Lint: 28 peringatan, tanpa error/fatal. APK aplikasi dan APK instrumentasi berhasil dibangun. Tes perangkat tidak dijalankan.

```sh
bash scripts/gradle.sh assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue
python3 scripts/import_latin.py --verify
python3 scripts/verify-native-optimization.py
```

Bukti: [ringkasan dan daftar suite](validation/1.3.0/summary.json), [log Gradle](validation/1.3.0/gradle-build.log), [lint](validation/1.3.0/lint.txt).

## Perilaku yang diperiksa

- Empat navigasi utama, tidak ada menu/pemutar/katalog video. Entri video lama disembunyikan tanpa menghapus penanda atau data belajar lain. AI tetap dapat dijangkau saat teks sistem diperbesar.
- Pemilihan mode, rujukan tematik, Latin offline 114 surah/6.236 ayat, hafalan delapan kelompok, sembunyikan/tampilkan, pengulangan Tikrar dan penghitung mandiri. Regresi perpindahan Al-Baqarah 2:255 ke Al-Fatihah tidak memulihkan rentang surah yang salah. Memulai ulang Tikrar pada ayat yang sama memakai jumlah pengulangan terbaru.
- Waqaf bersumber: 4.362 tanda pada 2.639 ayat; 6.234 ayat dapat diselaraskan. Rujukan 12:39 dan 12:41 tidak diberi anotasi tambahan. Small-high-seen dalam 2:245 dan 7:69 tidak salah dianggap saktah. Huruf, nomor ayat dan kebijakan basmalah kanonis tidak berubah.
- Gambar Madinah, navigasi halaman, pembesaran, teks pendamping, dan tombol yang tetap dapat dijangkau pada skala font 1,6. Semua 6.236 ayat mempunyai rujukan ke 604 halaman. Hash gambar asli serta penolakan data rusak diuji. Halaman 1, 2, 3 dan 604 disertakan; lainnya diunduh saat dibuka dengan cache 64 MiB.
- Seluruh **604 URL** diunduh penuh dan hash Git blob cocok: [bukti sumber gambar](validation/1.3.0/mushaf-all-pages-check.json). PNG halaman 3 memiliki latar rusak; versi JPG asli yang bersih dari penerbit yang sama menggantikannya, tanpa menggambar ulang teks. [Asal gambar](MUSHAF_IMAGES.md).
- Bahriyah: impor PDF lokal, penyimpanan URI/halaman, batas nomor halaman, keadaan awal dan pemulihan berkas tidak tersedia. Ini bukan mushaf Bahriyah bawaan; tata letak mengikuti PDF pengguna.
- Delapan foto qari asli, 14 ID rekaman yang cocok persis, SHA-256 gambar, identitas dan tombol pemilihan. Nama mirip tidak diberi wajah orang lain. [Sumber foto](QARI_PORTRAITS.md).
- Jadwal salat offline, pilihan kota/koordinat/zona waktu, pengaturan Hijriah dan koreksi menit, perpindahan hari, pengecualian terbit dari hitung mundur, serta rumus kiblat. Sensor yang tidak tersedia mempunyai penjelasan. [Metode](PRAYER.md).
- Tes lama untuk Quran, tafsir/tajwid, audio/sinkronisasi, AI lokal, izin/mikrofon, latihan dan penyimpanan tetap dijalankan dalam suite final. Ini bukan pengujian suara atau sensor HP fisik.

Cuplikan dari Compose/Robolectric: [beranda](validation/1.3.0/senior-home.png), [mushaf](validation/1.3.0/madinah-page.png), [teks besar](validation/1.3.0/madinah-large-text.png), [qari](validation/1.3.0/qari-portraits.png), [jadwal](validation/1.3.0/prayer-schedule.png), [hafalan](validation/1.3.0/hafalan-eight-blocks.png), [Bahriyah](validation/1.3.0/bahriyah-import.png).

## Integritas paket

`apksigner verify --verbose --print-certs` dan `zipalign -c -P 16 4` lulus. Sertifikat tetap `e2aa60317632bc6aea65f83c2247e54111941d0e0725875a2c9db3ffb93ac264`, sehingga APK dapat memperbarui instalasi lama. [Tanda tangan](validation/1.3.0/apk-signature.txt), [metadata](validation/1.3.0/apk-metadata.txt).

Guard native memeriksa kompilasi teroptimasi pada arm64-v8a, armeabi-v7a dan x86_64: [bukti](validation/1.3.0/native-compile-optimization.json). SHA-256 `quran.json` tetap `f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c`. Snapshot Latin dan waqaf serta manifest sumber disertakan dalam `docs/sources`.

`scripts/package-artifacts.py` memeriksa umur APK terhadap input build, kesamaan semua aset dengan sumber, optimasi native dan integritas ZIP. Keystore, cache, artefak build dan model AI tidak masuk ZIP sumber. Metadata distribusi memuat ukuran dan SHA-256; berkas publik diperiksa setelah unggah.

## Batas yang perlu diketahui

**PDF Mushaf Bahriyah belum dibundel** karena sumber edisi belum terverifikasi. Pengguna dapat membuka PDF miliknya. Hafalan delapan blok dan Tikrar adalah bantuan latihan, bukan salinan tata letak edisi cetak berlisensi tertentu. Latin tidak diwarnai dengan pemetaan dugaan; warna tajwid diterapkan pada Arab. Madinah/tajwid menyediakan halaman asli serta tombol ke teks ayat berwarna, bukan gambar pindai tajwid berwarna. Foto asli tersedia untuk delapan qari; lainnya memakai inisial.

Tidak ada verifikasi mikrofon, sensor kompas, pemutaran audio atau rendering PDF pada HP fisik untuk versi ini. Pemilihan PDF melalui penyedia dokumen Android dan izin persisten masih perlu dicoba pada perangkat pengguna. Jadwal adalah perhitungan astronomis dengan kota yang dipilih, bukan unduhan jadwal resmi Kemenag; kalender Hijriah dapat dikoreksi pengguna. AI masih memperkirakan dan membandingkan kata, bisa keliru, serta bukan penilai tajwid/makhraj tervalidasi. Tidak ada klaim jaminan koreksi seketika.
