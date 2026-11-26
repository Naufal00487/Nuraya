# Nuraya iOS — uji iPhone XR

[**Unduh Nuraya iOS (.IPA, 100,3 MB)**](https://github.com/Naufal00487/Nuraya/raw/refs/heads/downloads/nuraya-ios-preview/Nuraya-iOS-XR-unsigned.ipa)

**Build 3 — 26 November 2026.** Memperbaiki Murottal yang berhenti saat kembali ke Home. Audio yang dijeda tetap diam; audio pembaca Quran tetap berhenti saat meninggalkan pembaca. Perbaikan perpindahan tab, header, splash dua maskot, Koleksi Saya, dan lanjut baca dari build 2 tetap tersedia.

Minimum **iOS 16.0**. Paket ini belum ditandatangani untuk akun Apple pengguna. Di Ubuntu, buka **iloader → Import IPA** dan pilih file ini. Di Windows, gunakan **AltStore Classic → My Apps → +** pada iPhone setelah menyiapkan AltServer. Akun Apple gratis biasanya perlu refresh sebelum tujuh hari.

- [Panduan pemasangan dan uji fitur](https://github.com/Naufal00487/Nuraya/blob/feature/nuraya-ios-xr/docs/ios/UJI_IPHONE_XR.md).
- [Petunjuk singkat dalam berkas teks](Pasang-Nuraya-di-iPhone-XR.txt).
- [Laporan dan screenshot pengujian](https://github.com/Naufal00487/Nuraya/blob/feature/nuraya-ios-xr/docs/ios/STATUS.md).
- [30 pengujian yang lulus, termasuk pemutaran Murottal selama Home](https://github.com/Naufal00487/Nuraya/actions/runs/45427575713).
- [Kode sumber iOS](https://github.com/Naufal00487/Nuraya/tree/feature/nuraya-ios-xr/ios).
- [Android tetap tersedia](https://github.com/Naufal00487/Nuraya).

Untuk memperbarui aplikasi yang sudah dipasang, gunakan Apple ID yang sama di iloader/AltStore dan pilih IPA baru tanpa menghapus Nuraya. Periksa tulisan **iOS build 3** di bagian bawah Pengaturan.

Sumber aplikasi yang dibangun: `50be7ab9f38d3e2b6382a3dd5eace5b67424580d`, versi 1.7.1 (build 3), arsitektur ARM64. Seluruh 1.740 aset Android di bundle telah diperiksa dan tidak berubah. Checksum tersedia di [SHA256SUMS.txt](SHA256SUMS.txt); rincian mesin di [VALIDATION.json](VALIDATION.json).

Ini versi uji pribadi dan belum diterbitkan ke App Store. Mikrofon, kompas, performa AI pada A12, audio layar terkunci, dan notifikasi masih memerlukan pengujian pada iPhone fisik. Antarmuka belum diklaim identik piksel demi piksel dengan Android; sebagian terjemahan English belum lengkap. AI membandingkan perkiraan kata, bukan mengesahkan tajwid/makhraj. Suara notifikasi azan memakai cuplikan 28 detik mengikuti batas iOS.

Cabang ini berisi paket unduhan iOS. Sumber dan paket Android tidak dihapus atau ditimpa.
