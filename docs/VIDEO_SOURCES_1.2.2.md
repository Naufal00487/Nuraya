# Katalog video Nuraya 1.2.2

Katalog mengganti seluruh 12 rujukan lama dengan **7 tautan berbahasa Indonesia yang diberikan pengguna**: 4 video pelajaran dan 3 playlist. Urutan belajar mengikuti tajwid yang benar → pengucapan jelas → kelancaran → keindahan suara dan maqam. Pencarian hanya mencakup katalog ini, dengan filter tingkat, format, topik, dan Tonton nanti; tidak mencari seluruh YouTube.

## Materi dan urutan belajar

| Materi | Kanal/sumber yang diberikan | Format | Tingkat | Tautan |
| --- | --- | --- | --- | --- |
| 3 JAM MAHIR MEMBACA ALQURAN DARI 0 HINGGA MAHIR | Ngaji Bareng | Video, sekitar 3 jam | Pemula | [Tonton](https://www.youtube.com/watch?v=qj0qQ157jlg) |
| PENJELASAN LENGKAP ILMU TAJWID (HUKUM BACAAN) ALQUR'AN | Ngaji Bareng | Video, sekitar 3 jam 46 menit | Pemula | [Tonton](https://www.youtube.com/watch?v=QgCZizuFhoQ) |
| Kumpulan Materi Tajwid 1-30 | Uda Ridwan Channel | Playlist | Pemula | [Buka playlist](https://www.youtube.com/playlist?list=PLjDKbvDswLYpBkZBJX3pQjDNIne-YdwEc) |
| BELAJAR LENGKAP TINGKATAN MAQAM | Tilawah Qur’an Channel | Playlist | Menengah–lanjutan | [Buka playlist](https://www.youtube.com/playlist?list=PLkzw0NkHAvYxHKdkvjb6RqzhWU7GEWCwB) |
| BELAJAR BAYYATI, HIJAZ, NAHAWAND DENGAN SUARA JELAS | Nama kanal belum diberikan | Video, sekitar 20 menit | Menengah | [Tonton](https://www.youtube.com/watch?v=9SI4HgYA_pk) |
| BAYATI, HIJAZ, NAHAWAND, ROST & SIKAH SANGAT MUDAH DIIKUTI QORI' PEMULA | Seni Tilawatil Qur’an | Video | Menengah | [Tonton](https://www.youtube.com/watch?v=42VQXkAxCQY) |
| Belajar Qiroah Dari Dasar Sampai Mahir | MQ Tilawatil Anshar | Playlist | Menengah–lanjutan | [Buka playlist](https://www.youtube.com/playlist?list=PLo2rrrmYLhcxVDlDJDzzyU79zlkVe66gL) |

Pemula memulai dari huruf hijaiyah, harakat, makhraj, sifat huruf dan hukum tajwid dasar. Materi maqam ditempatkan di tingkat menengah setelah dasar bacaan cukup kuat, meskipun judul penerbit menyebut qari pemula. Tingkat lanjutan menampilkan kembali dua playlist yang diberikan untuk latihan perpindahan maqam, variasi dan penghayatan. Field `additionalLevels: ["advanced"]` membuat playlist itu muncul pada kedua filter tanpa menggandakan tautannya. Tingkat ini adalah panduan belajar Nuraya, bukan sertifikasi keahlian.

Entri simpan/riwayat video yang ID-nya tidak lagi ada di katalog tidak ditampilkan di Koleksi; data pribadi lain tetap dipertahankan.

Daftar yang diberikan tidak berisi URL Shorts. Semua Shorts lama dihapus dari katalog; video panjang tidak diberi label Shorts. Implementasi feed tetap tersedia dengan keadaan kosong, dan tombol masuk utama tidak ditampilkan saat katalog tidak memiliki Shorts.

## Asal metadata dan batas verifikasi

Judul asli, nama kanal, bahasa, uraian dan perkiraan durasi mengikuti rekomendasi pengguna. `originalTitle` menyimpan judul yang diberikan; judul kartu diringkas agar mudah dipindai. Kanal video `9SI4HgYA_pk` tidak ditebak: label kartu “Tutorial maqam” merupakan label materi, bukan atribusi kanal. Semua entri memakai `audioLanguage: id`, `metadataSource: user-provided`, `addedAt: 2026-10-27`, dan `verifiedAt` kosong. `verificationUrl` menunjuk ke URL asli yang diberikan, bukan bukti pemeriksaan pemutaran.

Akses metadata/halaman YouTube dari cloud dibatasi HTTP 403, termasuk percobaan dengan izin tambahan. Karena itu, **pemutaran, judul YouTube terkini dan isi video secara penuh belum diverifikasi secara independen**. Bahasa Indonesia merupakan informasi dari rekomendasi pengguna. Tidak ada URL, nama kanal atau izin embed tambahan yang ditebak. Manifest katalog saat ini, tujuh URL, dan checksum aset dicatat di `docs/sources/video-catalog-references.json`. Referensi lama dipertahankan hanya sebagai arsip `docs/sources/video-catalog-references-1.2.1.json`; arsip itu tidak dipakai aplikasi.

Semua entri memakai `license: link-only` dan `streamUrl: null`. Ketujuh URL mempunyai bentuk video/playlist yang didukung pemutar resmi YouTube IFrame di aplikasi; penerbit tetap menentukan ketersediaan dan izin embed. Nuraya tidak menyalin atau mengunduh video, dan `link-only` bukan lisensi redistribusi. Hak cipta tetap pada penerbit. Favorit bersifat lokal; riwayat embed hanya dicatat setelah pemutar melaporkan mulai bermain, bukan sebagai bukti menonton selesai. Rincian integrasi ada di [VIDEO_EMBED.md](VIDEO_EMBED.md).

## Pengujian

`VideoCatalogueTest` memeriksa tujuh URL yang diharapkan, metadata bahasa/provenansi, tidak adanya stream palsu atau Shorts lama, tingkat belajar, dan penolakan tag di luar konteks. `VideoSmartSearchTest` memeriksa pencarian kebutuhan belajar serta nama maqam. Pengujian UI mencakup katalog baru dan keadaan Shorts kosong; perilaku geser feed tetap diuji dengan data uji terpisah. Hasil pemeriksaan yang benar-benar dijalankan dicatat di [BUILD_VALIDATION.md](BUILD_VALIDATION.md). Pengujian katalog tidak membuktikan pemutaran video pada perangkat.
