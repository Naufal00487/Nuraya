# Nuraya 1.2.0 untuk Android

[Unduh APK Nuraya 1.2.0](Nuraya-1.2.0.apk?raw=1) · [Kode sumber](Nuraya-1.2.0-source.zip?raw=1)

APK 44,8 MB, Android 8.0 atau lebih baru. Pasang sebagai pembaruan versi sebelumnya; application ID dan sertifikat sama. Tidak perlu menghapus aplikasi lama. Ini build pengembangan bertanda tangan, bukan rilis Play Store.

## Pembaruan

- AI bacaan Al-Qur’an: **Beranda → AI bacaan Al-Qur’an → Unduh & aktifkan AI**. Model sekitar **59,7 MB** diunduh dan diverifikasi sekali, kemudian pengenalan kata berjalan offline di HP. Pilih ayat, tekan **Mulai membaca**, izinkan mikrofon, lalu **Selesai membaca**.
- Pencarian video berdasarkan kebutuhan belajar, sinonim dan filter. Cakupannya **12 rujukan yang dikurasi**, bukan pencarian langsung seluruh YouTube.
- Tombol **Tonton di aplikasi** memakai YouTube embed resmi untuk video/Shorts/playlist yang didukung. Internet diperlukan; sumber asli tetap bisa dibuka bila penerbit membatasi embed. Iklan platform YouTube dapat tampil.
- Teman Belajar langsung mencari kutipan ayat, tafsir Kemenag lengkap dan pelajaran tajwid secara lokal. Pertanyaan tidak dikirim ke server; hasil berupa sumber, bukan jawaban generatif.

Hasil AI adalah perkiraan kecocokan kata: dapat salah mengenali, melewatkan, atau melengkapi kata. **Belum menilai tajwid/makhraj secara tervalidasi**, dan kecepatan pembaruan bergantung HP. Pengujian dengan rekaman nyata pada host lulus; mikrofon, performa AI, dan YouTube belum diuji pada HP fisik.

## Validasi

105 tes JVM, 21 tes Python, 7 pemeriksaan native JNI dan 4 verifier offline lulus. Build APK/instrumentasi dan lint lulus (21 peringatan, tanpa error). Tanda tangan dan alignment 16 KB diverifikasi. [Laporan lengkap](BUILD_VALIDATION.md), [ringkasan mesin](validation/1.2.0/summary.json), [hasil model/probe](validation/1.2.0/model-inference.json), [asal/lisensi model](QURAN_AI_MODEL.md).

[Metadata dan checksum sumber](build-info.json) · [SHA-256 berkas unduhan](SHA256SUMS)

Fitur Quran Utsmani, warna tajwid, arti kata, tafsir, qari, hafalan, kuis dan progres versi sebelumnya tetap tersedia. Nuraya gratis; penyusun tidak mengambil keuntungan finansial dan tidak memasang iklan sendiri.

Versi terdahulu tetap disimpan: [1.1.0](Nuraya-1.1.0.apk?raw=1), [1.0.1](Nuraya-1.0.1.apk?raw=1). Tautan immutable distribusi terdahulu tidak diubah.
