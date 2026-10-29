# Perubahan Nuraya 1.3.0

Versi 1.3.0, versionCode 7, memakai applicationId dan sertifikat yang sama agar dapat memperbarui pemasangan sebelumnya. Referensi kebutuhan pengguna adalah jam/tanggal Hijriah, waktu salat, kiblat, pilihan belajar Quran dan navigasi yang lebih mudah dibaca. Nuraya tidak mengklaim afiliasi, menyalin merek, atau menjadi edisi resmi aplikasi lain.

## Navigasi dan aksesibilitas

Empat tujuan utama: **Beranda, Al-Qur’an, Murottal, Ibadah**. Beranda memberi tombol berlabel untuk AI bacaan, membaca, murottal, latihan, Teman Belajar, Koleksi dan Pengaturan. Tombol kembali diberi teks, ukuran tulisan diperbesar, warna teks sekunder diperjelas, dan area interaksi Material minimal 56 dp. Konten tetap dapat digulir dan menggunakan skala font sistem. Ukuran Arab awal 34 sp; pilihan tersimpan pengguna tetap dihormati. Pengaturan mempunyai tombol Kecilkan/Besarkan dan contoh Arab.

Ini perubahan antarmuka untuk membantu keterbacaan, bukan klaim telah diuji oleh semua kelompok lansia atau lolos sertifikasi aksesibilitas. Pengujian tampilan host memeriksa alur pada ukuran tertentu dan skala font yang dinyatakan di laporan build.

## Video dihapus

Seluruh katalog, pencarian, pemain YouTube, Shorts dan menu video dihapus. Entri video yang tersimpan dari versi lama disembunyikan oleh repository Koleksi; database tidak direset. Penanda Quran, audio favorit, unduhan, riwayat bacaan, kuis dan progres tetap dipertahankan. Dokumentasi video sebelum penghapusan disimpan sebagai arsip 1.2.2.

## Jam, Hijriah, salat dan kiblat

Jam dan tanggal mengikuti zona kota yang dipilih atau waktu perangkat sebelum kota dipilih. Kalender Hijriah memakai `HijrahDate`/Umm al-Qura berdasarkan tanggal sipil; pengguna dapat mengoreksi −2 sampai +2 hari untuk menyesuaikan keputusan setempat.

Jadwal dihitung offline dengan Adhan Java 1.2.1, Subuh 20°, Isya 18°, Asar Syafii, dan koreksi menit yang dapat disesuaikan. Kota/koordinat dan zona waktu harus dipilih secara eksplisit; tidak ada asumsi bahwa pengguna berada di Jakarta, izin GPS wajib, atau pelacakan latar. Hitung mundur melewati tengah malam menuju salat berikutnya, bukan matahari terbit. Hasil perhitungan bukan jadwal resmi yang diunduh dari Kemenag. Wilayah lintang tinggi dan waktu astronomis yang tidak tersedia mempunyai penjelasan tersendiri.

Arah kiblat dihitung dari koordinat menuju Ka'bah. Kompas menyesuaikan orientasi layar dan deklinasi magnetik bila sensor tersedia. Jika sensor tidak tersedia atau belum akurat, antarmuka tetap menunjukkan arah numerik terhadap utara sejati dan petunjuk kalibrasi. Pengujian rumus tidak membuktikan akurasi sensor fisik setiap HP.

## Pilihan membaca Quran

| Pilihan | Perilaku dan batas |
| --- | --- |
| Al-Qur’an per ayat | Ayat berurutan dengan audio, penanda dan bantuan belajar. |
| Per kata & tajwid | Arti kata ditampilkan langsung; warna hukum bacaan memakai anotasi pada teks Arab. |
| Latin & tajwid | Latin Indonesia untuk 6.236 ayat dari eQuran.id, tersedia offline, di samping teks Arab bertajwid. Latin tetap tanpa warna; warna Arab tidak dipetakan secara dugaan ke Latin. |
| Al-Qur’an Indonesia | Arab dan terjemahan Kementerian Agama. |
| Indonesia & tajwid | Terjemahan Indonesia bersama warna tajwid Arab. |
| Al-Qur’an Madinah | Penampil gambar mushaf 604 halaman, dengan pencarian nomor halaman, pembesaran dan pemetaan surah/ayat ke halaman. Halaman 1, 2, 3 dan 604 disertakan offline; halaman lainnya diunduh saat dibuka dan disimpan di cache. |
| Madinah & tajwid | Gambar halaman Madinah beserta tombol membuka bacaan ayat dengan warna tajwid. Gambar halaman asli tetap tanpa pewarnaan; ini bukan pindai edisi tajwid berwarna. |
| Hafalan 8 blok | Bacaan dipisahkan menjadi hingga delapan kelompok ayat untuk latihan lihat/sembunyikan; bukan reproduksi blok berwarna edisi cetak tertentu. Bacaan dengan kurang dari delapan ayat mempunyai lebih sedikit kelompok. |
| Al-Qur’an Tikrar | Pengulangan audio dan penghitung latihan mandiri; bukan klaim salinan mushaf Tikrar berlisensi tertentu. |
| Waqaf & ibtida | Tanda waqaf dari Quran Uthmani/Islamic Network ditampilkan sebagai anotasi hanya bila rasm ayat lengkap dan batas katanya selaras. Teks kanonis tetap utuh; tanda tidak dibuat berdasarkan tebakan. Belajar menyambung makna tetap memerlukan bimbingan guru. |
| Mushaf Bahriyah/pojok | Penampil PDF lokal untuk edisi Bahriyah milik pengguna, dengan pilihan berkas, pembesaran dan navigasi halaman. PDF Bahriyah belum disertakan karena belum ada sumber edisi yang terverifikasi. Halaman Madinah tidak dipakai sebagai pengganti. |
| Terjemah tematik | Indeks rujukan tema dengan kutipan terjemahan dari corpus yang sama dan tombol membuka konteks/tafsir. Indeks bukan tafsir baru atau daftar seluruh ayat setiap tema. |

