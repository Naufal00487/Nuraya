# Jam, Hijriah, jadwal salat, dan kiblat

Fitur Ibadah menggunakan perhitungan lokal. Tidak ada izin lokasi, pelacakan latar belakang, layanan geolokasi, atau pengiriman koordinat. Pada pemakaian pertama pengguna harus memilih kota atau mengisi koordinat dan zona waktu; aplikasi tidak menganggap pengguna berada di Jakarta. Lima belas kota mencakup WIB/WITA/WIT. Pencarian kota, isian manual, dan penghapusan lokasi tersimpan tersedia di tab Lokasi.

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
