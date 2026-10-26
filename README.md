# Nuraya Quran speech model v1

Model suara offline untuk **Nuraya 1.2.0**. Aplikasi mengunduh file ini sekali, memeriksa ukuran dan SHA-256, lalu memproses suara pada HP tanpa mengunggah rekaman.

- File: `nuraya-quran-whisper-base-q5_1.bin`
- Ukuran: **59.707.625 byte** (59,7 MB)
- SHA-256: `1de4498dc16fdee2045e67a97d73cda4624017640dbe5ddf809f3130da7c234a`
- Sumber: [tarteel-ai/whisper-base-ar-quran](https://huggingface.co/tarteel-ai/whisper-base-ar-quran/tree/5c3c53fdf9272c4f6ee0bee09a1e5a4a615ee25c), revisi `5c3c53fdf9272c4f6ee0bee09a1e5a4a615ee25c`, lisensi Apache-2.0 menurut kartu model.
- Modifikasi Nuraya: konversi tensor ke format whisper.cpp v1.7.6 GGML F16 lalu kuantisasi Q5_1; konteks decoder448 berasal dari bentuk tensor, bukan opsi generasi max_length1024. Tidak melatih ulang bobot.
- Runtime/konversi whisper.cpp MIT, commit `a8d002cfd879315632a579e73f0148d06959de36`. Filter mel/tokenizer berasal dari sumber yang dipin dalam provenance. Teks lisensi dan notices disertakan.

**Batas:** model mengenali teks suara dan bisa salah, termasuk melengkapi kata yang tidak diucapkan. Model ini tidak menilai tajwid, makhraj, harakat, mad, atau ketepatan hafalan. Hasil aplikasi merupakan perkiraan untuk diperiksa, bukan sertifikasi bacaan benar. Kecepatan bergantung pada HP; tidak ada jaminan latensi real-time.

Tujuh pemeriksaan JNI pada host lulus: memuat model dan mengenali rekaman Quran nyata tanpa prompt ayat, senyap/input tidak valid, pembatalan antrean dan inferensi berjalan, pemakaian ulang setelah pembatalan, serta penutupan konteks. Log disertakan; ini bukan pengujian mikrofon atau kinerja Android fisik. Pengujian tambahan dan skrip reproduksi ada dalam source Nuraya1.2.0.

Hak/lisensi tetap pada pembuat masing-masing. Nuraya tidak menjual model atau mengklaim telah melatihnya. Model card asli dipertahankan di `upstream-model-card.md`; laporan WER pembuat bukan hasil evaluasi Nuraya.
