# Pencarian surah, ayat, makna, dan suara

## Cara menggunakan

Buka **Cari Ayat** di Beranda atau **Cari ayat · makna & suara** di pilihan Quran. Ketik nama surah (`Yasin`, `Albaqarah`), rujukan (`2:255`), atau topik (`ayat tentang sabar`, `hukum nun mati ketemu ba`). Pilih **Pencarian makna** atau **Kata kunci**, lalu **Cari rujukan**. Hasil surah membuka awal surah yang dipilih; hasil ayat menyediakan teks sumber, terjemahan, kutipan tafsir Kemenag bila tersedia, dan tombol membuka bacaan.

Untuk dikte, pilih **Cari dengan suara · bahasa Indonesia**, izinkan mikrofon, lalu **Mulai bicara**. Ucapkan misalnya “cari surat Yasin”, “surah enam puluh tujuh”, atau “Al-Baqarah ayat dua ratus lima puluh lima”. Tekan **Selesai** bila pengenal belum berhenti sendiri. Transkrip masuk ke kolom pencarian yang sama dengan ketikan dan bisa diedit. Pencarian ini tidak meminta bacaan Arab atau model AI Quran. AI koreksi bacaan tetap berada di menu **AI bacaan**.

## Cara kerja dan data

`QuerySpeechSession` meminta bahasa `id-ID`, model dikte bebas, hasil sementara dan hingga lima alternatif kepada Android SpeechRecognizer. Pengenal di perangkat dipilih jika tersedia; jika tidak, layanan suara Android digunakan. Ketika pengenal lokal tidak mempunyai bahasa Indonesia, pengguna dapat memilih **Coba layanan suara Android**. Sesi dibatasi 30 detik; setelah selesai berbicara, aplikasi menunggu hasil maksimal 15 detik. Membatalkan, menutup panel, atau meninggalkan aplikasi melepas pengenal dan mengabaikan callback terlambat.

`QuranReferenceSearch` menggabungkan 114 nama sumber dengan alias Indonesia, awalan Al/An/Asy dan variasi transliterasi. Nomor yang diucapkan sebagai bilangan atau digit dibaca sebelum pemeringkatan topik. Nomor di luar batas ditolak. Ejaan yang mendekati nama surah menghasilkan pilihan berlabel **Kemungkinan rujukan**; aplikasi tidak langsung membuka tebakan. Pertanyaan topik yang jelas tidak diganti oleh alternatif nama surah dengan keyakinan pengenal lebih rendah. Normalisasi hanya untuk pencarian, tidak mengubah teks Quran.

Pencarian makna menggabungkan BM25, kecocokan terjemahan, perluasan variasi kata, dan *latent semantic analysis* (LSA) 128 dimensi. LSA dilatih dari 6.236 terjemahan Indonesia/Inggris dan tafsir Kemenag yang dibundel. Tidak memakai LLM atau jawaban generatif. Skor menunjukkan relevansi, bukan probabilitas kebenaran agama. Referensi bernomor, nama surah dan pelajaran tajwid yang dikenali didahulukan.

Model `semantic_index.bin` sekitar 3,3 MB memakai proyeksi istilah dan vektor int8. [Metadata](../app/src/main/assets/semantic_index_sources.json) mencatat metode, seed, checksum model dan 115 berkas sumber. [Skrip](../scripts/import_semantic_index.py) memerlukan NumPy, SciPy dan scikit-learn hanya untuk pelatihan ulang; Android membaca model langsung.

```sh
python3 scripts/import_semantic_index.py --verify
```

Teks Arab yang diketik tetap dapat dicari dengan pencocokan potongan ayat. Ini bukan mode bahasa untuk mikrofon pencarian Indonesia.

## Privasi dan batas

- Pencarian terhadap ayat, tafsir dan pelajaran dilakukan di perangkat. Nuraya tidak menyimpan rekaman atau transkrip pencarian ke riwayat/database atau mengirimkannya ke server Nuraya.
- Dikte memakai layanan Android, bukan model Whisper Quran. Jika pengenal Indonesia offline tidak tersedia, layanan perangkat dapat membutuhkan internet dan memproses audio melalui penyedia suara. Penjelasan ini tampil sebelum tombol mikrofon. Ketersediaan bahasa/provider bergantung pada HP.
- Izin mikrofon diperlukan. Pemutar dijeda selama dikte. Tidak ada perekaman latar belakang.
- Kesalahan transkripsi tetap mungkin; pengguna dapat mengedit hasil. Pencocokan nama tidak mencakup semua salah dengar, dan pencarian topik tidak menjamin menjawab pertanyaan secara lengkap.
- Pengujian otomatis memverifikasi bahasa intent, alternatif, pembatalan, timeout, rujukan seluruh surah, dan alur antarmuka. Akurasi mikrofon/pengenal perangkat fisik belum diverifikasi dalam rilis ini.
