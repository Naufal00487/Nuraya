# Fitur Nuraya 1.5.2

Versi 1.5.2, versionCode 12, memakai applicationId dan sertifikat yang sama agar dapat memperbarui pemasangan sebelumnya. Referensi kebutuhan pengguna adalah jam/tanggal Hijriah, waktu salat, kiblat, pilihan belajar Quran dan navigasi yang lebih mudah dibaca. Nuraya tidak mengklaim afiliasi, menyalin merek, atau menjadi edisi resmi aplikasi lain.

## Navigasi dan aksesibilitas

Empat tujuan utama: **Beranda, Al-Qur’an, Murottal, Ibadah**. Beranda memberi tombol berlabel untuk AI bacaan, membaca, murottal, latihan, Teman Belajar, Koleksi dan Pengaturan. Tombol kembali diberi teks, ukuran tulisan diperbesar, warna teks sekunder diperjelas, dan area interaksi Material minimal 56 dp. Konten tetap dapat digulir dan menggunakan skala font sistem. Ukuran Arab awal 34 sp; pilihan tersimpan pengguna tetap dihormati. Pengaturan mempunyai tombol Kecilkan/Besarkan dan contoh Arab.

Antarmuka ini membantu keterbacaan, bukan klaim telah diuji oleh semua kelompok lansia atau lolos sertifikasi aksesibilitas. Pengujian tampilan host memeriksa alur pada ukuran tertentu dan skala font yang dinyatakan di laporan build.

## Video dihapus

Seluruh katalog, pencarian, pemain YouTube, Shorts dan menu video dihapus. Entri video yang tersimpan dari versi lama disembunyikan oleh repository Koleksi; database tidak direset. Penanda Quran, audio favorit, unduhan, riwayat bacaan, kuis dan progres tetap dipertahankan.

## Jam, Hijriah, salat dan kiblat

Jam dan tanggal mengikuti zona kota yang dipilih atau waktu perangkat sebelum kota dipilih. Kalender Hijriah memakai `HijrahDate`/Umm al-Qura berdasarkan tanggal sipil; pengguna dapat mengoreksi −2 sampai +2 hari untuk menyesuaikan keputusan setempat.

Jadwal dihitung offline dengan Adhan Java 1.2.1, Subuh 20°, Isya 18°, Asar Syafii, dan koreksi menit yang dapat disesuaikan. Pada penggunaan pertama, tombol **Aktifkan izin lokasi** meminta izin Android; setelah disetujui, koordinat HP diisi otomatis dan zona waktu mengikuti pengaturan HP. Izin perkiraan didukung. Jika layanan lokasi mati atau izin ditolak, tersedia pengaturan, coba lagi dan pilihan kota manual. Nuraya tidak menebak kota atau melacak lokasi di latar belakang. Posisi tersimpan dapat diperbarui melalui **Ibadah → Lokasi → Gunakan lokasi HP**; koreksi jadwal/Hijriah tetap dipertahankan. Hitung mundur melewati tengah malam menuju salat berikutnya, bukan matahari terbit. Hasil perhitungan bukan jadwal resmi yang diunduh dari Kemenag. Wilayah lintang tinggi dan waktu astronomis yang tidak tersedia mempunyai penjelasan tersendiri.

Arah kiblat dihitung dari koordinat menuju Ka'bah. Kompas menyesuaikan orientasi layar dan deklinasi magnetik bila sensor tersedia. Jika sensor tidak tersedia atau belum akurat, antarmuka tetap menunjukkan arah numerik terhadap utara sejati dan petunjuk kalibrasi. Pengujian rumus tidak membuktikan akurasi sensor fisik setiap HP.

## Pilihan membaca Quran

