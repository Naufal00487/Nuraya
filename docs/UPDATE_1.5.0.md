# Nuraya 1.5.0 — revisi tampilan dan pembaca mushaf

Revisi dari `revisi nuraya.docx`, dengan jawaban pengguna: target pilihan 17 qari dan pemakaian halaman Madinah Nuraya yang terverifikasi beserta panel pendamping. Paket `com.nuraya.app`, versionCode 10, Android 8.0+.

## Tampilan dan lokasi

- Pengaturan di bar atas memakai ikon gir. Koordinat otomatis diperkaya nama kecamatan melalui Android Geocoder. Hasil “Kec. Kelapa Dua”, misalnya, tampil sebagai “Kelapa Dua”. Jika alamat tidak tersedia, koordinat dipertahankan; nama tidak ditebak. Geocoder dapat memerlukan internet, dibatasi enam detik, dan hasilnya disimpan lokal.
- Latar Ibadah, Quran dan Murottal serta 12 sampul menggunakan gambar asli dokumen pengguna. Quran/Murottal memilih gambar terang atau gelap sesuai tema. Nomor surah memakai bingkai bintang emas.
- Splash baru berada di luar Scaffold. Navigasi dan pemutar mini tidak ditampilkan di bawahnya. Android 12+ tetap mempunyai tahap splash sistem yang diwajibkan OS; tahap tersebut kini putih dengan ikon kosong, lalu langsung disambung gambar baru, tanpa layar hijau/logo lama.
- Insets status bar, cutout dan navigasi sistem dikonsumsi di akar tampilan. Pemeriksaan UI memasukkan area navigasi tiga tombol setinggi 48 px dan memastikan tombol aplikasi berada di atasnya.

## Ibadah

Masing-masing Subuh, Zuhur, Asar, Magrib dan Isya mempunyai sakelar pengingat: 10 menit sebelumnya serta saat masuk waktu. Suara azan terpisah dari sakelar notifikasi. Alarm berikutnya dijadwalkan satu per satu, tidak memasukkan Terbit; kunci jadwal menolak pemutaran ulang pesan alarm. Izin Android tetap diperlukan. Semua pilihan awalnya nonaktif; pilihan azan lama dipertahankan sebagai lima pilihan pengingat ketika masih diizinkan.

## Membaca seperti kitab

- Halaman asli Madinah dengan bingkai, ketuk sekali untuk tampil/sembunyikan tombol, ketuk dua kali untuk lanskap. Menu juga menyediakan tombol orientasi. Lanskap memenuhi lebar dengan gulir vertikal.
- Lompat halaman, surah/ayat atau juz; sebelumnya/berikutnya; pembesaran sampai 300%; simpan halaman offline ke penyimpanan privat persisten setelah checksum cocok.
- Panel arti Kemenag/QuranEnc, Latin Indonesia EQuran.id, ukuran Arab, arti per kata, tafsir dan tajwid bersumber. Gambar Quran asli tetap utuh; pewarnaan dan sorotan audio berada di panel teks pendamping.
- Pilihan 17 qari dengan pencarian/foto yang sudah dipetakan, pengulangan tiap ayat 1–20×, dengarkan dan jeda. Pilihan baca lain menyediakan tombol “Buka tampilan kitab Madinah”; fungsi khusus hafalan/Tikrar tetap ada di pembaca per ayat.
- Sampul adalah ilustrasi navigasi, bukan pernyataan bahwa setiap halaman edisi cetak pada foto disertakan. Bahriyah tetap memakai PDF lokal pengguna.

## Cakupan sorotan bacaan

Semua **17 qari** tetap mengikuti per ayat. Sorotan **per kata tersedia untuk 12 qari**, termasuk Alafasy serta 11 tambahan dari QUD Technologies v4.0.0: Abdul Basit, Abdullah Basfar, Sudais, Shatri, Ahmed al-Ajmi, Hani ar-Rifai, Hudhaify, Husary, Husary Mujawwad, Maher al-Muaiqly dan Shuraim. Setiap waktu memakai URL rekaman pasangannya, bukan meminjam waktu qari lain. Di sebelas tambahan, 68.528 klip ayat lolos pemetaan dan pemeriksaan interval; 68 lainnya menggunakan file ayat/sorotan per ayat. Cakupan Alafasy lama tetap 6.142 ayat bersorotan kata dan 94 ayat fallback.

**Belum seluruh 17 mendukung per kata.** Ayman Sowaid, Ibrahim Akhdar, Muhammad Ayyoub, Muhammad Jibreel dan Parhizgar tetap per ayat karena pasangan sumber yang dapat diintegrasikan belum terverifikasi. Data dari rekaman YouTube/Drive atau edisi lain tidak diterapkan secara dugaan ke file Nuraya. Jika sumber bertanda waktu gagal diputar, aplikasi mencoba file individual qari yang sama sekali dan menghapus sorotan kata yang tidak lagi sesuai.

Empat sampel Al-Fatihah tambahan berhasil diunduh dan didekode di host. Tujuh sumber lainnya diblokir proxy cloud, termasuk setelah percobaan dengan akses tambahan; belum diklaim berhasil diputar di HP. [Audit interval](audit/1.5.0/word-timing.json), [sampel rekaman](audit/1.5.0/paired-audio-samples.json), [asal dan lisensi](sources/qud-word-timing.json).

## Data dan fitur yang dipertahankan

Teks 6.236 ayat, aturan basmalah, terjemahan, AI lokal, latihan hafalan, kuis, progres, profil, koleksi dan foto qari dipertahankan. Tidak ada deploy bot atau pengiriman Telegram. [Laporan validasi](BUILD_VALIDATION.md) menjelaskan pengujian dan batas perangkat nyata.
