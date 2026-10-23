# Sumber data Nuraya

Nuraya menyertakan teks Al-Qur’an agar pembaca dan pencarian dapat digunakan
tanpa jaringan. Audio tidak disertakan dalam APK: rekaman diputar dari sumbernya
atau diunduh secara eksplisit oleh pengguna.

## Snapshot yang digunakan

- Repositori perantara: [Risan — Quran JSON](https://github.com/risan/quran-json).
- Commit tetap: [`6523a9ad3b73bb7e8cb3a9cdf0ea774b8bab3123`](https://github.com/risan/quran-json/tree/6523a9ad3b73bb7e8cb3a9cdf0ea774b8bab3123).
- Importer: [`scripts/import_quran.py`](../scripts/import_quran.py).
- Hash setiap berkas sumber: [`quran-source-checksums.json`](quran-source-checksums.json).
- Hasil: `app/src/main/assets/quran.json`, 4.099.885 byte.
- SHA-256 hasil: `f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c`.
- Terdapat 114 surah, 6.236 ayat, dan indeks 30 juz.

Importer memeriksa hash snapshot sumber, nomor dan urutan surah/ayat, kesesuaian
jumlah ayat pada ketiga bahasa, serta pembagian juz. Importer menyalin teks,
tanda baca, dan catatan terjemahan dari snapshot. Normalisasi di aplikasi hanya
digunakan pada indeks pencarian; teks yang ditampilkan tetap berasal dari sumber.
Pemeriksaan struktural ini bukan pengganti pemeriksaan ilmiah oleh ahli Al-Qur’an.

## Teks Arab dan metadata

Sumber: **Tanzil Project**, teks Uthmani; metadata surah dan juz Tanzil.

- Teks: <https://tanzil.net/pub/download/index.php?quranType=uthmani&outType=txt-2&agree=true>
- Metadata: <https://tanzil.net/res/text/metadata/quran-data.xml>
- Ketentuan: <https://tanzil.net/docs/text_license>
- Lisensi: **Creative Commons Attribution 3.0**, disertai ketentuan Tanzil bahwa
  teks hanya boleh disalin dan didistribusikan secara verbatim, tanpa perubahan.

Atribusi Tanzil, tautan sumber, dan pemberitahuan hak cipta disimpan di metadata
dataset dan `app/src/main/assets/licenses/TANZIL_NOTICE.txt`. Pemberitahuan ini
harus dipertahankan ketika teks disalin atau didistribusikan kembali. Kunjungi
<https://tanzil.net> dan <https://tanzil.net/updates/> untuk pembaruan sumber.

Penomoran mengikuti snapshot Hafs/Tanzil. Basmalah yang sudah ada pada teks
sumber tidak ditambahkan lagi sebagai ayat buatan.

## Terjemahan Bahasa Indonesia

Edisi: **Kementerian Agama Republik Indonesia — QuranEnc**,
`indonesian_affairs`, versi **1.0.1**. Metadata QuranEnc menjelaskan bahwa edisi
ini dikembangkan di bawah pengawasan Rowwad Translation Center.

- Baca sumber: <https://quranenc.com/en/browse/indonesian_affairs/>
- Unduhan sumber: <https://quranenc.com/downloads/sqlite/indonesian_affairs.zip>
- Ketentuan penggunaan: <https://quranenc.com/en/home/api>

Snapshot metadata sumber mencatat izin mengunduh dan menerbitkan ulang dengan
tujuh syarat: tidak mengubah/menambah/menghapus isi; mencantumkan penerbit dan
QuranEnc; mencantumkan versi; mempertahankan informasi naskah; melaporkan catatan
kepada penyedia; mengikuti versi terbaru; dan tidak menyertakan iklan yang tidak
pantas. Teks dan catatan pada edisi yang dibundel dipertahankan. Nuraya tidak
memuat iklan. Pemelihara perlu memeriksa versi terbaru sebelum menerbitkan
pembaruan aplikasi dan menyampaikan koreksi kepada QuranEnc, bukan mengubah
teks agama secara mandiri.

Catatan kaki ditampilkan sebagai **catatan terjemahan**, bukan tafsir lengkap.
Versi 1.0.1 adalah versi snapshot yang diverifikasi; pengujian ini tidak
menyatakan bahwa edisi tersebut adalah versi terbaru di server QuranEnc.

## Terjemahan Bahasa Inggris

Edisi: **Marmaduke Pickthall — The Meaning of the Glorious Koran (1930)**.

- Snapshot digital asal:
  <https://cdn.jsdelivr.net/gh/fawazahmed0/quran-api@1/editions/eng-mohammedmarmadu.json>
- Berkas perantara yang dipin: `data/extra/english_pickthall.json`.

Metadata snapshot menyatakan karya 1930 ini berada dalam domain publik:
Pickthall wafat pada 1936, sehingga masa hidup ditambah 70 tahun telah berakhir;
karya terbit 1930 memasuki domain publik di Amerika Serikat pada 1 Januari 2026.
Pernyataan ini berlaku pada karya terjemahan asli dan tidak berarti semua edisi,
anotasi modern, atau hak di setiap wilayah mempunyai status yang sama. Nuraya
tetap mencantumkan nama penerjemah dan tahun edisi.

## Lisensi kompilasi data

Repositori Quran JSON memberi kompilasi data lisensi **CC BY-SA 4.0**.
Teks lisensi lengkap disertakan di
`app/src/main/assets/licenses/QURAN_DATA_CC_BY_SA_4.0.txt`.

Nuraya mengubah struktur penyimpanan JSON menjadi daftar surah dan ayat yang
digabungkan, menambahkan nomor ayat global dan penanda juz dari indeks Tanzil,
serta memilih tiga qari dari metadata audio. Teks sumber tidak diparafrasekan.
Komponen kompilasi data turunan ini disediakan dengan CC BY-SA 4.0, sambil
mempertahankan ketentuan tiap sumber. Lisensi kompilasi ini tidak menghapus
ketentuan Tanzil, QuranEnc, atau hak pemilik rekaman, dan tidak secara otomatis
menentukan lisensi seluruh kode aplikasi.

## Murottal

Sumber rekaman: **Islamic Network / Al Quran Cloud**, 128 kbps, per surah.
Ketiga pilihan berikut tercantum dengan 114 surah pada snapshot manifest
resmi `https://cdn.islamic.network/quran/info/by-surah/info.json`:

| ID sumber | Qari |
| --- | --- |
| `ar.alafasy` | Mishary Rashid Alafasy |
| `ar.abdulbasitmurattal` | Abdul Basit Abdus Samad |
| `ar.saudalshuraim` | Saud Al-Shuraim |

Pola URL: `https://cdn.islamic.network/quran/audio-surah/128/{id}/{surah}.mp3`.
Nomor surah pada URL tidak diberi nol di depan.

- Dokumentasi CDN: <https://alquran.cloud/cdn>
- Ketentuan: <https://alquran.cloud/terms-and-conditions>

Ketentuan penyedia yang dibaca saat validasi menyatakan bahwa rekaman
dilisensikan untuk distribusi gratis nonkomersial pada bitrate yang disediakan;
streaming, penyematan, dan unduhan diperbolehkan untuk penggunaan pribadi dan
pendidikan. Hak cipta tetap berada pada qari atau pemilik haknya. Nuraya versi
ini gratis, tanpa iklan atau pembayaran. Jangan menganggap ketentuan ini memberi
izin yang sama untuk setiap penggunaan komersial atau rekaman dari sumber lain.

Pada pemeriksaan konektivitas terakhir, HTTPS HEAD untuk Alafasy, surah 1,
menghasilkan **HTTP 200**, `Content-Type: audio/mpeg`, dan
`Content-Length: 1420155`. Pemeriksaan ini menggunakan verifikasi TLS biasa.
Penolakan proxy 403 pada pemeriksaan sebelumnya sudah tidak terjadi untuk
permintaan tersebut. HEAD membuktikan ketersediaan metadata berkas saat diuji;
hasil pemutaran, unduhan lengkap, dan pengujian Android dicatat terpisah dalam
laporan build/pengujian. Tidak ada klaim bahwa semua 342 berkas telah diuji atau
diunduh. Hanya unduhan yang selesai dan masih ada di penyimpanan aplikasi yang
dapat diputar offline.

## Tafsir, transliterasi, dan tajwid

- Tafsir **tidak dibundel**. Tombol tafsir membuka Quran.com, dengan rujukan
  surah/ayat dan identitas **Ibn Kathir, edisi Bahasa Inggris**. Tautan ini
  membutuhkan browser dan jaringan; Nuraya tidak menyalin kontennya atau
  menganggapnya tersedia offline.
- Transliterasi ayat tidak disertakan karena sumber yang tersedia mencatat
  hasil generasi yang belum diperiksa pembaca berkualifikasi. Nama surah Latin
  tetap menggunakan metadata sumber.
- Penanda warna tajwid tidak dibuat tanpa data tajwid terverifikasi.
- Video dan jawaban AI tidak ditambahkan sebagai sumber agama resmi. Lihat
  [integrasi AI dan video](AI_AND_VIDEO.md) untuk status dan mekanismenya.

## Menjalankan pemeriksaan kembali

Dari direktori proyek, validasi lokal tanpa jaringan:

```sh
python3 scripts/import_quran.py --verify
```

Perintah tersebut sudah dijalankan dan berhasil: 114 surah, 6.236 ayat sejajar
pada tiga bahasa, 30 juz, dan hash aset cocok. Untuk membangun ulang dari
snapshot tetap dengan verifikasi SHA-256 dan TLS:

```sh
python3 scripts/import_quran.py
```

Atau gunakan checkout sumber pada commit yang sama:

```sh
python3 scripts/import_quran.py --source-dir /path/to/quran-json
```

Jangan mengubah hash yang diharapkan hanya agar import lolos. Pembaruan edisi
harus memeriksa perubahan teks, izin, atribusi, dan keselarasan ayat terlebih
dahulu. Pengambilan langsung laman Tanzil dan QuranEnc dari mesin cloud masih
ditolak proxy saat audit ini; dokumentasi ketentuannya didasarkan pada snapshot
provenance yang dipin. Laman ketentuan Al Quran Cloud dapat diakses dan dibaca
langsung dengan HTTP 200.
