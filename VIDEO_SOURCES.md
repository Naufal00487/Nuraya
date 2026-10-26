# Katalog video Nuraya 1.2.0

Katalog berisi 12 rujukan: 8 seri/kursus, 1 lokakarya, dan 3 Shorts yang dipetakan penerbit ke surah. Pencarian hanya mencari katalog belajar ini, dengan filter tingkat, format, topik, dan simpan untuk ditonton nanti. Tidak ada pencarian web umum atau feed hiburan.

## Batas verifikasi

Akses langsung YouTube (halaman, pencarian dan metadata) dibatasi proxy cloud dengan HTTP 403, termasuk setelah pemeriksaan dengan izin tambahan. Karena itu, **pemutaran, judul YouTube terkini dan isi video secara penuh belum diverifikasi**. Tautan dan label sumber diperiksa melalui referensi publik yang tersedia. Ini tidak disajikan sebagai audit ajaran atau sertifikasi pengajar.

- Seri/tutorial berasal dari daftar rujukan `JobeEnterprise/awesome-islam` dan `sajee05/Learn-Arabic-Speaking-Reading-and-Quran`. Hanya materi baca, tajwid, bahasa Al-Qur’an, hafalan dan pemahaman Al-Qur’an dipilih. Bagian lain dari daftar tersebut tidak dimasukkan.
- Tiga Shorts berasal dari `Zubair2524/quran-shorts/links.js`, tempat penerbit secara eksplisit memetakan ID video ke surah 112, 108 dan 103. Judul Indonesia/Inggris adalah label editorial berdasarkan pemetaan itu; bukan klaim judul asli YouTube. Nama pembicara dan bahasa tidak ditebak. Audio ditandai “periksa di sumber”, dan detail mencantumkan batas verifikasi.
- Tingkat pemula/menengah/lanjutan adalah panduan urutan belajar Nuraya, bukan sertifikasi keahlian atau jaminan menjadi ahli. Kursus lanjutan mencakup pemahaman bahasa/penjelasan Al-Qur’an.
- Bahasa hanya dicantumkan jika diketahui dari identitas/uraian sumber; nilai `und` berarti belum dikonfirmasi. Materi berbahasa Indonesia belum berhasil diverifikasi dalam katalog ini.

## Sumber tetap

Commit sumber, checksum referensi dan setiap URL disimpan di `docs/sources/video-catalog-references.json`. Tidak ada ID video yang dibuat oleh model. Field `originalTitle` menyimpan judul referensi atau pemetaan penerbit; pada Shorts ia bukan judul YouTube yang diklaim telah dibaca.

| Materi | Format | Sumber asli |
| --- | --- | --- |
| Dari huruf Arab ke bacaan Al-Qur’an | course | [Arabic101](https://www.youtube.com/@Arabic101/courses) |
| Tajwid langkah demi langkah | course | [Yasir Qadhi](https://www.youtube.com/playlist?list=PLSzUb0TgsIYsxT5mMj1KSFDXUEJ3psMCU) |
| Belajar tajwid dengan penjelasan bertahap | course | [FreeQuranEducation](https://www.youtube.com/playlist?list=PLwNeHLk_z0aSekqYqJdRtuYS74rcGXoWF) |
| Pengantar ilmu tajwid | course | [SafinaSociety](https://www.youtube.com/playlist?list=PLZ6keVEpgaQsQllF_6KYJ2EoQZaL4s7gJ) |
| Pendalaman pelajaran tajwid | course | [Shaykh Sajjad Gul](https://www.youtube.com/playlist?list=PLYO6Oz7uwCSgOvNlQPSbd95_uAh4zb1NP) |
| Lokakarya membaca dan menghafal Al-Qur’an | lesson | [Connect with the Qur’an workshop](https://www.youtube.com/watch?v=BKdYbwxKndU) |
| Contoh lantunan untuk latihan menyimak | course | [Shaykh Zakaullah Saleem](https://www.youtube.com/playlist?list=PLYRXQljU5MiIlQb5NL0z4EIrlbwlTGoqQ) |
| Pendalaman bahasa Arab Al-Qur’an | course | [Dr. Khalid Zaheer](https://www.youtube.com/playlist?list=PL8OYNJDeWEeHSbrInC4lBOstp3nNid-Yi) |
| Memahami Al-Qur’an bersama penjelasan | course | [Taimiyyah Zubair](https://www.youtube.com/watch?v=dBCUsU71cIs&list=PL5YuxmQcBQgIBv1wi9ISZA1lAVMbOY7g3) |
| Shorts: mengenal Surah Al-Ikhlas | short | [Quran in Shorts · Zubair2524](https://www.youtube.com/shorts/uwuqUvJ0cAc) |
| Shorts: mengenal Surah Al-Kautsar | short | [Quran in Shorts · Zubair2524](https://www.youtube.com/shorts/srP9YLeLXWw) |
| Shorts: mengenal Surah Al-Asr | short | [Quran in Shorts · Zubair2524](https://www.youtube.com/shorts/0OUTefkON3s) |

Semua entri tetap menggunakan `license: link-only` dan `streamUrl: null`. Mulai 1.2.0, URL video/Shorts/playlist yang valid dapat dibuka melalui pemutar resmi YouTube IFrame di aplikasi, sesuai izin embed penerbit. Halaman kanal/kursus dan video yang tidak mengizinkan embed tetap memakai sumber asli. Nuraya tidak menyalin atau mengunduh video; `link-only` bukan lisensi redistribusi. Hak cipta tetap pada penerbit. Favorit bersifat lokal; riwayat embed hanya dicatat setelah pemutar melaporkan mulai bermain, bukan sebagai bukti menonton selesai. Rincian integrasi dan batas verifikasi ada di [VIDEO_EMBED.md](VIDEO_EMBED.md).

## Pengujian

`VideoCatalogueTest` memeriksa isi katalog, tautan tanpa stream palsu, tingkat/format, pencarian multi-kata, dan penolakan tag di luar konteks. Test UI memeriksa filter Shorts serta pencarian tanpa hasil untuk topik tidak terkait. Hasil akhir dicatat di `BUILD_VALIDATION.md`. Pengujian ini tidak menggantikan pemutaran video pada perangkat.