Seluruh bantuan membaca mempertahankan teks kanonis, nomor ayat dan kebijakan basmalah. Warna tajwid mengikuti pilihan pengaturan pengguna. Terjemahan dan tafsir tetap mencantumkan sumber.

Latin diimpor dari `teksLatin` eQuran.id untuk 114 surah/6.236 ayat. Snapshot respons mentah dan checksum disimpan di `docs/sources/latin`, sedangkan data aplikasi disimpan dalam `assets/latin`. Transliterasi membantu pembaca awal, tetapi tidak menggantikan mendengar qari dan belajar pelafalan dengan guru.

Anotasi waqaf berasal dari `https://api.alquran.cloud/v1/quran/quran-uthmani`, dicatat dengan atribusi Islamic Network/Tanzil serta SHA-256 sumber. `scripts/import_waqaf.py` menyelaraskan rasm ayat secara penuh dan batas kata tanpa mengubah `quran.json`. Dari 6.236 ayat, 6.234 dapat diselaraskan; 4.362 tanda terdapat pada 2.639 ayat. Dua rujukan, 12:39 dan 12:41, sengaja tidak diberi anotasi tambahan karena rasm tidak cocok. Tanda small-high-seen di dalam kata seperti 2:245 dan 7:69 tidak dianggap saktah; hanya tanda batas kata sumber yang sesuai masuk ke indeks. Berkas `assets/waqaf.json` menyimpan tanda terverifikasi, referensi yang dikecualikan dan checksum sumber. Teks dengan tambahan tanda adalah tampilan anotasi; teks kanonis, rentang tajwid dan sorotan audio dasarnya dipertahankan.

## Gambar dan foto

Gambar Madinah berasal dari `GovarJabbar/Quran-PNG` pada commit `b189391148dfb4d7b44e7de40fe2cf942e7c84ac`, yang merujuk gambar Quran.com dan karya halaman/glyph King Fahd Quran Complex. Manifest 604 URL beserta hash Git blob tersedia dalam `assets/mushaf/madinah-images.json`; pemetaan 6.236 ayat ke halaman bersumber dari Quran Uthmani Islamic Network. Aplikasi memeriksa integritas gambar unduhan sebelum memakainya. Pratinjau pada pilihan Madinah merupakan halaman Quran yang nyata, bukan foto sampul semua edisi. Tidak ada teks Quran atau wajah qari yang dibuat menggunakan generator gambar. Tidak ada klaim lisensi CC terpisah untuk gambar tersebut.

PDF Bahriyah dibuka dari perangkat melalui pemilih berkas Android. Berkas tidak diunggah; susunan halaman mengikuti PDF yang dipilih. Pengguna harus mempunyai PDF edisi yang ingin dibaca. Penampil ini tidak mengklaim menyertakan atau menerbitkan ulang sebuah edisi Bahriyah tertentu.

Delapan foto qari asli disertakan: Mishary Alafasy, Abdul Basit, Abdurrahman As-Sudais, Abu Bakr Ash-Shatri, Hani Ar-Rifai, Mahmoud Al-Husary, Muhammad Siddiq Al-Minshawi dan Saud Ash-Shuraim. Sumbernya repositori frontend resmi Quran.com pada commit `aff1a035b09b66f28047b3216edcae4c5c949a49`. `assets/portraits/catalog.json` mencatat profil sumber, identitas rekaman dan SHA-256 setiap foto. Qari tanpa pemetaan terverifikasi memakai inisial; jumlah foto tidak sama dengan seluruh 138 pilihan rekaman. Hak foto tetap milik pemegangnya; lisensi MIT proyek sumber tidak dianggap lisensi terbuka terpisah untuk tiap foto.

## Fitur lama dan validasi

AI bacaan lokal, pemeriksaan mikrofon, Teman Belajar, tafsir, arti kata, warna tajwid, qari, sinkronisasi kata yang tersedia, kuis dan progres tetap dipertahankan. AI masih pengenal/perbandingan kata yang bisa keliru, bukan penilai tajwid/makhraj tervalidasi. Model AI tidak berubah hanya karena menu video dihapus.

Hasil build, tes JVM/Compose, lint, pemeriksaan APK dan batas perangkat dicatat di [BUILD_VALIDATION.md](BUILD_VALIDATION.md). Jangan memakai jumlah tes atau hasil video versi terdahulu sebagai bukti pengujian versi ini. Sumber data terdahulu ada di [DATA_SOURCES.md](DATA_SOURCES.md), [STUDY_DATA.md](STUDY_DATA.md) dan [AUDIO_CATALOG.md](AUDIO_CATALOG.md).
