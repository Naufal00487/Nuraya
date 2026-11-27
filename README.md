# Nuraya iOS — uji iPhone XR

[**Unduh Nuraya iOS (.IPA, 100,3 MB)**](https://github.com/Naufal00487/Nuraya/raw/refs/heads/downloads/nuraya-ios-preview/Nuraya-iOS-XR-unsigned.ipa)

**Build 4 — 27 November 2026.** Memperbaiki jeda tambahan pada audio pembaca Quran: ayat berurutan dari rekaman surah yang sama diputar tanpa memuat ulang tiap ayat, sementara berkas per ayat berikutnya disiapkan terlebih dahulu. Jeda napas/waqaf asli dipertahankan. Perbaikan Murottal saat Home, perpindahan tab, header, splash dan Koleksi Saya tetap tersedia.

Minimum **iOS 16.0**. Paket ini belum ditandatangani untuk akun Apple pengguna. Di Ubuntu, buka **iloader → Import IPA** dan pilih file ini. Di Windows, gunakan **AltStore Classic → My Apps → +** pada iPhone setelah menyiapkan AltServer. Akun Apple gratis biasanya perlu refresh sebelum tujuh hari.

- [Panduan pemasangan dan uji fitur](https://github.com/Naufal00487/Nuraya/blob/feature/nuraya-ios-xr/docs/ios/UJI_IPHONE_XR.md).
- [Petunjuk singkat dalam berkas teks](Pasang-Nuraya-di-iPhone-XR.txt).
- [Laporan dan screenshot pengujian](https://github.com/Naufal00487/Nuraya/blob/feature/nuraya-ios-xr/docs/ios/STATUS.md).
- [35 pengujian yang lulus, termasuk kesinambungan bacaan dan Murottal selama Home](https://github.com/Naufal00487/Nuraya/actions/runs/45947277501).
- [Kode sumber iOS](https://github.com/Naufal00487/Nuraya/tree/feature/nuraya-ios-xr/ios).
- [Android tetap tersedia](https://github.com/Naufal00487/Nuraya).

Untuk memperbarui aplikasi yang sudah dipasang, gunakan Apple ID yang sama di iloader/AltStore dan pilih IPA baru tanpa menghapus Nuraya. Periksa tulisan **iOS build 4** di bagian bawah Pengaturan.

Sumber aplikasi yang dibangun: `745a3752443a2ca9b8eb9000ce481818d5707d1a`, versi 1.7.1 (build 4), arsitektur ARM64. Seluruh 1.740 aset Android di bundle telah diperiksa dan tidak berubah. Checksum tersedia di [SHA256SUMS.txt](SHA256SUMS.txt); rincian mesin di [VALIDATION.json](VALIDATION.json).

Timing yang tidak layak pada 23 pasangan qari/surah memakai sumber per ayat dari qari yang sama tanpa sorotan kata. Rinciannya ada dalam laporan pengujian. Buffering jaringan atau keheningan dalam berkas asli masih dapat terdengar.

Ini versi uji pribadi dan belum diterbitkan ke App Store. Mikrofon, kompas, performa AI pada A12, audio layar terkunci, dan notifikasi masih memerlukan pengujian pada iPhone fisik. Antarmuka belum diklaim identik piksel demi piksel dengan Android; sebagian terjemahan English belum lengkap. AI membandingkan perkiraan kata, bukan mengesahkan tajwid/makhraj. Suara notifikasi azan memakai cuplikan 28 detik mengikuti batas iOS.

Cabang ini berisi paket unduhan iOS. Sumber dan paket Android tidak dihapus atau ditimpa.
