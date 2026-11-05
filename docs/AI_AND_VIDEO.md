# AI dan sumber belajar — Nuraya 1.3.0

Fitur video telah dihapus seluruhnya sesuai permintaan pengguna. Nama file ini dipertahankan agar tautan dokumentasi lama tetap terbuka; isinya menjelaskan status sekarang. [Arsip 1.2.2](AI_AND_VIDEO_1.2.2.md) mencatat implementasi video yang sudah dihentikan.

## AI bacaan lokal tetap tersedia

Buka **Beranda → AI bacaan Al-Qur’an**. Model Tarteel Whisper Quran sekitar 59,7 MB diunduh satu kali, diverifikasi ukuran/SHA-256, lalu dipakai oleh mesin Whisper native di HP. Rekaman/transkrip tidak disimpan atau diunggah oleh Nuraya. Tombol Cek mikrofon lima detik, indikator suara masuk, pembatalan dan batas waktu proses tetap tersedia.

Hasil pengenalan dibandingkan dengan kata ayat yang dipilih. Hasil sementara ditandai sementara; hasil akhir memperlihatkan perkiraan kata berbeda, belum terdengar atau tambahan. Model dapat keliru. Ini belum merupakan penilai tajwid, makhraj, harakat atau panjang mad tervalidasi; kecepatan bergantung perangkat. Pengenal suara Android merupakan pilihan terpisah dengan persetujuan bila penyedianya memproses online.

Lihat [RECITATION_ASSISTANT.md](RECITATION_ASSISTANT.md) dan [QURAN_AI_MODEL.md](QURAN_AI_MODEL.md) untuk model, alur privasi dan bukti pengujian terdahulu.

## Teman Belajar lokal tetap tersedia

Teman Belajar mencari ayat, terjemahan, tafsir Kemenag dan pelajaran tajwid dari data lokal. Pengguna dapat memakai pertanyaan sehari-hari, melihat kutipan sumber, kemudian membuka ayat. Hasil bukan jawaban generatif. Tidak ada kunci API, biaya layanan, atau pengiriman pertanyaan untuk alur ini. Lihat [LOCAL_LEARNING_ASSISTANT.md](LOCAL_LEARNING_ASSISTANT.md).

Kode `LearningRepository` dan `backend/server.py` tetap disertakan sebagai integrasi opsional, tetapi layar Teman Belajar tidak memanggilnya. `ai_config.json` tidak mempunyai alamat backend generatif aktif. Tes backend memakai mock dan tidak membuktikan kualitas jawaban model.

## Penghapusan video

APK 1.3.0 tidak memuat katalog `videos.json`, pemutar YouTube/WebView, Shorts atau pencarian video. Koleksi tidak lagi menampilkan jenis simpan/riwayat video, termasuk entri yang berasal dari pemasangan terdahulu. Data Quran, audio, penanda dan progres tetap tersedia.

Navigasi utama kini Beranda, Al-Qur’an, Murottal dan Ibadah. Lihat [pembaruan 1.3.0](UPDATE_1.3.0.md). Arsip sumber dan laporan pengujian video hanya merupakan catatan versi sebelumnya, bukan fitur yang masih aktif.