Sebelas pilihan bawaan membuka halaman kitab layar penuh, termasuk rujukan dari tema dan tombol Lanjutkan bacaan. Toolbar, navigasi aplikasi dan pemutar mini tidak mengambil ruang halaman. Ketuk halaman untuk membuka menu sebagai lapisan di atas gambar; ketuk lagi atau tekan Tutup menu baca untuk menyembunyikannya. Geser kanan ke halaman berikutnya, kiri ke sebelumnya; ketuk dua kali untuk lanskap. Rasio gambar sumber dipertahankan tanpa memotong tulisan sehingga ruang putih dapat tersisa pada layar yang lebih tinggi. Pada pembesaran, geseran dipakai untuk melihat bagian gambar.

Dua mode Madinah memakai sumber Madinah; sembilan mode bawaan lainnya memakai sumber Indonesia. Bantuan belajar tersedia melalui menu; ini bukan sebelas edisi cetak berbeda.

| Pilihan | Perilaku dan batas |
| --- | --- |
| Al-Qur’an per ayat | Halaman kitab Indonesia; menu Belajar per ayat membuka ayat berurutan, audio, penanda dan bantuan belajar. |
| Per kata & tajwid | Halaman kitab Indonesia; menu Arti & tajwid menampilkan arti kata dan anotasi warna pada teks Arab pendamping. |
| Latin & tajwid | Halaman kitab Indonesia; Latin aktif pada panel Arti & tajwid. Latin Indonesia untuk 6.236 ayat dari eQuran.id, tersedia offline, di samping teks Arab bertajwid. Latin tetap tanpa warna; warna Arab tidak dipetakan secara dugaan ke Latin. |
| Al-Qur’an Indonesia | 604 gambar Arab berbingkai; terjemahan Kementerian Agama dan arti per kata di panel pendamping. |
| Indonesia & tajwid | Gambar berbingkai yang sama, dengan tajwid berwarna dan arti per kata di panel pendamping. Warna tidak digambar ulang pada gambar halaman. |
| Al-Qur’an Madinah | Penampil gambar mushaf 604 halaman, dengan pencarian nomor halaman, pembesaran dan pemetaan surah/ayat ke halaman. Halaman 1, 2, 3 dan 604 disertakan offline; halaman lainnya diunduh saat dibuka dan disimpan di cache. |
| Madinah & tajwid | Gambar halaman Madinah beserta panel pendamping arti/kata/tajwid dan tombol membuka bacaan per ayat. Gambar halaman asli tetap tanpa pewarnaan; ini bukan pindai edisi tajwid berwarna. |
| Hafalan 8 blok | Halaman kitab Indonesia dengan tombol Latihan 8 blok. Di latihan, bacaan dipisahkan menjadi hingga delapan kelompok ayat untuk latihan lihat/sembunyikan; bukan reproduksi blok berwarna edisi cetak tertentu. Bacaan dengan kurang dari delapan ayat mempunyai lebih sedikit kelompok. |
| Al-Qur’an Tikrar | Halaman kitab Indonesia, audio awal tiga kali, dan tombol Latihan Tikrar. Pengulangan audio dan penghitung latihan mandiri; bukan klaim salinan mushaf Tikrar berlisensi tertentu. |
| Waqaf & ibtida | Halaman kitab Indonesia dengan tombol Panduan waqaf. Tanda waqaf dari Quran Uthmani/Islamic Network ditampilkan sebagai anotasi hanya bila rasm ayat lengkap dan batas katanya selaras. Teks kanonis tetap utuh; tanda tidak dibuat berdasarkan tebakan. Belajar menyambung makna tetap memerlukan bimbingan guru. |
| Mushaf Bahriyah/pojok | Penampil PDF lokal untuk edisi Bahriyah milik pengguna, dengan pilihan berkas, pembesaran dan navigasi halaman. Setelah PDF dipilih, halaman tampil penuh dan kontrol muncul dengan mengetuk halaman. PDF Bahriyah belum disertakan karena belum ada sumber edisi yang terverifikasi. Halaman Madinah tidak dipakai sebagai pengganti. |
| Terjemah tematik | Indeks rujukan tema dengan kutipan terjemahan dari corpus yang sama dan tombol membuka halaman kitab ayat tersebut. Tafsir tersedia dari menu bacaan. Indeks bukan tafsir baru atau daftar seluruh ayat setiap tema. |

