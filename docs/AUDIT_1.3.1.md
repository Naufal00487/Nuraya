# Audit Nuraya 1.3.1

Audit memeriksa data yang benar-benar disertakan dalam APK, sumber yang dipatok, pemetaan audio dan perilaku aplikasi. Pemeriksaan otomatis tidak menggantikan tashih ahli Al-Qur’an atau pemeriksaan seluruh rekaman oleh pendengar yang mengenali qarinya.

## Ayat dan penomoran

**114 surah dan 6.236 ayat**, tanpa referensi hilang, ID ayat ganda atau nomor ayat global ganda. Seluruh teks Arab, terjemahan Indonesia, terjemahan Inggris dan catatan Indonesia cocok persis dengan snapshot sumber impor. SHA-256 `quran.json` tetap `f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c`.

Pembuka basmalah 112 surah dipisahkan tanpa nomor; Al-Fatihah 1:1 tetap basmalah, At-Tawbah tidak ditambah pembuka, dan An-Naml 27:30 tetap utuh. Seluruh referensi ayat cocok dengan indeks 604 halaman Madinah.

Perbandingan sumber kedua menyisakan perbedaan ejaan pada **12:39 dan 12:41**. Teks Tanzil yang dipatok dipertahankan; tanda waqaf tambahan pada kedua ayat tidak ditebak. Audit ini tidak menetapkan sumber mana yang benar secara ilmiah. [Laporan corpus](audit/1.3.1/quran-corpus.md) dan [data lengkap](audit/1.3.1/quran-corpus.json).

## Bantuan cara membaca

Latin 6.236 ayat cocok persis dengan `teksLatin` pada snapshot eQuran.id. Tafsir tersedia untuk 6.236 ayat. Terdapat 60.023 rentang warna tajwid; tidak ditemukan rentang ganda atau nama aturan warna yang tidak dikenal. Dari 77.429 kata sumber, 77.428 dapat dipetakan tepat ke teks kanonis.

Pengecualian yang tetap dicatat: satu kata pada **11:13, posisi 3**, mempunyai ejaan berbeda sehingga tidak diberi sorotan dugaan; 34 rentang warna sumber yang melintasi perbedaan teks juga tidak dipaksakan. Terdapat 4.362 tanda waqaf pada 2.639 ayat, tanpa anotasi ganda. Penjelasan hamzah wasal dan mu‘anaqah diperbaiki sebagaimana [catatan perubahan](UPDATE_1.3.1.md).

Kesetiaan terhadap sumber tidak menjamin seluruh petunjuk pelafalan benar secara pedagogis. Latin tetap bantuan; AI membandingkan perkiraan kata dan **belum merupakan penilai tajwid/makhraj yang tervalidasi**. [Hasil pemeriksaan bantuan baca](audit/1.3.1/reading-guidance.json).

## Qari dan suara

Pemetaan ID ke sumber rekaman diperiksa. Bug penggantian qari yang tidak dikenal ke Alafasy telah diperbaiki; pilihan yang tidak tersedia sekarang meminta pengguna memilih ulang. Pemutaran riwayat per ayat juga mempertahankan ID qari serta referensi ayat yang tepat.

**138 sampel lengkap Al-Fatihah** berhasil diunduh dan didekode. **22 pemeriksaan audio ayat/timing/fallback** lulus. Sampel tersebut bukan pemeriksaan semua 15.732 berkas surah, dan tidak merupakan pengenalan identitas suara oleh manusia. Foto hanya diberikan untuk pemetaan identitas yang sudah dicatat; qari lain memakai inisial. [Laporan pemetaan dan identitas](audit/1.3.1/audio-identity.md), [sampel surah](audit/1.3.1/audio-full-samples.json), [sampel ayat](audit/1.3.1/verse-audio-samples.json).

## Duplikasi

- Tidak ada ID ayat/nomor global ganda. Ada 94 kelompok teks berulang pada 272 referensi yang sah dan cocok sumber; pengulangan ayat tersebut tidak dihapus.
- Dua alias Alafasy mempunyai sampel Al-Fatihah identik. Daftar utama kini menampilkan satu kartu; kedua ID tetap bisa membuka data lama. Ragam rekaman murattal/mujawwad atau riwayat yang berbeda tetap diberi label dan tidak disatukan sembarangan.
- Database menggunakan ID unik untuk koleksi dan unduhan. Tes menyimpan riwayat ayat yang sama 20 kali, membuka ulang database, dan memastikan entri yang sama hanya diperbarui. Referensi lain tetap terpisah. Penghapusan riwayat dilakukan hanya atas tindakan pengguna dan tidak menghapus penanda/favorit.

## Menjalankan ulang

```sh
python3 scripts/audit-quran-corpus.py
python3 scripts/audit-reading-guidance.py
python3 scripts/audit-audio-identity.py
python3 scripts/verify_audio_catalog.py
```

Snapshot dan checksum disertakan agar hasil dapat diperiksa. Pemeriksaan jaringan yang lengkap memerlukan `verify_audio_catalog.py --online` dan `verify_verse_audio.py --online`. [Bukti build dan tes](BUILD_VALIDATION.md) membedakan tes host dari pengujian HP fisik; GPS, mikrofon dan identitas suara seluruh qari belum diuji pada HP fisik dalam rilis ini.
