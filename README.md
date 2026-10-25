# Nuraya 1.1.0 — unduhan Android

**[Download Nuraya 1.1.0 APK](Nuraya-1.1.0.apk?raw=true)** · 32,7 MB
[Source ZIP](Nuraya-1.1.0-source.zip?raw=true) · [Hasil validasi](BUILD_VALIDATION.md) · [SHA-256](SHA256SUMS)

Pasang pada **Android 8.0 atau lebih baru** sebagai pembaruan versi Nuraya sebelumnya. Application ID `com.nuraya.app`, versionCode 3, sertifikat sama; tidak perlu menghapus aplikasi. APK ini edisi pengembangan yang ditandatangani dengan debug certificate, belum rilis Google Play.

## Pembaruan

- Tulisan Utsmani dengan **warna tajwid aktif secara default**, dapat dimatikan di Pengaturan. Pembetulan basmalah tetap dipertahankan.
- **138 pilihan rekaman** lengkap, termasuk beberapa edisi qari yang sama; **17 qari untuk audio per ayat**. Sorotan kata mengikuti rekaman Alafasy pada 6.142 ayat yang memiliki timing aman, dengan sorotan per ayat untuk 94 ayat lainnya. Qari lain menggunakan sorotan per ayat.
- Arti per kata Indonesia/Inggris, tafsir Kemenag Indonesia seluruh 6.236 ayat, dan panduan tajwid tersedia offline.
- Mode hafalan, pengulangan rentang, 8 materi tajwid, 10 soal kuis, target serta progres lokal.
- 12 rujukan video/Shorts belajar, pencarian terbatas materi Al-Qur’an, filter tingkat/format/topik, dan simpan untuk nanti. Tautan dibuka pada sumber aslinya.
- Latihan mikrofon melalui pengenal suara Android untuk perkiraan kecocokan kata. Tidak menyimpan audio/transkrip. Penyedia online memerlukan persetujuan di aplikasi.

**Batas fitur:** latihan mikrofon masih eksperimental; belum menilai tajwid, makhraj, harakat atau panjang mad. Ketersediaan pengenal Arab bergantung pada HP. YouTube dibatasi dari cloud sehingga tautan video baru diverifikasi melalui referensi publik; pemutaran dan isi lengkapnya belum diuji. Materi berbahasa Indonesia belum terverifikasi dalam katalog video ini. Tidak ada layanan AI berbayar yang diaktifkan.

**Validasi:** 73 test JVM dan 21 test Python lulus; build, lint, verifikasi data, tanda tangan APK serta alignment lulus. Sampel 138 pilihan rekaman, 17 qari per ayat, dua ayat fallback dan dua rekaman pasangan Alafasy lolos unduh/decode audio pada host. Belum diuji pada HP fisik; ini bukan klaim seluruh rekaman sudah didengarkan. Lihat [laporan lengkap](BUILD_VALIDATION.md).

## Sumber dan privasi

Gratis, tanpa iklan, akun wajib, langganan atau pembelian. Penyusun tidak mengambil keuntungan finansial. Data belajar/progres tersimpan lokal. Hak konten tetap milik sumber masing-masing.

[Data Quran](DATA_SOURCES.md) · [Data belajar](STUDY_DATA.md) · [Audio](AUDIO_CATALOG.md) · [Video](VIDEO_SOURCES.md) · [Latihan bacaan](RECITATION_ASSISTANT.md) · [Hafalan/kuis](PRACTICE.md)

[Tampilan host](study-previews/) · [Bukti pemeriksaan](validation/1.1.0/)

Berkas 1.0.1 dipertahankan untuk riwayat. Gunakan APK 1.1.0 di atas untuk pembaruan ini. Source ZIP memuat proyek Gradle dan bukti sumber/verifikasi; keystore pribadi tidak disertakan.
