# Nuraya 1.6.0

Pembaca Quran sekarang menampilkan **tujuh edisi gambar asli**, masing-masing 604 halaman: Indonesia, Indonesia Tajwid, Madinah, Madinah Tajwid, Per Kata Tajwid, Latin Tajwid, dan Hafalan 8 Blok. Per Kata menampilkan arti yang memang tercetak di bawah kata; Latin dan Hafalan juga memakai halaman edisinya sendiri. Sumber publik ditemukan setelah memeriksa APK dan rekaman layar yang diberikan pengguna.

## Membaca dan memilih ayat

- Halaman penuh saat masuk. Ketuk sekali untuk header tipis dan satu bar audio; ketuk lagi untuk menyembunyikannya. Panel terjemahan tidak mengubah ukuran halaman.
- Geser kanan/kiri untuk berpindah halaman; ketuk dua kali untuk lanskap. Zoom dan navigasi juga tersedia di menu.
- Pilihan surah/ayat memakai indeks sesuai edisi untuk seluruh 114 surah dan 6.236 ayat. Posisi audio lama tidak mengembalikan pilihan ke Al-Baqarah 2:6.
- Tekan lama pada ayat di enam edisi berpeta untuk memutar, menandai, menyalin, membagikan, atau membuka arti/tafsirnya. Sorotan ayat mengikuti rekaman aktif.
- Warna cetak mengikuti pilihan warna pengguna; jika dimatikan, tampilan gambar menjadi grayscale. Berkas sumber tidak diubah.
- Halaman 1, 2, 3, 7, 8 dan 604 tersedia langsung pada setiap edisi. Menu **Unduh 604 halaman edisi ini** menyediakan penyimpanan offline dengan progres; unduhan yang terhenti dapat dilanjutkan. Dialog unduhan perlu tetap terbuka.

## Audio dan latihan

Pengaturan murottal menyediakan surah/ayat awal dan akhir, termasuk rentang lintas surah; ulang tiap ayat, putaran seluruh rentang, atau loop hingga dijeda. Antrean memakai nomor ayat kanonis dan pasangan rekaman/timing qari yang sama. Pemilihan nama/foto qari yang sudah dikurasi tetap dipertahankan.

Tikrar memakai halaman Indonesia Tajwid dan penghitung latihan mandiri. Waqaf menyediakan panduan tanda serta ibtida. Hafalan memakai blok warna cetak dan pilihan menyembunyikan halaman untuk menguji ingatan. Al-Qur’an Per Ayat dan Terjemah Tematik tetap menyediakan fungsi tersendiri.

AI bacaan lokal, Teman Belajar, tafsir, kuis, progres, profil wajib, lokasi, salat dan kiblat dipertahankan. Video/Shorts dan Bahriyah/pojok tetap tidak disertakan. Tidak ada layanan bot baru yang di-deploy.

## Batas yang perlu diketahui

- Sumber Hafalan **belum memiliki peta koordinat ayat**. Halamannya lengkap, tetapi sorotan dan tekan lama untuk memilih ayat di gambar Hafalan belum tersedia. Pemilihan surah/ayat, audio dan panel arti tetap berfungsi.
- Sorotan pada gambar mengikuti **ayat**. Sorotan **kata** tersedia pada rincian teks untuk rekaman dengan timing yang sesuai, bukan di gambar halaman.
- Terjemahan/Latin yang tercetak mengikuti edisi gambar. Panel tambahan memakai sumber Nuraya dengan atribusinya sendiri.
- Seluruh 4.228 gambar diperiksa hash/dekode; dua kekurangan peta ayat diperbaiki lewat pemeriksaan visual. Ini bukan pentashihan semua huruf/harakat oleh ahli. [Sumber dan audit](PRINTED_MUSHAF.md).
- Pengujian otomatis dan render host tidak menggantikan uji HP fisik. AI tetap bantuan pengenalan/perbandingan kata, bukan penilai tajwid/makhraj tervalidasi.

Paket `com.nuraya.app`, versionCode 14, minimum Android 8.0. [Hasil validasi](BUILD_VALIDATION.md).
