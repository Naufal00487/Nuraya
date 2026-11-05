# Hafalan, tajwid, kuis, dan progres

Nuraya 1.1.0 menambahkan halaman **Latihan & hafalan** yang dapat dibuka dari beranda. Empat tab tersedia tanpa akun:

- **Hafalan:** pilih surah, rentang ayat dan jumlah pengulangan audio (1–10 kali per ayat). Teks disembunyikan pada awal latihan. Setelah mencoba dari ingatan, tampilkan teks, periksa, lalu catat `Perlu mengulang`, `Masih memakai bantuan`, atau `Lancar menurut saya`. Teks dan terjemahan memakai korpus pembaca yang sama; basmalah pembuka tidak disisipkan ke ayat bernomor.
- **Tajwid:** pengantar riwayat Hafs, contoh ayat dari korpus asli, tautan sumber, dan tombol mendengarkan contoh tiga kali melalui pemutar ayat. Warna diambil dari anotasi studi yang sudah diverifikasi, bukan dari penandaan manual seluruh kutipan. Tombol `Tandai sudah dibaca` hanya menandai kegiatan membaca pelajaran.
- **Kuis:** pertanyaan pengetahuan dengan pilihan jawaban, jawaban yang benar, alasan, dan skor sesi. Jawaban dikunci setelah disimpan; memilih opsi saja belum menambah statistik.
- **Progres:** target harian 1–50 penilaian ayat, jumlah kegiatan per hari, hari berturut-turut, ayat yang pernah dilatih, jadwal ulang, pelajaran dibaca, sesi selesai dan jumlah jawaban kuis. Membuka jadwal ulang membawa pengguna ke ayat terkait.

Pilihan rentang, posisi latihan dan jawaban kuis menggunakan state yang dapat disimpan saat berpindah tab. Root aplikasi juga mempertahankan state layar saat pengguna membuka pembaca/audio lalu kembali.

## Penyimpanan dan jadwal

`PracticeRepository` menggunakan DataStore `nuraya_practice` yang terpisah dari preferensi pembaca dan database koleksi. Tidak ada akun atau pengiriman statistik ke server. Menghapus data aplikasi menghapus progres lokal.

Penilaian **perlu mengulang** dijadwalkan kembali hari ini, **memakai bantuan** besok, dan **lancar** tiga hari kemudian. Penilaian lancar berturut-turut menggandakan interval sebelumnya dengan batas 60 hari. Jadwal dihitung dari tanggal penilaian yang benar-benar dilakukan. Ini pengingat latihan sederhana dan bukan algoritme penilaian kemampuan bersertifikat.

Setiap penilaian menambah kegiatan harian, termasuk latihan ulang ayat yang sama. Statistik ayat pernah dilatih menghitung referensi yang berbeda. Penilaian ayat terakhir dan penambahan sesi selesai ditulis dalam satu transaksi DataStore. Riwayat harian dibatasi sekitar satu tahun; jumlah kegiatan keseluruhan tetap tersimpan.

## Kejujuran hasil

Penilaian hafalan berasal dari pengguna. Menekan `Lancar menurut saya` tidak membuktikan hafalan benar. Kuis menguji pengetahuan, bukan kualitas pelafalan, tajwid, makhraj, atau hasil pengenalan suara. Guru Al-Qur’an tetap diperlukan untuk memeriksa bacaan.

Kutipan Arab harus identik dengan teks ayat yang dibundel. `PracticeContent` memvalidasi referensi kutipan dan hubungan pertanyaan ke pelajaran saat dibaca. Rujukan dan penjelasan editorial dicatat pada bagian sumber di bawah.

## Sumber pelajaran dan contoh

Aset `practice_lessons.json` berisi delapan pengantar tajwid riwayat Hafs dan sepuluh pertanyaan berbahasa Indonesia serta Inggris. Setiap pertanyaan memiliki jawaban, penjelasan, dan referensi ke pelajaran bersumber. Penjelasan adalah ringkasan editorial dari halaman About Tajweed di bawah, dibaca pada 24 Oktober 2026; bukan kutipan langsung, terjemahan Al-Qur’an, atau fatwa. Pengantar ini tidak mencakup semua rincian dan pengecualian tajwid.

