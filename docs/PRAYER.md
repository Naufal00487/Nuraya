# Jam, Hijriah, jadwal salat, dan kiblat

Fitur Ibadah menggunakan perhitungan lokal. Sejak 1.3.1, saat pemakaian pertama Nuraya menjelaskan dan meminta izin lokasi Android untuk mengisi koordinat secara otomatis. Pilihan lokasi akurat atau perkiraan dihormati. Lokasi hanya diminta saat pengguna mengaktifkannya, tidak dilacak di latar belakang, dan tidak diunggah oleh Nuraya. Posisi tersimpan tidak diganti dengan koordinat tebakan. Jika izin ditolak, layanan Lokasi mati, atau koordinat tidak tersedia, aplikasi memberi tombol izin/pengaturan/coba lagi; Al-Qur’an tetap dapat dibaca. Lima belas kota serta koordinat manual tetap tersedia sebagai alternatif.

Lokasi memakai layanan Android GPS/network, tanpa ketergantungan Google Play Services atau reverse geocoding. Hanya posisi berumur paling lama dua menit dengan perkiraan akurasi yang memenuhi batas yang diterima. Zona waktu mengikuti pengaturan HP dan ditampilkan saat posisi ditemukan; aplikasi tidak menebak zona dari bujur. Koreksi Hijriah/menit pengguna dipertahankan. Permintaan lokasi dihentikan setelah hasil, batas waktu, pembatalan atau layar berhenti. Pembaruan lokasi saat bepergian tersedia di Ibadah → Lokasi → Gunakan lokasi HP.

## Jam dan kalender

Jam dan hitung mundur diperbarui setiap detik saat layar aktif. Tanggal mengikuti zona waktu lokasi yang dipilih; sebelum lokasi dipilih, jam mengikuti zona waktu perangkat dan dilabeli demikian. Kalender Hijriah memakai `java.time.chrono.HijrahDate` (Umm al-Qura), berdasarkan tanggal sipil. Koreksi −2 sampai +2 hari tersedia karena keputusan rukyat setempat dapat berbeda. Kalender tidak diklaim sebagai pengumuman resmi awal bulan, dan pergantian harinya bukan otomatis saat Magrib.

## Jadwal

Perhitungan memakai [Adhan Java 1.2.1](https://github.com/batoulapps/adhan-java), berlisensi MIT, melalui artefak Maven `com.batoulapps.adhan:adhan:1.2.1`. Sudut Subuh 20° dan Isya 18°, dengan Asar Syafii. Koreksi bawaan +2 menit berlaku untuk lima salat; matahari terbit tidak dikoreksi. Pengguna dapat mengubah koreksi −30 sampai +30 menit. Ini perhitungan astronomis, bukan salinan jadwal resmi Kemenag atau masjid tertentu.

Waktu Adhan dibulatkan ke menit; pecahan milidetik Calendar yang tidak terkait perhitungan dihapus supaya tampilan dan transisi hitung mundur sesuai. Terbit tampil sebagai informasi dan tidak dipilih sebagai salat keenam. Setelah Isya, target hitung mundur menjadi Subuh hari berikutnya. Saat lintang tinggi, batas porsi malam memakai metode sudut; jika tidak ada hasil lengkap (misalnya matahari tidak terbit), jadwal dinyatakan tidak tersedia, tanpa waktu buatan.

## Kiblat

Bearing memakai lintasan lingkaran besar dari koordinat pilihan ke Ka'bah, dihitung Adhan, relatif terhadap utara sejati. Pada Jakarta hasilnya sekitar 295°. Kompas memakai sensor rotation vector atau pasangan akselerometer/magnetometer, remapping sesuai rotasi layar, dan deklinasi magnetik Android `GeomagneticField`. Sensor hanya aktif saat layar kompas aktif/resumed dan dilepas saat layar berhenti.

Panah baru ditampilkan setelah ada pembacaan sensor. Akurasi rendah menampilkan permintaan kalibrasi dan tidak mengklaim perangkat sudah menghadap kiblat. Jika sensor tidak tersedia atau pendaftaran gagal, pengguna tetap melihat bearing numerik untuk dipakai dengan kompas lain. Pengguna diminta meletakkan HP mendatar dan menjauhkannya dari magnet/logam. Kompas fisik belum dinyatakan teruji hanya berdasarkan tes JVM.

## Pemeriksaan

`PrayerCalculatorTest` memeriksa jadwal rujukan Jakarta, zona/tanggal lokal, koreksi waktu, transisi tepat pada waktu salat, pergantian ke Subuh besok, pengabaian terbit dalam target salat, hitung mundur, kasus lintang kutub, arah kiblat, pembungkusan sudut, batas Ramadan/Syawal, serta penolakan masukan invalid. `PrayerPreferenceStoreTest` memeriksa pilihan lokasi yang kosong secara bawaan, persistensi, penghapusan koordinat, dan data tersimpan yang rusak. `PrayerPresentationTest` memeriksa alur pemilihan kota, munculnya hitung mundur, dan navigasi ke bearing kiblat melalui UI nyata yang dirender pada host. Hasil tes yang benar-benar dijalankan dicatat dalam laporan build utama.

## Pengingat azan 1.4.0

Ikon pengeras suara pada beranda awalnya nonaktif. Mengaktifkannya memerlukan lokasi tersimpan, izin notifikasi, dan izin alarm tepat waktu pada Android yang memerlukannya. Alarm menjadwalkan hanya salat berikutnya (bukan terbit), menjadwalkan ulang setelahnya, serta menyesuaikan setelah lokasi/koreksi waktu berubah, reboot, perubahan jam/zona, atau pembaruan aplikasi. Ketika izin dicabut, pengingat dibatalkan dan ditampilkan nonaktif saat aplikasi kembali dibuka.

Suara memakai rekaman Makkah yang dibundel dari DBChoco/Muezzin pada commit yang dicatat di `docs/sources/adhan-recording.json`; lisensi proyek sumber ikut dibundel. Ini bukan klaim identitas personal muazin. Audio mengikuti volume alarm HP, memakai fokus audio dan foreground service dengan tombol Hentikan azan, lalu melepas pemutar ketika selesai. Jadwal tidak mengharuskan internet setelah lokasi dipilih. Pembatasan sistem/hemat baterai dan kondisi perangkat nyata tetap perlu diperiksa pada HP pengguna.

`AdhanReminderTest` memeriksa keadaan awal nonaktif, satu alarm berikutnya, penggantian tanpa duplikasi, pembatalan, tidak memulai layanan tanpa lokasi/dengan alarm kedaluwarsa, serta pencabutan izin notifikasi. Tes ini tidak mengklaim pengeras suara HP nyata sudah diuji.
