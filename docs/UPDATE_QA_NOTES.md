# Pemeriksaan integrasi pembaruan 1.1.0

Arsip versi 1.1.0. Video yang disebut di bawah telah dihapus pada 1.3.0. Perilaku terkini ada di [UPDATE_1.3.0.md](UPDATE_1.3.0.md); hasil arsip ini bukan bukti pengujian ulang versi terbaru.

Dokumen ini mencatat pemeriksaan integrasi fitur belajar, bukan hasil pengujian
perangkat yang belum dilakukan. Hasil build final dicatat terpisah di
`BUILD_VALIDATION_1.1.0.md`.

## Kontrak yang diperiksa

- Korpus Al-Qur'an versi 1.0.1 tetap byte-identik. SHA-256 acuannya
  `f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c`.
  Data belajar tambahan tidak mengganti ayat, penomoran, atau terjemahan utama.
- Basmalah pembuka tetap terpisah pada 112 surah. Al-Fatihah 1:1 dan basmalah
  dalam An-Naml 27:30 dipertahankan; At-Taubah tanpa pembuka.
- Warna tajwid aktif secara default pada pemasangan baru dan preferensi lama
  yang belum mempunyai kunci baru. Menonaktifkannya tetap tersimpan bersama
  bahasa, terjemahan, ukuran tulisan, dan mode gelap.
- Span tajwid hanya dapat diterapkan pada teks Arab yang identik dengan teks
  referensinya, dengan offset UTF-16 yang valid. Sorotan kata memerlukan waktu
  yang berasal dari rekaman, bukan perkiraan dari panjang teks.
- Pencarian video dibatasi pada katalog pembelajaran yang terkurasi. Tautan
  video publik tidak berarti rekaman berlisensi bebas atau boleh diunduh ulang.
- Hasil pengenalan suara merupakan pencocokan kata perkiraan. Hasil tersebut
  tidak membuktikan ketepatan makhraj, panjang harakat, atau seluruh hukum tajwid.

## Test integrasi tambahan

`StudyIntegrationTest` menggunakan aset dan SettingsRepository aplikasi yang
sebenarnya. Test memeriksa hash korpus yang dibekukan dari rilis sebelumnya,
batas basmalah, default warna, dan persistensi preferensi baru bersama preferensi
lama. Test renderer juga memeriksa bahwa teks tetap utuh, perbedaan tanda
gabungan tidak dianggap identik, rentang rusak tidak diterapkan, dan sorotan kata
tetap terlihat ketika warna tajwid dimatikan. Fragmen sintetis pada test renderer
hanya menguji batas anotasi; fragmen tersebut bukan sumber data Al-Qur'an.
Pemetaan warna juga memeriksa ejaan kunci sumber `idghaam_no_ghunnah`
agar tidak keliru diberi warna ghunnah hanya karena nama kunci memuat kata itu.

`QuranRecitationPresentationTest` menjalankan pembaca Compose sebenarnya dengan
state pemutar yang diberikan test. Skenarionya memeriksa pembukaan pembaca
saat ayat 2:255 aktif, serta pengaturan mengikuti audio yang dimatikan agar ayat
2:1 tetap tampil. Tombol ayat aktif harus mengendalikan audio yang sama;
tombol ayat lain harus meminta audio ayat tersebut. Test ini tidak memutar
atau menilai rekaman suara. Skenario ketiga memakai offset kata 1:1 dari aset
sebenarnya untuk memeriksa sorotan kata saat warna tajwid dimatikan.

Test integrasi data memeriksa semua 6.236 referensi ayat, 77.429 arti kata
Indonesia/Inggris, dan 60.023 rentang tajwid yang dibundel. Semua teks rujukan
harus sama persis dengan pembaca; substring kata yang dipetakan harus sama
dengan rentangnya. Kata 11:13 posisi 3 sengaja tidak dipetakan karena ejaan
sumber berbeda, tanpa menggeser posisi kata berikutnya. Tafsir dan sumber
atribusi harus tersedia pada seluruh referensi ayat.

Test integrasi di atas sudah dijalankan pada build awal 1.1.0 dan lulus.
Pemeriksaan tambahan juga meliputi pergantian qari pada ayat yang sedang aktif,
state rentang hafalan ketika berpindah tab, buka/tutup teks hafalan, serta
tampilan arti per kata dan tafsir dari asset sebenarnya. Hasil akhir seluruh
suite dan APK dicatat di `BUILD_VALIDATION.md` setelah paket final disusun.

Pemeriksaan Python pada 24 Oktober 2026 telah dijalankan: verifikasi korpus
dan aset belajar berhasil, delapan test pembuka Al-Qur'an lulus, dan delapan
test backend lulus.
Tidak ada layanan model eksternal yang dipanggil oleh pemeriksaan tersebut.

## Pemeriksaan yang membutuhkan perangkat

Pengujian speaker/Bluetooth, perpindahan ayat saat pemutaran, izin mikrofon,
ketersediaan layanan pengenal Bahasa Arab pada HP, lifecycle rekaman, serta
pemasangan pembaruan perlu dijalankan pada perangkat atau emulator yang stabil.
Build APK dan test host tidak menggantikan pengujian tersebut.