Seluruh bantuan membaca mempertahankan teks kanonis, nomor ayat dan kebijakan basmalah. Warna tajwid mengikuti pilihan pengaturan pengguna. Terjemahan dan tafsir tetap mencantumkan sumber.

Latin diimpor dari `teksLatin` eQuran.id untuk 114 surah/6.236 ayat. Snapshot respons mentah dan checksum disimpan di `docs/sources/latin`, sedangkan data aplikasi disimpan dalam `assets/latin`. Transliterasi membantu pembaca awal, tetapi tidak menggantikan mendengar qari dan belajar pelafalan dengan guru.

Anotasi waqaf berasal dari `https://api.alquran.cloud/v1/quran/quran-uthmani`, dicatat dengan atribusi Islamic Network/Tanzil serta SHA-256 sumber. `scripts/import_waqaf.py` menyelaraskan rasm ayat secara penuh dan batas kata tanpa mengubah `quran.json`. Dari 6.236 ayat, 6.234 dapat diselaraskan; 4.362 tanda terdapat pada 2.639 ayat. Dua rujukan, 12:39 dan 12:41, sengaja tidak diberi anotasi tambahan karena rasm tidak cocok. Tanda small-high-seen di dalam kata seperti 2:245 dan 7:69 tidak dianggap saktah; hanya tanda batas kata sumber yang sesuai masuk ke indeks. Berkas `assets/waqaf.json` menyimpan tanda terverifikasi, referensi yang dikecualikan dan checksum sumber. Teks dengan tambahan tanda adalah tampilan anotasi; teks kanonis, rentang tajwid dan sorotan audio dasarnya dipertahankan.

## Gambar dan foto

Gambar Madinah berasal dari `GovarJabbar/Quran-PNG` pada commit `b189391148dfb4d7b44e7de40fe2cf942e7c84ac`, yang merujuk gambar Quran.com dan karya halaman/glyph King Fahd Quran Complex. Manifest 604 URL beserta hash Git blob tersedia dalam `assets/mushaf/madinah-images.json`; pemetaan 6.236 ayat ke halaman bersumber dari Quran Uthmani Islamic Network. Aplikasi memeriksa integritas gambar unduhan sebelum memakainya. Pratinjau pada pilihan Madinah merupakan halaman Quran yang nyata, bukan foto sampul semua edisi. Tidak ada teks Quran atau wajah qari yang dibuat menggunakan generator gambar. Tidak ada klaim lisensi CC terpisah untuk gambar tersebut.

PDF Bahriyah dibuka dari perangkat melalui pemilih berkas Android. Berkas tidak diunggah; susunan halaman mengikuti PDF yang dipilih. Pengguna harus mempunyai PDF edisi yang ingin dibaca. Penampil ini tidak mengklaim menyertakan atau menerbitkan ulang sebuah edisi Bahriyah tertentu.

Delapan foto qari dari Quran.com tetap disertakan: Mishary Alafasy, Abdul Basit, Abdurrahman As-Sudais, Abu Bakr Ash-Shatri, Hani Ar-Rifai, Mahmoud Al-Husary, Muhammad Siddiq Al-Minshawi dan Saud Ash-Shuraim. Sumbernya repositori frontend resmi Quran.com pada commit `aff1a035b09b66f28047b3216edcae4c5c949a49`. `assets/portraits/catalog.json` mencatat profil sumber, identitas rekaman dan SHA-256 setiap foto. Sebanyak 126 foto tambahan dipasang dari dokumen penyusun berdasarkan nama yang dituliskan dan ID edisi penyedia, sehingga totalnya 134 foto/140 pemetaan ID. Lima entri dokumen tidak dipasang: empat placeholder dan satu label yang bertentangan dengan keterangan pada gambar. Identitas wajah tidak dinyatakan telah diautentikasi secara independen. Qari tanpa foto memakai inisial. Daftar murottal menampilkan 137 pilihan, sementara 138 ID rekaman tetap didukung untuk data lama; dua alias Alafasy tampil sebagai satu kartu. Hak foto tetap milik pemegangnya; lisensi MIT proyek sumber tidak dianggap lisensi terbuka terpisah untuk tiap foto.

