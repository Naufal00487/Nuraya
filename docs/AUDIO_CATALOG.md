# Katalog audio Nuraya 1.3.1

Aplikasi mendukung **138 ID edisi rekaman lengkap 114 surah**. Daftar menampilkan **137 pilihan** karena dua alias Alafasy diringkas menjadi satu kartu, sementara seluruh ID tetap tersedia untuk favorit, daftar putar dan unduhan lama. Angka tersebut menghitung edisi rekaman, bukan jumlah orang; beberapa edisi berasal dari qari yang sama. Nama berasal dari metadata Islamic Network.

## Rekaman surah

- Manifest HTTPS: <https://cdn.islamic.network/quran/info/by-surah/info.json>.
- Snapshot: `docs/sources/islamic-network-surah-manifest.json`, SHA-256 `6f183510806ab226e3df8be6675cd222e35209123f5153f51aa1b8b08005e25a`.
- Nama edisi: `https://api.alquran.cloud/v1/edition?format=audio`, hash snapshot tercatat pada `reciters.json`.
- Setiap edisi aktif mempunyai tepat `1.mp3` hingga `114.mp3` dalam manifest 128 kbps.
- Pada 4 November 2026, **138 sampel Al-Fatihah** dari seluruh edisi yang disertakan berhasil diunduh lengkap dan didekode. [Bukti sampel](audit/1.3.1/audio-full-samples.json).
- [Audit identitas sumber](audit/1.3.1/audio-identity.md) mencatat pemetaan ID, ejaan nama, ragam riwayat dan sampel identik. Pemetaan metadata tidak membuktikan autentikasi setiap suara melalui pendengaran manusia.

Rekaman murottal tidak dibundel dalam APK. Rekaman azan pengingat 1.4.0 disertakan offline; lihat [PRAYER.md](PRAYER.md). Streaming membutuhkan internet; rekaman surah tersedia offline setelah unduhan selesai. Sumber dan hak cipta tetap pada penerbit/pemegang hak.

## Sinkronisasi ayat dan kata

Pembaca menyediakan **17 qari per ayat**, dengan setiap edisi mempunyai 6.236 file pada manifest penyedia. Metadata kecil dan katalog disimpan offline di `verse_reciters.json`. Pemutar memakai sesi media Android yang sama dengan murottal; tidak ada pemutar kedua yang berbunyi bersamaan. Rentang hafalan mengulang ayat dalam antrean yang sebenarnya, bukan dengan animasi perkiraan durasi.

Alafasy memakai data resmi Quran.com, recitation ID 7, dengan **URL rekaman QuranicAudio yang dipasangkan oleh sumber tersebut**. Timestamp tidak diterapkan ke file qari atau rekaman lain. Data 114 surah mencakup 6.236 ayat dan 80.288 segmen sumber:

- **6.142 ayat** mempunyai rentang yang lolos pemeriksaan untuk sorotan kata.
- **94 ayat** menggunakan file individual Alafasy dan sorotan per ayat. Data dengan urutan/overlap atau batas waktu tidak aman tidak dipakai untuk memotong rekaman.
- Segmen kata yang tidak tersedia menghasilkan posisi 0 (tanpa sorotan kata); tidak ada pembagian waktu berdasarkan jumlah kata.
- Kata 11:13 posisi 3 tetap tidak disorot karena ejaan sumber tidak cocok persis dengan teks kanonis. Posisi kata selanjutnya tidak digeser.
- Batas klip mencakup awal/akhir segmen sumber yang valid, sehingga kata pertama tidak terpotong oleh perbedaan puluhan milidetik pada metadata ayat.
- Kasus sumber bermasalah termasuk 14:17, ketidakcocokan posisi 37:130, overlap antarayat, dan overlap posisi kata berbeda. Pemeriksaan menolak kasus ini secara konservatif.

Qari lainnya menggunakan pergantian file ayat yang nyata dan sorotan ayat. Antarmuka menunjukkan kemampuan qari dan jenis sorotan pada ayat yang sedang diputar. Mode sinkron membutuhkan internet dan tidak memakai unduhan surah lama sebagai pengganti file pasangan timestamp.

## Validasi dan batasnya

- Pada 5 November 2026, **22 sampel audio** berhasil diunduh lengkap dan didekode: 17 ayat 1:1, tiga surah dengan timing pasangan, serta dua ayat fallback qari yang sama. [Bukti sampel ayat](audit/1.3.1/verse-audio-samples.json).
- ID qari yang tidak tersedia ditolak tanpa memilih suara lain. Riwayat audio ayat mempertahankan qari, surah dan ayat yang tepat.
- Integritas timestamp, posisi kata terhadap data studi, rentang klip, pengulangan antrean dan fallback diperiksa melalui script dan test JVM.
- **Belum diuji pada speaker/perangkat Android.** Pemeriksaan sampel tidak membuktikan seluruh ribuan file tersedia atau seluruh timestamp akurat secara pendengaran. Layanan pihak ketiga dapat berubah.

```sh
python3 scripts/verify_audio_catalog.py
python3 scripts/import_audio_timing.py --verify
# Opsional, mengunduh ulang satu sampel setiap edisi:
python3 scripts/verify_audio_catalog.py --online --report artifacts/validation/audio-catalog-new-check.json
```

ffprobe memakai `-select_streams a` dan ffmpeg memakai `-map 0:a:0`; gambar sampul MP3 tidak dianggap stream audio. Data asli Quran tetap byte-identik, dan semua anotasi sorotan hanya diterapkan pada teks yang cocok persis.

## Pengulangan hafalan 1.4.0

Di Latihan → Hafalan, pengguna memilih rentang awal/akhir, jumlah 1–20 dan mode Per ayat atau Seluruh rentang. Pada Per ayat, 1–2 ×3 berarti `1,1,1,2,2,2`; pada Seluruh rentang, 1–5 ×3 berarti `1,2,3,4,5,1,2,3,4,5,1,2,3,4,5`. Antrean berakhir setelah jumlah tersebut, tanpa loop tak terbatas. Tombol seluruh surah memilih ayat 1 hingga ayat terakhir. Pemutar menampilkan putaran ketika rentang diulang, lalu menghapus status itu ketika beralih ke murottal surah.
