# Pemeriksaan referensi Islampedia

Pekerjaan ini memakai APK **Islampedia Quran Belajar 1.9.10** dan rekaman layar 56,98 detik yang diberikan pengguna, bukan perkiraan dari nama fitur. APK berhasil diunduh melalui tautan Drive pengguna: 57.846.147 byte, SHA-256 `c992c27d56897c62df6f515313303b9f1efdda1c8bca36c332d5c6ab16e202cc`. Paket: `id.quranbelajar.app`, versionCode 116. Rekaman SHA-256 `3654ae154fa55e8b053832f084aaae4d559e2d8a42ae54245c8b4afc4fc7532b`.

APK diperiksa statis; database berisi 114 surah dan 6.236 ayat unik. Gambar halaman lengkap tidak dibundel. Server gambar yang dirujuk meminta autentikasi HTTP 401; kredensialnya tidak diekstrak/dipakai. Dekompilasi parsial membantu memeriksa perilaku dan skema koordinat, bukan menyalin implementasi.

Pengguna belum memiliki PDF/ZIP penerbit. Pencarian selanjutnya menemukan **sumber publik tujuh edisi lengkap** yang cocok secara visual dengan halaman Per Kata contoh. Rincian sumber, pemeriksaan 4.228 gambar, dua koreksi koordinat, indeks tiap edisi dan batas Hafalan ada di [PRINTED_MUSHAF.md](PRINTED_MUSHAF.md). Pengguna tidak perlu mengumpulkan screenshot halaman satu-satu untuk tujuh edisi tersebut.

| Adegan rekaman | Perilaku yang diamati | Implementasi Nuraya |
| --- | --- | --- |
| 00:06 | Tekan lama ayat membuka tindakan | Pilih ayat pada gambar berpeta, putar, penanda, salin, bagikan, terjemahan |
| 00:08–14 | Halaman Madinah p7 memenuhi layar; bar tipis dan sorotan ayat | Gambar asli, overlay kontrol satu bar, sorotan ayat pada peta sumber |
| 00:16–18 | Pengaturan qari/rentang/pengulangan | Memakai pemutar Nuraya dengan identitas qari dan timing yang sudah dipasangkan |
| 00:24–26 | Madinah Tajwid p8 berbeda dari Madinah biasa | Sumber PNG `md-tajwid`, bukan teks reflow atau gambar `md` yang diberi nama lain |
| 00:28–36 | Panel terjemahan/Latin dapat dibuka dan ditutup | Panel scroll, pemilihan ayat, toggle Latin dan ukuran tulisan, tanpa resize halaman |
| 00:38 | Menuju surah dan ayat | Indeks spesifik edisi, mencakup seluruh 114 surah/6.236 ayat |

AI, profil privat, qari terkurasi dan ibadah Nuraya dipertahankan. Bahriyah/pojok serta Video/Shorts tidak dikembalikan. Tidak ada klaim bahwa seluruh konten/layanan/branding Islampedia disalin atau bahwa tes host menggantikan tes HP fisik.
