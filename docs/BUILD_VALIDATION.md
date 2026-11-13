# Build dan validasi Nuraya 1.5.3

Diperiksa 13 November 2026 (Asia/Jakarta). Paket `com.nuraya.app`, versionCode **13**, minimum Android 8/API 26, target API 35. [Perubahan](UPDATE_1.5.3.md).

## Hasil

**BUILD SUCCESSFUL. 189 tes JVM/Compose lulus, 0 gagal/error/dilewati.** Lint: 0 error/fatal, 41 peringatan. APK aplikasi dan APK instrumentasi berhasil dibangun. Instrumentasi belum dijalankan pada HP atau emulator.

```sh
bash scripts/gradle.sh assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue
python3 scripts/verify-native-optimization.py
```

Bukti: [ringkasan](validation/1.5.3/summary.json), [JUnit](validation/1.5.3/junit), [build](validation/1.5.3/gradle-build.log), [lint](validation/1.5.3/lint.txt).

## Regresi yang diperiksa

| Bagian | Bukti pengujian |
| --- | --- |
| Seluruh surah | Pembaca digital benar-benar dirender untuk 114 surah, masing-masing menampilkan teks kanonis ayat pertamanya, dengan posisi audio lama 2:6 tersimpan. |
| Gambar mushaf | Pemetaan kedua edisi diperiksa terhadap seluruh 6.236 ayat. Al-Kahf, Yasin, Al-Mulk, Al-Ikhlas dan An-Nas membuka nomor halaman dan ayat pendamping yang diminta meskipun posisi audio lama 2:6 masih ada. Tes ini tidak mengunduh ulang seluruh 604 gambar. |
| Mengikuti audio | Posisi jeda tidak mengubah pilihan surah. Audio surah lain ditolak oleh pengikut halaman; perubahan ayat aktif pada surah yang sama tetap diikuti jika pengaturannya aktif. |
| Mode membaca | Semua pilihan menu dibuka. Mode gambar tetap layar penuh; mode belajar menampilkan fungsi khusus. Arti kata, Latin, warna sumber, blok hafalan, Tikrar, panduan waqaf dan rujukan tema/tafsir diperiksa. |
| Hafalan & Tikrar | Memilih blok kedua Al-Mulk menampilkan ayat blok itu dan mengirim rentang audio yang sama. Sembunyikan/tampilkan tidak mengganti teks. Penghitung Tikrar disimpan per ayat dan tidak bertambah karena audio. |
| Profil wajib | Nama kosong/spasi tidak diterima, tombol Kembali tidak melewati pengisian, dan nama yang disimpan membuka beranda serta tersimpan lokal. Profil lama berisi nama tetap dipakai. |
| Ibadah/Pengaturan | Batas geometri judul, gambar, kalender dan pilihan diperiksa agar tidak bertumpuk, pada skala font 1,5×/1,6×. Kota dapat dicari/dipilih; hitung mundur dan kiblat tetap dapat diakses. |
| Qari | Lima pilihan surah lengkap dan tujuh pilihan per ayat mempunyai pemetaan foto Quran.com yang eksplisit, tanpa dua pilihan untuk orang yang sama dalam satu daftar. Nama tampilan tidak mengubah ID atau URL rekaman. |
| Fitur lain | Tes AI, mikrofon simulasi, audio, lokasi, salat, profil, kuis, penyimpanan dan navigasi yang sudah ada ikut lulus. Ini bukan pengujian hardware atau autentikasi suara oleh ahli. |

[Per kata](validation/1.5.3/mode-word_tajwid.png), [Latin](validation/1.5.3/mode-latin_tajwid.png), [hafalan](validation/1.5.3/mode-hafalan_8.png), [Tikrar](validation/1.5.3/mode-tikrar.png), [profil wajib](validation/1.5.3/profile-required.png), [Ibadah tulisan besar](validation/1.5.3/prayer-large-text.png), [Pengaturan tulisan besar](validation/1.5.3/settings-large-text.png), dan [sakelar Pengaturan](validation/1.5.3/settings-toggles-large-text.png) diperiksa secara visual. Gambar ini render Compose/Robolectric di host, bukan tangkapan HP fisik.

## Data dan sumber

SHA-256 `quran.json` tetap `f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c`. Teks Arab, nomor ayat, basmalah, terjemahan, Latin, waqaf, anotasi tajwid, gambar/indeks halaman dan URL/timing rekaman tidak diubah. Pengemasan membandingkan setiap asset APK dengan source secara byte.

126 foto dari dokumen penyusun dan sampul Bahriyah dihapus dari APK. Delapan foto beratribusi Quran.com cocok dengan hash sumber. Menu dan penampil PDF Bahriyah dihapus. [Audit data/kurasi rilis](audit/1.5.3/release-data.json).

Audit gambar sebelumnya tetap relevan terhadap berkas yang sama: [604 gambar dan checksum](audit/1.5.1/mushaf-indonesia-files.json), [batas halaman](audit/1.5.1/mushaf-indonesia.json), [corpus](audit/1.5.0/quran-corpus.json). Tidak ada klaim setiap huruf/harakat telah ditashih ulang oleh ahli. Madinah & tajwid adalah tata letak teks digital menurut batas halaman, bukan pindai edisi cetak tajwid. [Sumber/batas mode](UPDATE_1.5.3.md).

## APK

Ukuran **63.771.254 byte**. SHA-256: `a46713876ff263fc9a0efa65b1dcc1493e998077fa460f105862f6eac4278e1a`.

`apksigner verify --verbose --print-certs` dan `zipalign -c -v -P 16 4` lulus. Sertifikat tetap `e2aa60317632bc6aea65f83c2247e54111941d0e0725875a2c9db3ffb93ac264`. Optimasi native tiga ABI lulus. [Tanda tangan](validation/1.5.3/apk-signature.txt), [alignment](validation/1.5.3/apk-alignment.txt), [metadata](validation/1.5.3/apk-metadata.txt), [optimasi native](validation/1.5.3/native-optimization.json).

HP fisik, GPS, kompas, suara azan dan streaming audio tidak diuji pada perangkat dalam pembaruan ini. AI tetap perkiraan kata lokal, bukan penilaian tajwid/makhraj tersertifikasi. Backend banner tidak berubah, tidak di-deploy, dan tidak ada pesan Telegram yang dikirim.
