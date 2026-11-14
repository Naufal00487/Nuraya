# Pencarian hadis offline

Nuraya 1.6.2 memakai [fawazahmed0/hadith-api](https://github.com/fawazahmed0/hadith-api), commit `df57907be35291c91ad6a6691180e22ca9920784`, [Unlicense](licenses/HADITH-UNLICENSE.txt). Sumber Arab dan Indonesia dipasangkan berdasarkan **nomor lengkap dan referensi kitab/hadis yang sama**, bukan urutan array. Kutipan disimpan tanpa perubahan; normalisasi hanya untuk indeks pencarian.

Tersedia 15.152 entri: Bukhari 7.589 dan Muslim 7.563. Sebanyak 11.785 mempunyai teks Indonesia dan 14.940 mempunyai teks Arab. Entri kosong tetap ditandai belum tersedia; aplikasi tidak mengarang isinya. Varian riwayat dan nomor pecahan mengikuti edisi dataset, bukan disatukan atau dibulatkan.

Cari kata, frasa atau rujukan `Bukhari 1`, `Muslim 1`, lalu pilih hasil untuk membaca teks lengkap. Filter kitab dan muat hasil berikutnya tersedia. Pencarian memakai SQLite FTS4 secara lokal; tidak mengirim kueri ke server dan tidak membuat jawaban agama baru. Ini pencarian kata/rujukan, bukan mesin fatwa.

Corpus gzip dikemas sebagai `assets/hadith/hadith.database`: ekstensi `.gz` dihindari karena Android asset merger menguraikannya otomatis. Database diekstrak sekali ke penyimpanan privat aplikasi saat pertama kali dibutuhkan. Data terkompresi sekitar 11,3 MB, hasil ekstraksi sekitar 44,5 MB. Indeks FTS4 contentless menghindari salinan teks tambahan. Verifier memeriksa hash, struktur tabel fisik dan kecocokan posting indeks untuk setiap entri; full integrity_check SQLite 3.53 tidak mendukung rekonstruksi isi tabel contentless.

```sh
python3 scripts/import_hadith.py --verify
# Untuk membangun ulang dari sumber yang dipatok:
python3 scripts/import_hadith.py --cache /tmp/nuraya-hadith
```

Hash sumber, ukuran, nomor commit dan jumlah tiap kitab tercatat di `app/src/main/assets/hadith/sources.json`.
