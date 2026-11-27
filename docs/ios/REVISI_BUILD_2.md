# Revisi iOS 1.7.1 build 2

Revisi ini menangani laporan pengguna pada 23 November 2026. Rujukan perilaku dan gambar tetap aplikasi Android 1.7.1. Android tidak diubah.

| Laporan | Penyebab yang ditemukan | Perbaikan |
| --- | --- | --- |
| Pilihan Quran per ayat menutupi Murottal setelah tab dipindah | Layar surah memakai tautan navigasi langsung yang tidak tercatat dalam `path`. Mengosongkan `path` dan mengganti isi root tidak membuang layar tersebut. | Pilihan mode sekarang memakai rute yang tercatat. Pergantian tab juga membuat ulang tumpukan navigasi agar halaman dari tab sebelumnya tidak tertinggal. |
| Gambar atas Quran dan Murottal terlalu kecil | Gambar memakai `aspectFit` di dalam tinggi tetap 155/160 pt sehingga lebarnya mengecil. | Header mengisi lebar konten, tinggi minimum 190 pt, potongan gambar dan keterangan biru mengikuti `SectionArtwork` Android. Sumber gambar tidak diganti. |
| Splash dua maskot tidak muncul | Layar pemuatan hanya menampilkan logo; konfigurasi launch screen kosong. | Launch storyboard dan layar pemuatan menggunakan `brand/splash.jpg` asli. Saat proses pertama dibuka, gambar tampil sekurangnya 700 ms seperti Android. |
| Koleksi Saya tampak belum selesai | Lima kategori disembunyikan dalam menu, keadaan kosong hanya satu baris, dan entri tidak menyimpan mode bacaan. | Kategori langsung terlihat, ada petunjuk khusus setiap bagian, kartu penanda/audio, informasi unduhan, konfirmasi hapus unduhan dan riwayat, serta penyimpanan mode bacaan. |

Perbaikan terkait yang ditemukan saat membandingkan kode:

- **Lanjutkan bacaan** dan penanda mempertahankan mode mushaf, ayat, dan pilihan juz. Data build sebelumnya tetap terbaca; data lama yang belum menyimpan mode memakai Quran Indonesia sebagai fallback Android.
- **Quran per ayat** menampilkan satu ayat, navigasi sebelumnya/berikutnya, geser kanan untuk lanjut, dan menu bacaan. Pilihan juz melintasi batas surah sesuai juz tersebut.
- Teks Arab disejajarkan ke kanan dalam tata letak kanan-ke-kiri. Teks sumber tidak diubah.
- Pemilihan halaman mushaf memakai rujukan tujuan sejak pembaca dibuat, tanpa sempat memulai dari halaman pertama.
- Menu pindah bacaan memiliki pilihan surah, juz, halaman untuk edisi cetak, dan nomor ayat. Dialog audio dan pindah bacaan menyediakan tombol Tutup.

## Verifikasi

Hasil build, jumlah pengujian, hash paket dan tautan unduh yang telah diverifikasi dicatat pada [STATUS.md](STATUS.md) dan [VALIDATION.json](VALIDATION.json). Screenshot diambil dari aplikasi pada simulator iPhone XR. Pengujian simulator tidak menggantikan konfirmasi pada iPhone fisik pengguna.

Untuk memasang pembaruan, gunakan IPA baru melalui AltStore dengan Apple ID yang sama. Hindari menghapus aplikasi lama agar penyimpanan lokal tetap tersedia. Jalur pemasangan dan batas akun gratis ada di [UJI_IPHONE_XR.md](UJI_IPHONE_XR.md).
