# Katalog audio Nuraya 1.1.0

Aplikasi menyertakan **138 pilihan rekaman lengkap 114 surah**. Angka ini menghitung edisi rekaman; beberapa edisi berasal dari qari yang sama. Nama berasal dari metadata Islamic Network. Seluruh 12 ID versi 1.0.1 dipertahankan agar favorit, daftar putar, dan unduhan lama tetap dapat dibuka.

## Rekaman surah

- Manifest HTTPS: <https://cdn.islamic.network/quran/info/by-surah/info.json>.
- Snapshot: `docs/sources/islamic-network-surah-manifest.json`, SHA-256 `6f183510806ab226e3df8be6675cd222e35209123f5153f51aa1b8b08005e25a`.
- Nama edisi: `https://api.alquran.cloud/v1/edition?format=audio`, hash snapshot tercatat pada `reciters.json`.
- Setiap edisi aktif mempunyai tepat `1.mp3` hingga `114.mp3` dalam manifest 128 kbps.
- Pemeriksaan awal mencakup 145 unduhan Al-Fatihah dan decode MP3 lengkap pada host. Tujuh sampel gagal, kemudian gagal pula pada pemeriksaan ulang. Ketujuh edisi baru tersebut dikeluarkan; **seluruh 138 sampel edisi yang disertakan lulus**.
- Hasil awal, pemeriksaan ulang, dan seleksi final dipertahankan dalam `artifacts/validation/audio-catalog-1.1.0.json`, `audio-catalog-recheck-1.1.0.json`, dan `audio-catalog-final-1.1.0.json`.

Edisi yang dikeluarkan: `ar.abdulbarimohammed`, `ar.abdulmohsenalharthy`, `ar.ezzatsabri`, `ar.khalifaaltunaiji`, `ar.mohamedabdelhakimsaadalabdullah`, `ar.saberabdulhakam`, `ar.yasseralmazroyee`. Ini berdasarkan sampel yang gagal saat diperiksa, bukan klaim bahwa semua rekaman mereka salah.

Rekaman tidak dibundel dalam APK. Streaming membutuhkan internet; rekaman surah tersedia offline setelah unduhan selesai. Sumber dan hak cipta tetap pada penerbit/pemegang hak.

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

- Seluruh 17 sampel file per ayat, serta dua sampel fallback anomali, berhasil diunduh dan didekode pada host (`verse-audio-1.1.0.json`).
- Rekaman pasangan QuranicAudio Al-Fatihah dan An-Nas diperiksa terpisah dengan unduhan lengkap dan decode host (`paired-audio-final-1.1.0.json`). Beberapa percobaan awal dibatasi proxy; hasil pemeriksaan awal dipertahankan dan tidak disamakan dengan hasil final.
- Integritas timestamp, posisi kata terhadap data studi, rentang klip, pengulangan antrean dan fallback diperiksa melalui script dan test JVM.
- **Belum diuji pada speaker/perangkat Android.** Pemeriksaan sampel tidak membuktikan seluruh ribuan file tersedia atau seluruh timestamp akurat secara pendengaran. Layanan pihak ketiga dapat berubah.

```sh
python3 scripts/verify_audio_catalog.py
python3 scripts/import_audio_timing.py --verify
# Opsional, mengunduh ulang satu sampel setiap edisi:
python3 scripts/verify_audio_catalog.py --online --report artifacts/validation/audio-catalog-new-check.json
```

ffprobe memakai `-select_streams a` dan ffmpeg memakai `-map 0:a:0`; gambar sampul MP3 tidak dianggap stream audio. Data asli Quran tetap byte-identik, dan semua anotasi sorotan hanya diterapkan pada teks yang cocok persis.
