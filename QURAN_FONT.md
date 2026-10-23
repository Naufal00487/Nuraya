# Huruf Al-Qur'an Utsmani

Nuraya menampilkan teks **Tanzil Uthmani** dengan **Amiri Quran**, font Naskh
yang dirancang untuk teks Al-Qur'an. Font dibundel di APK agar bentuk huruf,
harakat, dan tanda Al-Qur'an tetap tersedia tanpa internet dan tidak hanya
mengandalkan font bawaan setiap HP. Pemilihan font tidak mengubah karakter
teks sumber, penomoran ayat, atau terjemahan. Ini bukan klaim bahwa tata letak
halamannya identik dengan mushaf cetak Madinah/QPC.

## Sumber dan lisensi

- Proyek resmi: <https://github.com/aliftype/amiri>.
- Rilis upstream: `1.003`.
- Commit yang dipin: `480bb746e99ea700bb0d6b4dbf96302d58192103`.
- Berkas upstream:
  <https://raw.githubusercontent.com/aliftype/amiri/480bb746e99ea700bb0d6b4dbf96302d58192103/fonts/AmiriQuran.ttf>.
- Lokasi aplikasi: `app/src/main/res/font/amiri_quran.ttf`.
- Hak cipta: **Copyright 2010–2022 The Amiri Project Authors**.
- Lisensi: **SIL Open Font License 1.1**, disertakan lengkap sebagai
  `app/src/main/assets/licenses/AMIRI_OFL.txt`.
- Font disalin utuh tanpa perubahan glyph, subset, atau nama internal.

Metadata internal berkas Amiri Quran menyebut **Version 1.002**, walaupun
berkas ini memang terdapat dalam tag rilis upstream `1.003`. Kedua nomor
tersebut dicatat tanpa mengganti metadata font. Hash font dan lisensi tersimpan
di [`quran-font-source.json`](quran-font-source.json).

## Pemeriksaan

Font berhasil dibuka dengan `fontTools.ttLib.TTFont`. Cmap font mencakup seluruh
62 karakter berbeda dalam 6.236 teks Arab corpus Nuraya saat pemeriksaan.
Tabel OpenType `GSUB`, `GPOS`, dan `GDEF` tersedia untuk penyambungan huruf
serta penempatan tanda baca dan harakat. Pemeriksaan cakupan karakter tidak
menggantikan pemeriksaan tampilan pada HP; pembaca tetap menggunakan arah
kanan-ke-kiri dan ruang antarlajur yang memadai.

Font ini tidak memberikan hukum tajwid berwarna. Nuraya tidak mengarang
penandaan tajwid atau mengganti teks Al-Qur'an agar sesuai dengan suatu font.