| Pelajaran | Contoh ayat | Rujukan |
| --- | --- | --- |
| Ghunnah pada nun dan mim bertasydid | 108:3 | [The Ghunnah](https://www.abouttajweed.com/index.php/tajweed-rules/46-the-ghunnah-and-its-ranks/18-the-ghunnah) |
| Mad asli: dua harakat | 1:4 | [The Mudood (Lengthenings) Part 1](https://www.abouttajweed.com/index.php/tajweed-rules/49-the-mudood-the-lengthenings/101-the-mudood-lengthenings-part-1) |
| Qalqalah: pantulan tanpa vokal tambahan | 112:3 | [The Qalqalah Mechanism](https://www.abouttajweed.com/index.php/tajweed-rules/50-the-qalqalah/46-the-qalqalah-mechanism) |
| Alif lam syamsiyyah | 1:3 | [Laam saakinah rules, part 1](https://www.abouttajweed.com/index.php/tajweed-rules/48-the-laam-saakinah/44-laam-saakinah-rules-part-1-laam-of-the-definite-article-the) |
| Idgham dengan dengung | 105:4 | [Merging of the noon saakinah and tanween part 2](https://www.abouttajweed.com/index.php/tajweed-rules/47-the-noon-saakinah-and-tanween/37-merging-of-the-noon-saakinah-and-tanween-part-2) |
| Idgham tanpa dengung | 112:4 | [Merging of the noon saakinah and tanween part 1](https://www.abouttajweed.com/index.php/tajweed-rules/47-the-noon-saakinah-and-tanween/36-merging-of-the-noon-saakinah-and-tanween-part-1) |
| Iqlab sebelum ba | 2:10 | [Changing (iqlaab) rule of the noon saakinah and tanween](https://www.abouttajweed.com/index.php/tajweed-rules/47-the-noon-saakinah-and-tanween/39-changing-iqlaab-rule-of-the-noon-saakinah-and-tanween) |
| Ikhfa pada nun sukun dan tanwin | 113:2 | [Concealment (ikhfa') rule of the noon saakinah and tanween](https://www.abouttajweed.com/index.php/tajweed-rules/47-the-noon-saakinah-and-tanween/40-hiding-ikhfa-rule-of-the-noon-saakinah-and-tanween) |

Kutipan Arab disalin langsung dari korpus Tanzil yang dibundel melalui `study/{surah}.json`, tanpa pengetikan ulang atau normalisasi. Setiap kutipan merupakan substring persis dari ayat bernomor setelah kebijakan pemisahan basmalah. Semua contoh mengandung rentang anotasi untuk hukum yang dijelaskan. Rentang warna berasal dari [quran-tajweed oleh Colin P. Fair](https://github.com/cpfair/quran-tajweed), berlisensi CC BY 4.0; paletnya mengikuti `TajwidPalette`. Sumber teks dan anotasi selengkapnya ada di [DATA_SOURCES.md](DATA_SOURCES.md).

Lama ghunnah dijelaskan sebagai bunyi yang dipelajari dengan guru, sesuai penjelasan sumber; pelajaran tidak menetapkan durasi detik atau jumlah hitungan yang kaku. Mad asli dibedakan secara eksplisit sebagai dua harakat yang mengikuti tempo bacaan. Contoh idgham, iqlab, dan ikhfa lintas kata dijelaskan untuk bacaan sambung. Audio contoh memutar ayat lengkap agar konteks sambung dan berhentinya dapat didengar.

## Pengujian

`PracticeRepositoryTest` memeriksa penyimpanan yang dibuka ulang, transisi jadwal dan pengulangan, penulisan sesi secara atomik, input tidak valid, hitungan hari berturut-turut, serta integritas kutipan dan pertanyaan pada aset sebenarnya. Root menjalankan Gradle bersama pengujian aplikasi lainnya; hasil eksekusi dicatat di [BUILD_VALIDATION.md](BUILD_VALIDATION.md).
