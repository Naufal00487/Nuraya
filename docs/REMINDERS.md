# Pengingat lokal Nuraya

Pengaturan → Pengingat memisahkan dua notifikasi Quran per hari, pilihan tiap salat, waktu pengingat sebelum salat, dan alarm suara azan. Izin notifikasi Android diperlukan untuk notifikasi; izin alarm tepat waktu hanya diminta saat pengguna mengaktifkan suara azan. Instalasi baru memilih kelima salat dan pengingat Quran, dengan suara azan mati. Pilihan salat yang sudah disimpan versi sebelumnya tidak ditimpa.

## Isi dan jadwal

`assets/notifications/quran.json` adalah 100 kutipan/ringkasan yang disediakan penyusun, dipertahankan persis dari unggahan. Teks ini bukan pengganti terjemahan lengkap Kemenag dalam pembaca. Semua nomor surah dan ayat/rentang diverifikasi terhadap korpus kanonis; mengetuk pesan membuka ayat pertama rujukannya. `prayer.json` memuat 25 varian pengingat dari penyusun, juga tanpa perubahan pada sumber.

Setiap hari memakai dua ID pesan berbeda: satu jam acak 08.00–12.00 dan satu jam acak 15.00–sebelum 20.00, mengikuti zona waktu perangkat. Pilihan harian disimpan; aplikasi hanya menjadwalkan kejadian berikutnya. Catatan pengiriman mencegah slot yang sama diulang ketika aplikasi dibuka kembali atau jam diubah. Dua pesan dalam satu hari berjeda minimal tiga jam berdasarkan waktu pengiriman aktual, termasuk bila Android menunda pesan pertama. Pesan tidak dikirim setelah pukul 20.00, sebelum pukul 08.00, untuk tanggal sebelumnya, atau dari jadwal dengan zona waktu lama. Slot yang terlewat tidak dikirim bertumpuk; instalasi/pemberian izin pada sore hari dapat menerima kurang dari dua pesan hari itu.

Pengingat salat memakai lokasi dan penyesuaian jadwal tersimpan. Prioritas template: Ramadan (Subuh/Magrib bila tersedia), Jumat (Zuhur), per salat, umum. Koreksi tanggal Hijriah milik pengguna juga memengaruhi pilihan Ramadan. Pilih pengingat 5/10/15/30 menit. Bila Android terlambat mengirim, sisa menit disesuaikan agar tidak menyebut hitung mundur yang salah; setelah waktunya masuk, pesan berubah menjadi pemberitahuan waktu salat. Audio azan tidak dimulai jika terlambat dua menit atau lebih.

## Izin, privasi, dan batas Android

Semua jadwal, ID pesan yang dikirim, status popup, profil, dan koordinat berada di penyimpanan pribadi HP. Pengingat berjalan lokal tanpa server push, akun online, atau pengiriman profil/koordinat ke layanan notifikasi. Log pengingat lama dipangkas setelah 14 hari. Tidak ada permintaan pelacakan lokasi latar belakang.

Kartu izin notifikasi tampil paling sering tujuh hari sekali bila izin masih mati; jika jam HP mundur, kartu tidak langsung diulang. Pengenalan lokasi awal hanya sekali. Masuk kiblat tanpa izin tetap menawarkan pengaturan lokasi. Menutup kartu tidak menghalangi pembacaan Quran.

Pengingat dijadwalkan kembali ketika aplikasi aktif, HP selesai menyala, waktu/zona waktu berubah, atau aplikasi diperbarui. Tanpa izin alarm tepat waktu, notifikasi memakai alarm biasa Android. Doze, optimasi baterai, pengaturan kanal notifikasi, HP mati, atau force-stop dapat menunda/menghilangkan pengiriman. Membuka kembali aplikasi menjadwalkan kejadian yang masih akan datang; aplikasi tidak menjanjikan ketepatan waktu yang tidak dapat dijamin Android. Alarm azan memerlukan izin alarm tepat waktu, lokasi tersimpan, dan izin notifikasi.

Tes JVM memeriksa pemilihan template, 100 referensi, 1.000 rencana acak, pengiriman ulang, keterlambatan, jam tenang, zona waktu, izin terpisah, pembukaan ayat, dan batas popup mingguan. Pengiriman aktual dengan layar mati/Doze dan keluaran azan masih perlu diperiksa pada HP fisik.
