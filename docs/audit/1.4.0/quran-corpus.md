# Audit corpus Al-Qur’an — Nuraya 1.4.0

Audit otomatis tidak menemukan perbedaan impor terhadap sumber yang dipatok: **6.236 teks Arab, 6.236 terjemahan Indonesia, 6.236 terjemahan Inggris dan 6.236 entri catatan terjemahan cocok persis** dengan snapshot sumber. Jumlah 114 surah, 6.236 nomor ayat global dan 30 juz sesuai metadata. Duplikat ID ayat: 0; duplikat nomor global: 0. Corpus tidak diubah.

Sumber dipatok ke `risan/quran-json` commit `6523a9ad3b73bb7e8cb3a9cdf0ea774b8bab3123`. Empat snapshot dari audit 1.3.1 diverifikasi ulang secara lokal; checksum SHA-256-nya cocok dengan pin importer. Snapshot tersimpan di `docs/audit/1.3.1/sources/`. Arab berasal dari Tanzil Uthmani; Indonesia dari QuranEnc/Kementerian Agama versi 1.0.1; Inggris dari Pickthall. [Hasil mesin dan checksum](quran-corpus.json).

Kebijakan basmalah mempertahankan Al-Fatihah 1:1, tidak menambah pembuka At-Tawbah, dan memisahkan tepat **112 pembuka tanpa nomor** dari rentang tampilan ayat pertama surah lainnya. Pemisahan dapat digabungkan kembali byte-per-byte tanpa mengubah sumber. Basmalah dalam An-Naml 27:30 tetap berada dalam ayat. Nomor dan teks ayat tidak digeser.

Pemetaan Madinah memuat **6.236 referensi ke 604 halaman**, seluruhnya sama dengan sumber indeks; tidak ada referensi hilang/ganda atau halaman di luar batas. Ini audit indeks, bukan pembacaan visual setiap huruf pada gambar.

Sumber silang Islamic Network cocok persis pada 1876 ayat dan cocok setelah normalisasi pembanding pada 6234 ayat. Normalisasi hanya untuk audit, tidak diterapkan pada bacaan. Dua rujukan, **12:39 dan 12:41**, memiliki perbedaan ejaan yang tetap dicatat pada JSON (antara lain `يَٰصَٰحِبَىِ` dibanding `يَٰصَىٰحِبَىِ`). Teks Tanzil yang dipatok tetap dipertahankan; penanda waqaf tambahan pada kedua ayat itu tetap tidak ditebak. Audit ini tidak memutuskan salah/benar secara ilmiah atas dua sumber tersebut.

Ada **94 kelompok teks ayat yang berulang** pada 272 referensi berbeda; semua referensinya sah dan teksnya juga cocok persis dengan sumber utama. Ini pengulangan lafaz dalam Al-Qur’an, bukan duplikasi baris/ID aplikasi, sehingga tidak dihapus. Daftar lengkap referensi ada di hasil JSON.

Kesimpulan dibatasi pada integritas struktur, kesetiaan impor dan perbandingan sumber. Audit otomatis tidak menggantikan pemeriksaan ahli Al-Qur’an, tidak menyatakan seluruh pelafalan/makhraj terverifikasi, dan tidak membuktikan identitas suara qari. Pemeriksaan Latin, tajwid, waqaf dan audio dilaporkan terpisah.

Jalankan ulang: `python3 scripts/audit-quran-corpus.py`. Skrip memakai snapshot dengan hash yang telah dipatok dan tidak mengubah aset aplikasi.
