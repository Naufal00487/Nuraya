# Pencarian kebutuhan belajar video

Pencarian berjalan lokal pada katalog rujukan video Al-Qur’an. Tidak memerlukan API key, biaya server, atau pengiriman pertanyaan ke pihak lain. Ini adalah pencarian dengan pemetaan kebutuhan belajar dan pemeringkatan; bukan LLM yang membuat jawaban, pencarian YouTube umum, atau bukti bahwa semua isi video sudah diperiksa.

## Perilaku

- Kalimat Indonesia/Inggris seperti **“saya belum bisa baca Quran”**, **“cara ngaji dari nol”**, dan **“Quran from scratch”** diarahkan ke materi huruf/harakat untuk pemula.
- Istilah sehari-hari dipetakan ke topik: **panjang pendek → mad**, **dengung → ghunnah**, **berhenti → waqf**, **hafalan/murajaah → memorization**. Tag topik berasal dari metadata katalog; pencarian tidak menambahkan klaim isi video baru.
- **“Nun mati ketemu ba”** dikenali sebagai kebutuhan iqlab. Karena katalog belum memiliki video khusus iqlab yang diverifikasi, aplikasi menjelaskan keterbatasan itu dan menunjukkan seri tajwid terkait. Ini bukan klaim bahwa episode yang tepat sudah ditemukan.
- **“Hafalan surat pendek”** mengarah ke materi hafalan, bukan otomatis ke video Shorts yang hanya berisi ringkasan surah. Antarmuka memberitahu bahwa hasil masih materi terkait.
- Judul, nama pembicara, dan nama surah tetap dapat dicari. Kecocokan judul/pembicara berbobot lebih tinggi daripada uraian. Filter tingkat dan format tetap berlaku; kata “pemula”, “lanjutan”, atau “Shorts” dapat membantu saat filter belum dipilih.
- Ejaan salah satu karakter, huruf berlebih/hilang, atau pertukaran dua huruf bersebelahan diperbaiki hanya jika ada satu kandidat yang jelas dalam kosakata. Potongan pendek tidak dibentangkan menjadi kata acak. Contoh: **tajwdi → tajwid**.
- Kata bermakna yang tidak dikenali tidak dibuang diam-diam: ia harus benar-benar ada di metadata hasil. Permintaan di luar konteks tidak dipindahkan ke pencarian web. Pencarian kosong/tidak cocok menyediakan contoh kebutuhan belajar yang dapat diketuk.

`VideoCatalog.searchWithGuidance(query, level, format, language)` mengembalikan `VideoSearchResults(items, guidance, suggestions, relatedOnly)`. API `search` lama tetap tersedia dan memakai peringkat yang sama. Antarmuka perlu menampilkan guidance agar hasil yang lebih umum tidak disangka video khusus yang telah diverifikasi.

## Batas

Katalog saat ini tetap terbatas pada referensi yang sudah tersedia. Pencarian tidak menebak ID video, judul, bahasa, atau tanggal dari model. Metadata dan batas verifikasi sumber dicatat di [VIDEO_SOURCES.md](VIDEO_SOURCES.md). Kelengkapan hasil akan bertambah hanya jika referensi yang benar ditambahkan. Mengubah kata kunci tidak dapat menjamin tersedianya materi khusus atau berbahasa Indonesia.

## Verifikasi

`VideoSmartSearchTest` memeriksa kebutuhan pemula dalam dua bahasa, padanan istilah, pengenalan iqlab dengan penjelasan hasil terkait, hafalan surat pendek, koreksi typo, nama pembicara/surah, filter, dan penolakan permintaan di luar konteks. `VideoCatalogueTest` tetap memeriksa integritas katalog/sumber.
