# Teman belajar lokal

Menu **Teman belajar** kini dapat digunakan tanpa server, akun, token API, atau koneksi internet. Pengguna dapat menulis pertanyaan dengan bahasa sehari-hari, istilah tajwid, nama surah/nomor ayat, atau topik. Hasil berupa pelajaran yang sudah disusun dan kutipan sumber yang mungkin relevan. **Fitur ini adalah pencarian sumber lokal, bukan percakapan AI generatif atau penerbit fatwa.** AI pengenalan bacaan merupakan fitur terpisah.

## Isi dan perilaku

- Pencarian mencakup terjemahan Indonesia dan Inggris dari seluruh **6.236 ayat**, serta **tafsir Kemenag Indonesia seluruh ayat** yang sudah dibundel. Tidak diperlukan unduhan tambahan untuk indeks ini.
- Indeks menimbang kelangkaan kata, frekuensi, dan panjang dokumen; kecocokan pada terjemahan ayat memperoleh prioritas tambahan. Beberapa istilah umum/ejaan diperluas secara eksplisit, misalnya `sholat` → `salat`, `kesabaran` → `sabar`, serta pasangan kata Inggris yang relevan. Perluasan ini tidak diklaim sebagai model bahasa.
- Pertanyaan seperti `hukum nun mati ketemu ba` diarahkan ke materi iqlab yang memang tersedia. Materi tetap memakai ringkasan editorial serta sumber About Tajweed yang sudah dikurasi; jawaban agama baru tidak dibuat saat pencarian.
- Rujukan langsung seperti `2:255`, `Al-Fatihah ayat 3`, dan `ayat kursi` menggunakan nomor sumber yang sebenarnya. Nomor tidak valid menghasilkan pesan yang jelas, tanpa penggantian ke ayat lain.
- Hasil menampilkan teks Arab kanonis, terjemahan, kutipan tafsir, atribusi, dan tombol untuk membuka ayat, tafsir lengkap, serta audio pada pembaca.
- Kutipan tafsir merupakan **substring persis dari sumber**, maksimum 900 karakter. Tampilan menyatakan bila hanya sebagian teks yang ditampilkan. Teks tidak diringkas ulang oleh model; kutipan tetap berbahasa Indonesia pada UI Inggris.
- Pertanyaan yang jelas di luar pembelajaran ditolak; pencarian yang tidak cocok menghasilkan saran memperpendek topik atau memakai nomor ayat. Hasil kosong tidak diklaim sebagai bukti bahwa Al-Qur’an tidak membahas topik tersebut.
- Pertanyaan dan hasil tidak dikirim atau dimasukkan ke riwayat percakapan. Teks masukan dapat dipertahankan sementara oleh state layar Android saat layar dibuat ulang. Layar menegaskan bahwa kecocokan pencarian belum tentu menjawab seluruh pertanyaan.

## Data dan implementasi

`LocalLearningRepository.kt` membaca `assets/learning_index.bin`, format `NLI1`, dengan batas jumlah dokumen, istilah, panjang istilah dan posting. Indeks berisi 26.172 istilah dan berukuran sekitar 2,9 MB sebelum kompresi APK. Ia memuat nomor ayat/frekuensi dan panjang dokumen; sumber yang ditampilkan selalu diambil dari `QuranRepository` dan `StudyRepository`, bukan direkonstruksi dari indeks. Tidak ada salinan penuh tafsir kedua yang dipertahankan di memori. Repository studi menggunakan cache tiga surah. Pencarian/IO berjalan pada `Dispatchers.IO`, dengan pemeriksaan pembatalan.

`assets/learning_index_sources.json` mencatat checksum Quran dan seluruh 114 berkas studi yang digunakan. Indeks dapat dibuat ulang secara deterministik tanpa jaringan:

```sh
python3 scripts/import_learning_index.py
python3 scripts/import_learning_index.py --verify
```

Sumber tetap tunduk pada atribusi/hak yang dicatat di [DATA_SOURCES.md](DATA_SOURCES.md), [STUDY_DATA.md](STUDY_DATA.md), dan [PRACTICE.md](PRACTICE.md). Pengindeksan tidak mengubah teks aslinya.

Backend `LearningRepository` dan `backend/` terdahulu tetap sebagai integrasi opsional untuk pengembangan; tidak dipanggil oleh layar Teman belajar lokal. Konfigurasi backend kosong tidak lagi menjadikan layar utama ini tidak berfungsi. Tidak ada layanan AI berbayar atau endpoint publik tidak resmi yang diaktifkan.

## Pemeriksaan

`LocalLearningRepositoryTest` mencakup rujukan numerik/nama surah, pembuka ayat sebenarnya, pertanyaan tajwid sehari-hari, sinonim pencarian, penolakan konteks lain, ketahanan terhadap instruksi untuk memalsukan kutipan, serta kecocokan kutipan persis dengan sumber. Verifier Python membandingkan ulang seluruh indeks dan hash dengan sumber terbundel. Hasil eksekusi test Android dicatat dalam laporan validasi rilis; keberadaan test bukan klaim bahwa pengujian perangkat telah dilakukan.
