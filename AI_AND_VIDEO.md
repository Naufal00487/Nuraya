# AI, sumber belajar, dan video — Nuraya 1.2.2

## AI bacaan lokal

Layar **AI bacaan** memakai mesin Whisper native di Android. Model Tarteel Whisper Quran berukuran sekitar **59,7 MB**, diunduh sekali, diperiksa ukuran dan SHA-256, lalu disimpan pada penyimpanan privat aplikasi. Audio mikrofon diproses pada HP; Nuraya tidak mengunggah atau menyimpan rekaman/transkrip. Buka **Beranda → AI bacaan Al-Qur’an → Unduh & aktifkan AI** untuk memulai. Lihat [RECITATION_ASSISTANT.md](RECITATION_ASSISTANT.md) untuk alur, model, pengujian, dan batas kemampuan.

Hasil pengenalan dibandingkan dengan kata ayat yang dipilih. Hasil sementara ditandai sementara; hasil akhir menunjukkan perkiraan kata berbeda, belum terdengar, atau tambahan. Kecepatan pembaruan bergantung pada HP. Ini belum merupakan penilai tajwid, makhraj, harakat, atau panjang mad. Pengenal suara Android tersedia sebagai pilihan terpisah dengan persetujuan jika penyedianya memproses online.

## Teman belajar yang langsung tersedia

Menu **Teman belajar** kini mencari ayat, terjemahan, tafsir Kemenag, dan pelajaran tajwid dari data lokal. Pertanyaan boleh memakai bahasa sehari-hari; hasil adalah kutipan sumber, bukan jawaban generatif. Pengguna dapat membuka ayat dari setiap rujukan. Tidak ada server, kunci API, biaya layanan, atau pengiriman pertanyaan untuk alur ini. Lihat [LOCAL_LEARNING_ASSISTANT.md](LOCAL_LEARNING_ASSISTANT.md).

Kode `LearningRepository` dan `backend/server.py` lama tetap disertakan untuk integrasi opsional, tetapi tidak dipanggil oleh layar Teman belajar lokal. `ai_config.json` tetap tanpa alamat server. Tidak ada backend generatif berbayar yang diaktifkan atau dideploy. Pengujian backend memakai mock dan tidak membuktikan kualitas jawaban model.

## Pencarian dan pemutar video

Katalog mengganti seluruh tautan lama dengan **7 rujukan berbahasa Indonesia dari pengguna**: 4 video dan 3 playlist. Materi disusun dari makhraj/tajwid dasar, pengucapan dan kelancaran hingga tilawah/maqam. Dua playlist dapat dipilih pada tingkat menengah maupun lanjutan tanpa duplikasi. Tidak ada URL Shorts pada daftar baru, sehingga katalog Shorts kosong. Pencarian lokal mengenali variasi ejaan, istilah terkait, dan kalimat kebutuhan belajar, lalu mengurutkan hasil. Filter tingkat, format, topik, dan Tonton nanti tetap berlaku. Jika materi khusus belum ada, hasil terkait diberi penjelasan; video tidak diklaim membahas subtopik yang belum diperiksa. Ini bukan pencarian langsung seluruh YouTube dan tidak memerlukan YouTube Data API. Lihat [SMART_VIDEO_SEARCH.md](SMART_VIDEO_SEARCH.md).

Pemutar YouTube menggunakan embed resmi untuk URL video/Shorts/playlist yang didukung dan dimuat setelah pengguna menekan **Tonton di aplikasi**. Ketujuh entri mempunyai bentuk URL yang dapat disematkan; ini tidak menjamin penerbit masih mengizinkan pemutarannya. Nuraya tidak mengekstrak stream, menyalin file, memblokir iklan, atau melewati pembatasan penyematannya. Video tetap memakai internet dan ketentuan YouTube. Video yang menolak embed menyediakan tautan sumber. Judul, bahasa dan atribusi katalog mengikuti informasi pengguna; pemeriksaan metadata YouTube langsung dari cloud terhalang HTTP 403. Pemutaran YouTube dan penggunaan mikrofon AI belum diuji pada HP fisik. Rincian keamanan, referrer, lifecycle dan batas pengujian terdapat di [VIDEO_EMBED.md](VIDEO_EMBED.md).

`streamUrl` tetap kosong untuk entri YouTube; `license: link-only` tidak diubah menjadi klaim lisensi redistribusi. Jalur Media3 terpisah hanya untuk sumber dengan izin stream yang dinyatakan, bukan URL halaman YouTube. Hak konten tetap pada penerbit. Bukti sumber katalog ada di [VIDEO_SOURCES.md](VIDEO_SOURCES.md); hasil pengujian rilis di [BUILD_VALIDATION.md](BUILD_VALIDATION.md).
