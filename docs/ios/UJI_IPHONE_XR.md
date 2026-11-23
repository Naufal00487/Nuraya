# Memasang dan mencoba Nuraya pada iPhone XR

Paket iOS ini untuk pengujian pribadi. Minimum iOS 16.0. Kode dan APK Android tetap tersedia di repositori yang sama.

## Pemasangan dari Windows

Siapkan iPhone XR, kabel USB, komputer Windows, koneksi internet, dan Apple ID milik sendiri. Akun Apple Developer berbayar belum diperlukan untuk jalur uji pribadi ini.

1. Ikuti [panduan resmi AltStore Classic untuk Windows](https://faq.altstore.io/altstore-classic/how-to-install-altstore-windows). Panduan tersebut memuat tautan iTunes dan iCloud versi Apple yang diperlukan, pemasangan AltServer, serta pengaturan sinkronisasi Wi-Fi.
2. Sambungkan iPhone ke Windows, buka kuncinya, lalu pilih **Percayai/Trust** jika diminta. Jalankan AltServer dan gunakan **Install AltStore → iPhone**. Masukkan Apple ID hanya pada alat pemasangan di komputer sendiri.
3. Pada iPhone, percayai profil pengembang melalui **Pengaturan → Umum → VPN & Manajemen Perangkat**. Aktifkan **Pengaturan → Privasi & Keamanan → Mode Pengembang**, kemudian ikuti permintaan mulai ulang. Nama menu dapat berbeda mengikuti bahasa iOS.
4. Unduh `Nuraya-iOS-XR-unsigned.ipa` dari [halaman unduh iOS Nuraya](https://github.com/Naufal00487/Nuraya/tree/downloads/nuraya-ios-preview). Simpan berkas IPA di aplikasi **Files/File** pada iPhone. Jika diunduh dari Windows, pindahkan berkas tersebut ke Files.
5. Buka **AltStore → My Apps → +**, pilih IPA Nuraya, lalu tunggu proses penandatanganan dan pemasangan. Biarkan AltServer aktif dan komputer serta iPhone tersambung selama proses.
6. Buka Nuraya, isi nama profil, lalu izinkan lokasi untuk menghitung jadwal salat dan kiblat. Jika lokasi tidak tersedia, layar pengaturan lokasi menyediakan pilihan manual.

IPA berlabel **unsigned** ditandatangani oleh AltStore menggunakan akunmu saat dipasang. Jangan mengubah ekstensi IPA menjadi APK atau ZIP untuk memasangnya.

Untuk memperbarui pemasangan yang sudah ada, pilih IPA baru melalui AltStore menggunakan Apple ID yang sama. Jangan hapus Nuraya lebih dahulu. Setelah pembaruan, periksa nomor **iOS build 2** di bagian bawah Pengaturan; profil dan koleksi yang ada seharusnya tetap tersedia.

Akun Apple gratis biasanya memerlukan **Refresh All** di AltStore sebelum tujuh hari dan memiliki batas aplikasi aktif. Biarkan AltServer tersedia pada komputer ketika memperbarui. Ketentuan terbaru mengikuti panduan resmi AltStore dan Apple. Hapus aplikasi hanya jika siap kehilangan data lokal; penanda, profil, progres, dan unduhan berada di penyimpanan aplikasi.

## Urutan uji di perangkat

Pengujian simulator sudah mencakup data, mesin pengenal bacaan dengan rekaman uji, serta sejumlah alur layar. Pemeriksaan berikut memerlukan iPhone fisik dan belum dinyatakan lulus oleh pengujian simulator.

| Bagian | Langkah | Hasil yang perlu diperiksa |
| --- | --- | --- |
| Splash | Tutup proses Nuraya lalu buka kembali | Gambar dua maskot asli tampil saat aplikasi dimulai; bukan hanya logo kecil. |
| Pergantian tab | Al-Qur’an → Quran per ayat → sebelum memilih surah, tekan tab Murottal; ulangi dari mode Madinah dan Tematik | Halaman Murottal tampil dan dapat ditekan. Daftar surah atau tema tidak menutupi tab baru. |
| Header | Buka tab Al-Qur’an dan Murottal dalam tema terang dan gelap | Gambar atas memenuhi lebar konten seperti header Android, dengan judul dan keterangan terbaca. |
| Koleksi Saya | Buka dari Beranda, pilih Penanda, Favorit, Daftar putar, Unduhan, Riwayat | Kelima kategori terlihat tanpa menu tersembunyi. Bagian kosong memiliki petunjuk; entri yang ada dapat dibuka, diputar atau dihapus sesuai jenisnya. |
| Melanjutkan bacaan | Buka An-Nas dalam Quran per ayat, maju satu ayat, tandai, tutup. Buka Lanjutkan bacaan dan penanda di Koleksi | Keduanya kembali ke An-Nas ayat 2 dalam mode Quran per ayat. Ulangi dengan mushaf Madinah agar pilihan edisi juga tetap. |
| Profil dan lokasi | Isi nama, izinkan lokasi, buka Beranda dan Ibadah | Nama tersimpan, nama daerah muncul, tanggal Hijriah dan jadwal salat terbaca. |
| Mushaf | Buka Quran Madinah; pilih An-Nas, lalu surah di bagian tengah Quran | An-Nas menuju halaman 604. Surah lain menuju halaman awal sesuai indeksnya. Ketuk halaman untuk menyembunyikan/menampilkan tombol; coba cubit untuk zoom dan geser halaman. |
| Mode Quran | Buka per ayat, per kata/tajwid, terjemahan, dan mushaf cetak | Mode menampilkan sumber dan panel yang sesuai; gambar mushaf dapat dibuka setelah unduhan bila belum tersimpan. |
| Audio Quran | Pilih qari, putar rentang dua ayat, tutup pembaca | Audio bacaan berhenti. Dengan qari yang memiliki timing kata, sorotan mengikuti rekaman yang sama. Qari lainnya memakai audio per ayat. |
| Murottal | Pilih qari dan surah, kecilkan pemutar, kunci layar | Audio tetap berjalan; tombol jeda/lanjut pada layar terkunci bekerja. Coba headset serta interupsi panggilan. |
| Unduhan | Unduh satu audio, putus koneksi, putar dari Koleksi | Audio tersimpan dapat diputar tanpa mengunduh ulang. |
| Pencarian Indonesia | Cari `Yasin`, `2:255`, dan `ayat kursi`; coba dikte “cari surat Yasin” | Hasil dikte tetap menjadi pencarian Indonesia. Jika layanan suara lokal Apple tidak tersedia, gunakan pilihan layanan daring atau ketik. |
| AI lokal | Buka AI bacaan, lakukan cek mikrofon lima detik, unduh model, pilih Al-Ikhlas ayat 1, lalu mulai membaca | Indikator suara bergerak dan perkiraan kata muncul. Catat waktu proses setelah **Selesai membaca**, suhu perangkat, dan hasil transkrip. |
| Hafalan dan kuis | Buka Belajar, pilih level bebas; simpan sesi. Coba hafalan rentang bebas dan penilaian mandiri | Sesi, progres, dan catatan tetap ada setelah aplikasi ditutup lalu dibuka. Target harian opsional; tidak ada kompetisi atau level terkunci. |
| Pengaturan | Ubah tema gelap, ukuran Arab, qari, serta warna tajwid | Pengaturan tersimpan dan dipakai oleh pembaca. Navigasi utama tetap dapat dijangkau. |
| Kiblat | Buka Ibadah → Kiblat, kalibrasi dan putar iPhone jauh dari magnet | Sensor arah berubah; arah numerik sesuai koordinat. Simulator tidak dapat membuktikan ketepatan sensor. |
| Notifikasi | Izinkan notifikasi, periksa jadwal berikutnya, coba suara azan jika diinginkan | Pemberitahuan mengikuti izin suara/Fokus iOS. Suara azan default mati; cuplikan notifikasi berdurasi 28 detik. |

Model AI sekitar 59,7 MB diunduh sekali dan diperiksa hash-nya. Jalur AI lokal memproses suara di iPhone; rekaman/transkrip tidak disimpan. Hasil berupa perkiraan kata secara berkala, belum menjadi penilaian tajwid, makhraj, atau panjang mad yang tervalidasi. Kecepatan pada chip A12 iPhone XR perlu diukur pada perangkat.

Notifikasi dijadwalkan beberapa hari ke depan karena batas permintaan lokal iOS, lalu diisi ulang ketika Nuraya dibuka. Tidak dijanjikan alarm terus-menerus tanpa membuka aplikasi lagi.

## Jika menemui masalah

Untuk masalah pemasangan, catat **pesan error lengkap dari AltStore/AltServer**, versi iOS, dan pada langkah mana error muncul. Untuk masalah aplikasi, sebutkan menu, surah/ayat atau nama qari, langkah pemicu, serta apakah sedang online. Screenshot atau rekaman layar singkat membantu menunjukkan letak masalah; jangan menyertakan kata sandi atau kode verifikasi Apple ID.
