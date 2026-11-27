# Revisi iOS 1.7.1 build 4 — kesinambungan audio pembaca Quran

Laporan pengguna: audio pada halaman Al-Qur’an terdengar berjeda setiap berpindah ayat di berbagai mode bacaan.

## Penyebab dan perubahan

Pemutar sebelumnya menghentikan AVPlayer, membuat AVPlayerItem baru, dan melakukan seek setiap kali ayat selesai. Untuk qari dengan timing, URL itu sebenarnya rekaman surah yang sama. Pergantian item yang tidak diperlukan mengulang persiapan/buffering. Untuk audio per ayat, berkas selanjutnya baru disiapkan setelah ayat sebelumnya selesai.

Sekarang ayat berurutan dari rekaman surah yang sama diputar melalui satu item. Ketika melewati batas ayat, hanya informasi ayat dan sorotan yang berubah. Tidak ada pause, seek, penggantian item atau pemotongan jeda napas/waqaf di batas internal tersebut. Rekaman berhenti pada akhir rentang yang dipilih. Pengulangan ayat/rentang, perpindahan surah, dan tombol lompat tetap menjadi tindakan terpisah.

Qari dengan berkas per ayat memakai AVQueuePlayer dengan maksimal tiga item aktif: ayat sekarang dan dua ayat berikutnya. Antrean native meneruskan audio ke item yang sudah disiapkan. Berkas tidak dipercepat, ditumpuk dengan crossfade, atau dipotong untuk menghapus keheningan asli. Koneksi lambat atau keheningan yang memang terdapat dalam berkas sumber masih dapat terdengar.

Seluruh mode pembaca memakai jalur audio bersama ini, termasuk Quran per ayat, per kata, tajwid, mushaf dan hafalan. Perilaku build 3 dipertahankan: keluar dari pembaca menghentikan audio Quran; Murottal tetap memiliki konteks audio latar belakang sendiri.

## Penanda waktu yang tidak dapat dipakai

Audit memeriksa 1.254 pasangan qari/surah bertiming (11 qari × 114 surah). Empat pasangan menunjukkan batas waktu mundur atau placeholder sangat singkat:

| Qari | Surah | Masalah sumber |
| --- | --- | --- |
| Mishary Rashid Alafasy | Ibrahim (14), ayat 17 | Akhir klip lebih awal daripada akhir ayat sebelumnya. |
| Mishary Rashid Alafasy | Al-Ankabut (29), ayat 11 | Awal klip mundur ke rentang ayat sebelumnya. |
| Ahmed Al-Ajmi | Asy-Syu’ara (26), ayat 21 | Placeholder 0–1 milidetik. |
| Hani Ar-Rifai | Al-An’am (6), ayat 139 | Placeholder 0–1 milidetik. |

Pemeriksaan juga menolak batas klip yang tumpang tindih ketika sumber sudah menandai penyelarasan katanya tidak layak (`usable: false`). Secara keseluruhan ada **23 pasangan** yang memakai sumber per ayat: Alafasy surah 3, 4, 6, 13, 14, 20, 23, 26, 27, 29, 33, 37, 59, 60, 66, 70, 84, 93, 98, 101, 113; Al-Ajmi surah 26; serta Ar-Rifai surah 6.

Untuk pasangan ini, seluruh surah menggunakan sumber audio per ayat dari qari yang sama, yang juga disiapkan dalam antrean. Nuraya tidak menebak batas baru atau mengubah data sumber. Sorotan kata dinonaktifkan untuk pasangan ini karena tidak ada timing yang lolos pemeriksaan. Pasangan lain tetap memakai rekaman surah dan timing yang sesuai identitas sumber.

## Pemeriksaan

Tes tambahan memeriksa:

- Seluruh 1.254 pasangan bertiming: 1.231 membentuk satu rentang pemutaran per surah, 23 lainnya memakai sumber per ayat yang sesuai.
- Urutan ayat dengan pengulangan per ayat, beberapa putaran, dan rentang lintas surah.
- Rekaman Al-Fatihah Alafasy yang sesuai URL timing: player item tetap sama, jam audio terus maju, dan tidak ada seek/pause ketika melewati ayat 1–3.
- Tiga rekaman Al-Ikhlas Muhammad Jibreel: item ayat 2–3 sudah diantrekan sebelum ayat 1 selesai dan item yang sama digunakan pada perpindahan.
- Memulai dari ayat 2, menjeda ketika pemutar masih bersiap, melanjutkan, dan menghentikan pembaca.

Rekaman pengujian dipatok SHA-256 dan hanya tersedia pada bundle tes. Paket aplikasi pengguna tidak memuat rekaman atau model tes. [Workflow build 4](https://github.com/Naufal00487/Nuraya/actions/runs/45947277501) berhasil: **35 tes lulus** (11 core, 13 platform, 11 UI). Hasil build dan checksum tercatat di [STATUS.md](STATUS.md) dan [VALIDATION.json](VALIDATION.json). Simulator tidak menggantikan pemeriksaan pendengaran pada iPhone fisik dan jaringan pengguna.
