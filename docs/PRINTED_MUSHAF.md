# Tujuh edisi mushaf bergambar

Nuraya menggunakan 604 halaman **masing-masing** untuk Indonesia, Indonesia Tajwid, Madinah, Madinah Tajwid, Per Kata Tajwid, Latin Tajwid, dan Hafalan 8 Blok. Gambar Per Kata Al-Fatihah cocok dengan halaman contoh pengguna: bingkai, susunan baris, dan terjemahan tiap kata tercetak pada gambarnya. Gambar tidak dirakit dari screenshot, tidak di-OCR, dan tidak digambar ulang.

## Sumber dan pemeriksaan

Sumber publik: [Quran Image Assets — agusibrahim/quran-image](https://github.com/agusibrahim/quran-image/tree/df1ca931ca6e83f8ff6b2d3a9308406e11994d00), commit `df1ca931ca6e83f8ff6b2d3a9308406e11994d00`. README sumber menyebut penggunaan untuk aplikasi pembaca Quran serta perlunya memperhatikan atribusi/lisensi. Repositori tidak menyediakan berkas LICENSE terpisah; Nuraya tidak menyatakan aset ini berlisensi CC, memiliki sertifikasi cetak tertentu, atau memiliki hubungan resmi dengan Islampedia.

Pada 13 November 2026, seluruh **4.228 PNG**, total **937.239.789 byte**, berhasil diunduh, dicocokkan dengan hash Git blob dari commit tetap, dihitung SHA-256-nya, dan didekode. Tidak ada kegagalan hash/dekode. Setiap edisi memiliki 604 hash unik. Manifest aplikasi menyimpan URL tetap, ukuran dan kedua hash setiap halaman. Pemeriksaan tersebut memverifikasi integritas berkas; bukan pentashihan ahli atas seluruh huruf, harakat, latin, terjemahan dan tanda waqaf.

Indeks faktual surah/ayat/halaman berasal dari database APK Islampedia 1.9.10 yang diberikan pengguna (SHA-256 `c992c27d56897c62df6f515313303b9f1efdda1c8bca36c332d5c6ab16e202cc`). Tidak ada kode, kredensial, teks Quran, atau API privat APK tersebut yang disalin. Indeks Indonesia dan Madinah masing-masing mencakup 6.236 ayat. Indeks Madinah baru **berbeda pada 56 ayat** dari gambar Madinah lama Nuraya: jangan mencampur kedua edisi. Contoh terverifikasi visual: 69:35 ada di halaman 567 edisi baru, tetapi di halaman 568 edisi lama.

## Sorotan dan pemilihan ayat

Pemetaan koordinat berasal dari `map.json` publik, dengan viewport yang sesuai gambar. ID/ID Tajwid, MD/MD Tajwid, serta Per Kata/Latin masing-masing berbagi peta yang identik pada sumber. Nilai `blok` pada peta berarti pecahan kotak sebuah ayat, **bukan nomor blok hafalan**.

Dua kekurangan peta sumber diperbaiki setelah melihat gambar aslinya:

- Per Kata/Latin 10:95, halaman 219: kotak `[21,1572,242,1708]` dan `[21,1708,1023,1844]` (kiri, atas, kanan, bawah).
- Madinah 69:35, halaman 567: kotak `[12,1556,469,1684]`.

Sembilan kotak yang melampaui tepi dibatasi ke ukuran gambar. Perubahan hanya menyentuh koordinat, bukan PNG atau teks Arab. Hasil normalisasi mencakup seluruh 6.236 ayat pada enam edisi dengan peta. Rincian tersimpan di `sources/printed-mushaf-map-audit.json`.

**Hafalan 8 Blok tidak memiliki peta koordinat pada sumber publik.** Halamannya tetap memakai edisi Hafalan yang sebenarnya, tetapi tidak menampilkan sorotan/seleksi sentuh ayat di gambar. Pilihan surah/ayat, audio, terjemahan dan latihan tetap tersedia. Peta edisi lain tidak digunakan sebagai perkiraan. Sorotan pada gambar edisi lain mengikuti **ayat**; sorotan **kata** tetap tersedia pada rincian teks dengan pasangan rekaman/timing yang sudah ada.

## Perilaku pembaca

- Halaman penuh saat dibuka; ketuk untuk menampilkan/menyembunyikan header dan satu bar audio. Kontrol serta panel terjemahan mengambang di atas gambar, tanpa memperkecil gambar.
- Geser kanan/kiri untuk maju/mundur; ketuk dua kali untuk lanskap. Menu juga menyediakan navigasi, zoom dan orientasi.
- Tekan lama ayat pada enam edisi berpeta untuk memutar, menandai, menyalin, membagikan, atau membuka artinya. Koordinat gambar dan sorotan menggunakan transformasi yang sama pada potret, lanskap dan zoom.
- Warna asli tetap aktif sesuai pengaturan. Mematikan warna menampilkan grayscale tanpa mengubah berkas sumber. Legenda teks digital tidak dianggap sebagai legenda semua edisi cetak.
- Pengaturan murottal menerima surah/ayat awal dan akhir, termasuk lintas surah. Ulang tiap ayat dan putaran rentang dibedakan; loop berlangsung sampai dijeda. Urutan memakai nomor global kanonis tanpa menyisipkan ayat basmalah tambahan. Antrean dibatasi 12.500 item untuk menjaga memori.
- Panel terjemahan memakai sumber Nuraya: Kemenag RI melalui QuranEnc v1.0.1; Latin panel dari EQuran.id. Terjemahan tercetak pada gambar mengikuti sumber gambar, tidak disamakan dengan edisi panel.
- Tikrar dan Waqaf memakai halaman Indonesia Tajwid sebagai dasar latihan, dengan alat khususnya. Keduanya tidak diklaim sebagai dua edisi cetak tambahan. Per Ayat dan Tematik tetap pembaca teks/fungsi masing-masing. Bahriyah/pojok dan Video tetap dihapus.

## Penyimpanan dan reproduksi

Halaman 1, 2, 3, 7, 8 dan 604 setiap edisi dibundel (42 PNG). Halaman lain diunduh ketika dibuka. Cache terpisah, maksimum 64 MiB per edisi. Menu unduhan satu edisi menyimpan 604 halaman dengan progres. Dialog harus tetap terbuka; penghentian/gagal koneksi mempertahankan halaman yang selesai, dan unduhan dapat dilanjutkan. Halaman yang disimpan eksplisit bertahan di penyimpanan privat aplikasi. PNG transparan didekode ARGB_8888 di atas latar krem agar RGB di area transparan tidak muncul sebagai warna hijau.

Importer mengunci commit, memeriksa hash semua PNG dan peta, memastikan urutan ayat sama dengan korpus Nuraya, lalu menulis manifest, indeks, peta dan gambar bundel:

```sh
python3 scripts/import_printed_mushaf.py --cache /path/to/public-sources
```

Pemeriksaan aset paket tanpa jaringan/APK referensi: `python3 scripts/import_printed_mushaf.py --verify`.

Cache berisi pohon Git, `image-audit.json`, dan direktori `quran-image/`. Opsi `--reference-apk` hanya diperlukan untuk membentuk ulang indeks sumber; hasil indeks faktual sudah ada di repositori. `quran.json` tidak berubah (SHA-256 `f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c`).
