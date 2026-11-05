# Bantuan Latin Al-Qur’an

Mode **Latin & tajwid** memakai transliterasi Indonesia yang diterbitkan **EQuran.id API v2**. Aset offline mencakup 114 surah dan 6.236 ayat. Setiap bacaan dihubungkan menggunakan nomor surah dan nomor ayat; teks Arab kanonis tidak diubah. Basmalah tetap ayat 1 pada Al-Fatihah, bukan pengganti ayat pertama surah lain.

Transliterasi Latin adalah bantuan mengikuti bunyi. Warna hukum tajwid ditampilkan pada teks Arab dengan anotasi sumber yang telah ada. Aplikasi tidak mengarang pemetaan huruf Latin ke hukum tajwid dan tidak mengklaim bahwa huruf Latin berwarna atau mewakili makhraj dengan lengkap. Audio qari dan bimbingan guru tetap menjadi rujukan cara pengucapan.

## Asal dan integritas data

- Dokumentasi sumber: <https://equran.id/apidev/v2>.
- Respons setiap surah: `https://equran.id/api/v2/surat/{nomor}`; field `data.ayat[].teksLatin`.
- Snapshot respons asli yang dikompresi: `docs/sources/latin/{1..114}.json.gz`.
- URL, tanggal pengambilan, ukuran respons dan SHA-256 setiap snapshot: `docs/sources/latin/manifest.json`.
- Aset aplikasi: `app/src/main/assets/latin/{1..114}.json` dan `index.json`.
- Teks ditampilkan sesuai sumber setelah spasi awal/akhir dibuang. Tidak dihasilkan AI, ditransliterasikan ulang, atau dicocokkan dengan nomor ayat lain.

Importer memeriksa status API, identitas surah, jumlah ayat, urutan setiap nomor ayat, ketiadaan duplikasi dan kelengkapan seluruh 6.236 rujukan terhadap korpus aplikasi. Pemeriksaan tersebut membuktikan integritas penyalinan dan pemetaan rujukan; bukan sertifikasi pengucapan. SHA-256 `quran.json` tetap `f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c`.

## Pengulangan proses

```sh
# Verifikasi offline terhadap snapshot yang sudah disimpan:
python3 scripts/import_latin.py --verify
# Bangun ulang aset dari snapshot:
python3 scripts/import_latin.py
# Ambil versi sumber baru secara eksplisit, empat unduhan bersamaan:
python3 scripts/import_latin.py --refresh
```

`LatinRepositoryTest` memeriksa semua rujukan, kebijakan pembuka surah, serta penolakan rujukan di luar rentang. `LatinReaderPresentationTest` memeriksa panel memakai aset Latin offline ketika model ayat kanonis tidak memiliki field transliterasi. Hasil Gradle akhir dicatat dalam `BUILD_VALIDATION.md`.
