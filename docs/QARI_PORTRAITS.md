# Foto qari Nuraya 1.7.1

Seluruh **28 pilihan qari** memiliki foto yang dibundel offline, dipakai bersama oleh Murottal, pemutar mini/penuh, pengaturan Quran dan pilihan audio mushaf. Delapan foto profil berasal dari Quran.com; dua puluh foto tambahan diambil byte demi byte dari dokumen penyusun **bentarr lagi selesai 02.docx**, tanpa modifikasi gambar. Pemotongan tampilan mengikuti ukuran komponen Android.

Pemetaan mengikuti label nama eksplisit di dokumen dan pasangan ID rekaman surah/ayat. Tidak ada pencocokan nama samar atau klaim verifikasi identitas wajah secara independen. Ali Jaber dan Abdullah Al-Matrood dihapus dari pilihan atas permintaan penyusun. Berkas audio/progres pengguna yang sudah tersimpan tidak dihapus.

`app/src/main/assets/portraits/catalog.json` mencatat nama, ID audio, sumber dokumen/gambar atau URL Quran.com, jumlah byte, SHA-256 dan keterangan hak. Foto Quran.com dipin ke commit `aff1a035b09b66f28047b3216edcae4c5c949a49`. Hak setiap foto tetap pada pemiliknya; lisensi MIT proyek Quran.com tidak dianggap sebagai lisensi terbuka setiap foto. Foto unggahan tidak diklaim sebagai karya fotografi penyusun.

Kredit foto ditampilkan di Pengaturan → Sumber & Atribusi, bukan di layar pemutar. Desain gambar profil/avatar, banner, splash screen, logo dan maskot dikreditkan terpisah kepada **Naufal Irfan Ramadhan** sesuai keterangannya.

Tes memeriksa hash, format gambar, alias audio, kelengkapan seluruh 28 pilihan dan pengecualian dua qari tersebut. Tes UI memeriksa identitas gambar, pemilihan audio, serta tampilan pemutar. Pemeriksaan ini bukan autentikasi suara atau uji speaker HP. Lihat [validasi](BUILD_VALIDATION.md).
