# Pencarian makna dan suara

## Cara menggunakan

Buka **Cari ayat & makna** dari Beranda, banner belajar, atau tombol **Cari ayat · makna & suara** di pilihan Quran. Ketik topik seperti “hati tenang mengingat Allah”, istilah tajwid, atau rujukan “Al-Baqarah ayat 255”. Pilih **Pencarian makna** atau **Kata kunci** lalu **Cari rujukan**. Setiap hasil mempunyai ayat, terjemahan, kutipan tafsir Kemenag bila tersedia, atribusi sumber, dan tombol membuka bacaan lengkap.

Untuk suara, pilih **Cari ayat lewat suara**, unduh model jika belum ada, dan izinkan mikrofon. Bacakan potongan ayat **bahasa Arab**, bukan pertanyaan bahasa Indonesia. Tekan **Selesai & cari ayat** setelah 5–20 detik. Hasil pengenalan masuk ke kolom pencarian agar bisa diperiksa/diedit. Rekaman berhenti otomatis pada 20 detik. Frasa yang muncul di beberapa ayat dapat menghasilkan beberapa rujukan.

## Cara kerja dan data

Pencarian makna memakai gabungan BM25, kecocokan langsung terjemahan, perluasan variasi kata, dan *latent semantic analysis* (LSA) 128 dimensi. LSA dilatih dari 6.236 terjemahan Indonesia/Inggris dan tafsir Kemenag yang sudah dibundel. Tidak menggunakan LLM, layanan API berbayar, akun, atau jawaban generatif. Skor adalah ukuran relevansi, bukan probabilitas kebenaran agama. Referensi bernomor dan pelajaran tajwid yang dikenali didahulukan.

Model `semantic_index.bin` berukuran sekitar 3,3 MB; proyeksi istilah dan vektor ayat dikuantisasi int8. [Metadata](../app/src/main/assets/semantic_index_sources.json) mencatat metode, seed, jumlah dimensi, checksum model, dan checksum 115 berkas sumber. Skrip pelatihan: [import_semantic_index.py](../scripts/import_semantic_index.py). Python memerlukan NumPy, SciPy, dan scikit-learn untuk pelatihan ulang; Android membaca model langsung tanpa dependensi ML baru.

```sh
python3 scripts/import_semantic_index.py --verify
# Latih ulang hanya setelah sumber yang sah diperbarui dan diaudit:
python3 scripts/import_semantic_index.py
```

Pencarian suara memakai `LocalQuranRecitationSession` dan model Whisper Quran yang sama dengan AI bacaan. Tidak perlu mengunduh model kedua. Kecocokan potongan dicari di seluruh ayat dengan penyelarasan kata, termasuk potongan di tengah ayat. Normalisasi ejaan hanya untuk pencarian; teks kanonis tidak diubah. Hasil dibatasi delapan rujukan, sehingga frasa berulang bisa mempunyai rujukan lain yang tidak tampil.

## Privasi dan batas

- Pertanyaan, rekaman, transkrip, dan hasil pencarian tidak dikirim ke server. Pengunduhan model memerlukan internet satu kali; jika model sudah siap, pencarian suara berjalan lokal.
- Izin mikrofon dan model yang siap wajib sebelum mulai. Pemutar audio dijeda saat merekam. Menutup panel, membatalkan, atau meninggalkan aplikasi membebaskan mikrofon dan membatalkan sesi.
- Kemampuan pengenalan bergantung pada perangkat, kebisingan, dan bacaan. Bacaan terlalu pendek, melintasi banyak ayat, atau berbeda jauh dari hasil pengenalan bisa tidak ditemukan. Cocokkan transkrip dan sumber sebelum memakai hasil.
- Relevansi makna terbatas pada korpus dan model statistik ini. Hasil kosong bukan berarti suatu topik tidak dibahas dalam Quran; hasil yang muncul belum tentu menjawab pertanyaan secara lengkap.
- Fitur ini bukan alat pentashihan, penilaian tajwid/makhraj, atau pemberi fatwa. AI bacaan dan alat belajar lain tetap tersedia seperti sebelumnya.
