# Build dan validasi Nuraya 1.4.0

Diperiksa 7 November 2026. Paket `com.nuraya.app`, versionCode **9**, minimum Android 8/API 26, target API 35. [Perubahan](UPDATE_1.4.0.md).

## Hasil akhir

**BUILD SUCCESSFUL. 162 tes JVM/Compose lulus, 0 gagal/error/dilewati.** Tujuh tes backend banner lulus. Lint tanpa error/fatal, dengan 37 peringatan. APK aplikasi dan APK instrumentasi berhasil dibangun. Instrumentasi belum dijalankan pada HP/emulator dalam rilis ini.

```sh
bash scripts/gradle.sh assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue
node --test backend/banner-bot/worker.test.mjs
python3 scripts/audit-quran-corpus.py
python3 scripts/audit-reading-guidance.py
python3 scripts/audit-audio-identity.py
python3 scripts/verify-native-optimization.py
```

Bukti: [ringkasan suite](validation/1.4.0/summary.json), [JUnit](validation/1.4.0/junit), [log Gradle](validation/1.4.0/gradle-build.log), [lint](validation/1.4.0/lint.txt), [tes bot](validation/1.4.0/banner-tests.log). Percobaan awal terkena masalah pemuatan font native Robolectric (`FileSystemAlreadyExistsException`), diikuti error native terkait. Pengujian terfokus lalu seluruh suite diulang dalam JVM baru dan lulus; tidak ada tes yang dihapus/dilewati. Pemeriksaan pintasan Progres kemudian disesuaikan untuk menggulir LazyColumn hingga kartu yang belum dirender, lalu tetap memeriksa tab terpilih dan alur kembali. Peringatan lint mencakup saran KTX, kapasitas disk, resource/SDK, konteks aplikasi LatinRepository dan false positive `ImplicitSamInstance` pada `Context.stopService(Intent)`: API tersebut mencocokkan komponen layanan, bukan identitas objek Intent.

## Alur yang diperiksa

- Profil nama/avatar disimpan lokal, ditampilkan kembali di beranda, dan dipakai kuis pribadi. Pintasan Progres langsung membuka tab Progres dan kembali ke profil. Tidak ada galeri, akun atau leaderboard.
- Navigasi empat menu, lokasi pertama, pintasan AI dan kompas, bacaan/Madinah/Bahriyah, serta kontrol dengan skala teks 130%.
- Urutan Al-Mulk 1–5 ×3 tepat 15 item; per-ayat menghasilkan urutan berbeda; seluruh surah mempertahankan ayat terakhir. Jumlah/rentang invalid ditolak.
- Azan awalnya nonaktif; satu alarm salat berikutnya dijadwalkan tanpa terbit/duplikasi. Menonaktifkan atau mencabut izin membatalkan alarm; lokasi kosong/alarm kedaluwarsa tidak memulai layanan. Rekaman MP3 dibaca penuh oleh ffmpeg tanpa error (204,648 detik).
- Quran, basmalah, Latin, tajwid, tafsir, riwayat, audio, izin dan sesi mikrofon/AI tetap menjalani regresi. Teks Quran dan katalog ID audio tidak berubah dari versi sebelumnya.
- 134 foto: hash/ukuran/dekode serta pemetaan 140 ID tanpa duplikasi diperiksa. Foto tambahan mengikuti label dokumen pengguna, bukan identifikasi wajah independen.
- Bot menolak secret salah, non-admin dan grup; memeriksa payload/tujuan, menolak pengiriman ulang foto, serta menguji unggah/edit/aktif/nonaktif/urutan/hapus memakai layanan tiruan. Wrangler **dry-run** berhasil; tidak ada deploy atau pesan Telegram nyata yang dikirim. [Bukti bundling](validation/1.4.0/worker-dry-run.log).

Cuplikan hasil render Compose/Robolectric yang diperiksa: [beranda](validation/1.4.0/home-with-location.png), [profil](validation/1.4.0/profile.png), [kompas tanpa sensor](validation/1.4.0/compass.png), [qari](validation/1.4.0/qari-portraits.png), [rentang hafalan/teks besar](validation/1.4.0/memorization-range-large-text.png), [kuis pribadi](validation/1.4.0/quiz-personal.png). Ini render host, bukan tangkapan layar HP fisik.

## Audit Quran dan sumber

Seluruh 6.236 ayat Arab, terjemahan Indonesia/Inggris dan catatan Indonesia cocok persis dengan snapshot sumber yang dipatok. Tidak ada ID ayat ganda. Latin dan tafsir mencakup 6.236 ayat. Warna tajwid/waqaf mempertahankan pengecualian pemetaan yang sudah dicatat; tidak diisi dengan tebakan. [Audit corpus](audit/1.4.0/quran-corpus.json), [bantuan baca](audit/1.4.0/reading-guidance.json), [audio/foto](audit/1.4.0/audio-identity.json).

Verifikasi audio memakai metadata dan bukti unduhan sebelumnya: 138 Al-Fatihah lengkap pada 4 November, 22 sampel ayat/timing/fallback pada 5 November. Tidak diklaim seluruh rekaman diunduh atau suara masing-masing qari diautentikasi ulang. Teks kanonis SHA-256 tetap `f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c`.

[Sumber desain](sources/brand-assets.json), [foto dokumen](sources/qari-user-images.json), dan [audio azan](sources/adhan-recording.json) mencatat asal serta hash. Lima entri foto ditahan karena placeholder/label bertentangan. Biografi pribadi lengkap semua qari belum terverifikasi; aplikasi menampilkan keterangan edisi rekaman dari penyedia.

## Integritas APK

`apksigner verify --verbose --print-certs` dan `zipalign -c -v -P 16 4` lulus. Sertifikat sama dengan distribusi sebelumnya: `e2aa60317632bc6aea65f83c2247e54111941d0e0725875a2c9db3ffb93ac264`. [Tanda tangan](validation/1.4.0/apk-signature.txt), [alignment](validation/1.4.0/apk-alignment.txt), [metadata](validation/1.4.0/apk-metadata.txt).

Guard native memverifikasi optimasi runtime pada arm64-v8a, armeabi-v7a dan x86_64: [bukti](validation/1.4.0/native-compile-optimization.json). Pengemasan memeriksa kesamaan semua aset sumber dengan APK dan tidak menyertakan cache, keystore, token, node_modules atau model AI dalam ZIP source.

## Batas perangkat dan layanan

GPS, arah kompas, mikrofon dan pemutaran azan fisik belum diuji. Jadwal adalah perhitungan astronomis berdasarkan lokasi/zona waktu dan koreksi pengguna. AI tetap perkiraan kata yang dapat keliru, bukan penilai tajwid/makhraj tervalidasi. Bahriyah memakai PDF pengguna; halaman Madinah asli tidak diwarnai ulang. Banner remote belum diaktifkan sesuai pilihan pengguna; banner bawaan tetap berjalan. Token Telegram yang pernah dibagikan perlu diganti sebelum aktivasi layanan.