## AI, audio dan validasi

AI bacaan lokal, pemeriksaan mikrofon, Teman Belajar, tafsir, arti kata, warna tajwid, qari, sinkronisasi kata yang tersedia, kuis dan progres tetap dipertahankan. AI masih pengenal/perbandingan kata yang bisa keliru, bukan penilai tajwid/makhraj tervalidasi. Qari yang tidak tersedia meminta pengguna memilih ulang, tanpa mengganti diam-diam ke suara lain. Riwayat audio ayat mempertahankan qari dan referensi ayat yang tepat.

Hasil build, tes JVM/Compose, lint, pemeriksaan APK dan batas perangkat dicatat di [BUILD_VALIDATION.md](BUILD_VALIDATION.md). Hasil audit ayat, bantuan baca, pemetaan audio dan duplikasi ada di [AUDIT_1.3.1.md](AUDIT_1.3.1.md). Rujukan sumber ada di [DATA_SOURCES.md](DATA_SOURCES.md), [STUDY_DATA.md](STUDY_DATA.md) dan [AUDIO_CATALOG.md](AUDIO_CATALOG.md).

## Desain 1.5.0

Beranda memakai empat lanskap dari berkas desain sesuai jam setempat (05–10 pagi, 11–14 siang, 15–17 sore, selebihnya malam), jam/Hijriah, jadwal dan hitung mundur, tombol lokasi, azan, kiblat, pengaturan dan profil. Profil berisi nama panggilan dan lima ilustrasi avatar internal; tidak ada unggahan galeri, akun atau papan peringkat. Kuis dan progres tetap lokal.

Daftar qari berupa dua kolom foto/nama, dengan pilihan horizontal di beranda. Halaman qari menampilkan keterangan edisi rekaman yang bersumber dari penyedia. Biografi pribadi lengkap untuk semua qari belum tersedia; tanggal lahir/negara/riwayat pendidikan tidak diisi dengan dugaan.

Latihan hafalan menyediakan dua urutan audio: setiap ayat diulang N kali sebelum maju, atau seluruh rentang ayat diputar lalu diulang N kali. Nilai N dapat dipilih 1–20, dan ada pilihan seluruh surah. Contoh Al-Mulk 1–5 sebanyak tiga putaran menghasilkan tepat 15 item sebelum berhenti. Penilaian hafalan tetap catatan pribadi pengguna.

Banner beranda dapat digeser. Proyek backend Telegram tersedia di `backend/banner-bot`; belum deploy karena pengguna memilih menyiapkan proyek dahulu. Sebelum layanan diaktifkan, banner bawaan tetap berfungsi. Token bot hanya disimpan sebagai secret backend; tidak dimasukkan ke APK atau Git. Azan dijelaskan di [PRAYER.md](PRAYER.md).

Sejak 1.5.1, sumber gambar Quran Indonesia dipisahkan dari Madinah, termasuk indeks, cache dan halaman simpanan offline. Sejak 1.5.2 semua pilihan bawaan langsung membuka kitab; bantuan belajar dan latihan khusus tetap dapat dibuka dari menu. Bahriyah tetap memerlukan PDF edisi pengguna. [Sumber dan batas gambar](MUSHAF_IMAGES.md#indonesia-ornamental-pages--151).
