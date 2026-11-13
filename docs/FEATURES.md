# Fitur Nuraya 1.5.3

Versi 1.5.3, versionCode 13, memakai applicationId dan sertifikat yang sama agar dapat memperbarui pemasangan sebelumnya. Referensi kebutuhan pengguna adalah jam/tanggal Hijriah, waktu salat, kiblat, pilihan belajar Quran dan navigasi yang lebih mudah dibaca. Nuraya tidak mengklaim afiliasi, menyalin merek, atau menjadi edisi resmi aplikasi lain.

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

Tujuh pilihan membuka edisi gambar asli yang berbeda, masing-masing 604 halaman. Pembaca dimulai layar penuh. Ketuk untuk kontrol tipis, tekan lama untuk tindakan ayat pada edisi berpeta, dan geser untuk berpindah halaman. Bahriyah/pojok telah dihapus.

| Pilihan | Yang terbuka |
| --- | --- |
| Al-Qur’an per ayat | Teks ayat dan terjemahan sesuai pengaturan, dengan navigasi per ayat. |
| Per kata & tajwid | Gambar Per Kata Tajwid, terjemahan Indonesia tercetak langsung di bawah kata. |
| Latin & tajwid | Gambar Latin Tajwid dengan transliterasi tercetak menurut edisi sumber. |
| Al-Qur’an Indonesia | 604 gambar Indonesia berbingkai. |
| Indonesia & tajwid | 604 gambar Indonesia dengan warna tajwid asli. |
| Al-Qur’an Madinah | 604 gambar Madinah dengan indeks halaman yang sesuai edisi ini. |
| Madinah & tajwid | 604 gambar cetak Madinah Tajwid, bukan tata letak teks digital. |
| Hafalan 8 blok | 604 gambar Hafalan dengan blok warna cetaknya; uji hafalan dengan menyembunyikan halaman. Belum ada peta sorotan ayat untuk gambar Hafalan. |
| Al-Qur’an Tikrar | Halaman Indonesia Tajwid dengan pengaturan audio berulang dan penghitung latihan mandiri melalui menu Latihan Tikrar. |
| Waqaf & ibtida | Halaman Indonesia Tajwid dengan panduan tanda dan ibtida melalui menu pembaca. |
| Terjemah tematik | Rujukan ayat per tema, terjemahan dan tafsir. |

Arti/Latin panel dapat dibuka tanpa mengubah ukuran halaman. Audio dapat dimulai dan diakhiri pada surah/ayat berbeda, dengan ulang tiap ayat, putaran rentang dan loop hingga dijeda. Sorotan gambar mengikuti ayat pada enam edisi berpeta; sorotan kata tersedia pada panel teks dan rekaman yang memiliki timing. Detail sumber, koreksi peta dan cakupan pemeriksaan: [PRINTED_MUSHAF.md](PRINTED_MUSHAF.md).


Seluruh bantuan membaca mempertahankan teks kanonis, nomor ayat dan kebijakan basmalah. Warna tajwid mengikuti pilihan pengaturan pengguna. Terjemahan dan tafsir tetap mencantumkan sumber.

Latin diimpor dari `teksLatin` eQuran.id untuk 114 surah/6.236 ayat. Snapshot respons mentah dan checksum disimpan di `docs/sources/latin`, sedangkan data aplikasi disimpan dalam `assets/latin`. Transliterasi membantu pembaca awal, tetapi tidak menggantikan mendengar qari dan belajar pelafalan dengan guru.

Anotasi waqaf berasal dari `https://api.alquran.cloud/v1/quran/quran-uthmani`, dicatat dengan atribusi Islamic Network/Tanzil serta SHA-256 sumber. `scripts/import_waqaf.py` menyelaraskan rasm ayat secara penuh dan batas kata tanpa mengubah `quran.json`. Dari 6.236 ayat, 6.234 dapat diselaraskan; 4.362 tanda terdapat pada 2.639 ayat. Dua rujukan, 12:39 dan 12:41, sengaja tidak diberi anotasi tambahan karena rasm tidak cocok. Tanda small-high-seen di dalam kata seperti 2:245 dan 7:69 tidak dianggap saktah; hanya tanda batas kata sumber yang sesuai masuk ke indeks. Berkas `assets/waqaf.json` menyimpan tanda terverifikasi, referensi yang dikecualikan dan checksum sumber. Teks dengan tambahan tanda adalah tampilan anotasi; teks kanonis, rentang tajwid dan sorotan audio dasarnya dipertahankan.

## Gambar dan foto

Tujuh edisi gambar berasal dari sumber publik `agusibrahim/quran-image` pada commit tetap. Manifest mencatat 4.228 PNG dan hash sumber; setiap unduhan diperiksa sebelum digunakan. Halaman bundel tersedia langsung, halaman lain diunduh saat dibuka atau melalui unduhan satu edisi penuh. Gambar dan indeks edisi lama dipertahankan sebagai sumber historis/kompatibilitas, bukan ditampilkan di menu tujuh edisi baru. Tidak ada gambar Quran atau wajah qari yang dibuat menggunakan generator gambar. [Sumber dan batas pemeriksaan](PRINTED_MUSHAF.md).

Delapan foto qari dari Quran.com dipertahankan dengan pemetaan ID rekaman yang eksplisit. Sebanyak 126 foto dari dokumen penyusun dihapus dari APK. Daftar utama menampilkan lima qari surah lengkap; audio per ayat menampilkan tujuh qari dengan timing kata yang dipasangkan pada sumber rekaman. Rincian nama dan batasnya ada di [catatan rilis](UPDATE_1.5.3.md#qari-dan-profil). Identitas tidak disimpulkan dari kemiripan wajah/nama. Hak foto tetap milik pemegangnya.

## AI, audio dan validasi

AI bacaan lokal, pemeriksaan mikrofon, Teman Belajar, tafsir, arti kata, warna tajwid, qari, sinkronisasi kata yang tersedia, kuis dan progres tetap dipertahankan. AI masih pengenal/perbandingan kata yang bisa keliru, bukan penilai tajwid/makhraj tervalidasi. Qari yang tidak tersedia meminta pengguna memilih ulang, tanpa mengganti diam-diam ke suara lain. Riwayat audio ayat mempertahankan qari dan referensi ayat yang tepat.

Hasil build, tes JVM/Compose, lint, pemeriksaan APK dan batas perangkat dicatat di [BUILD_VALIDATION.md](BUILD_VALIDATION.md). Hasil audit ayat, bantuan baca, pemetaan audio dan duplikasi ada di [AUDIT_1.3.1.md](AUDIT_1.3.1.md). Rujukan sumber ada di [DATA_SOURCES.md](DATA_SOURCES.md), [STUDY_DATA.md](STUDY_DATA.md) dan [AUDIO_CATALOG.md](AUDIO_CATALOG.md).

## Desain 1.5.0

Beranda memakai empat lanskap dari berkas desain sesuai jam setempat (05–10 pagi, 11–14 siang, 15–17 sore, selebihnya malam), jam/Hijriah, jadwal dan hitung mundur, tombol lokasi, azan, kiblat, pengaturan dan profil. Profil berisi nama panggilan dan lima ilustrasi avatar internal; tidak ada unggahan galeri, akun atau papan peringkat. Kuis dan progres tetap lokal.

Daftar qari berupa dua kolom foto/nama, dengan pilihan horizontal di beranda. Halaman qari menampilkan keterangan edisi rekaman yang bersumber dari penyedia. Biografi pribadi lengkap untuk semua qari belum tersedia; tanggal lahir/negara/riwayat pendidikan tidak diisi dengan dugaan.

Latihan hafalan menyediakan dua urutan audio: setiap ayat diulang N kali sebelum maju, atau seluruh rentang ayat diputar lalu diulang N kali. Nilai N dapat dipilih 1–20, dan ada pilihan seluruh surah. Contoh Al-Mulk 1–5 sebanyak tiga putaran menghasilkan tepat 15 item sebelum berhenti. Penilaian hafalan tetap catatan pribadi pengguna.

Banner beranda dapat digeser. Proyek backend Telegram tersedia di `backend/banner-bot`; belum deploy karena pengguna memilih menyiapkan proyek dahulu. Sebelum layanan diaktifkan, banner bawaan tetap berfungsi. Token bot hanya disimpan sebagai secret backend; tidak dimasukkan ke APK atau Git. Azan dijelaskan di [PRAYER.md](PRAYER.md).

Setiap edisi mempunyai gambar, indeks, cache dan penyimpanan offline yang sesuai. Panel bantuan melengkapi halaman asli tanpa menggantinya. Pengisian nama wajib dilakukan sebelum masuk beranda; profil yang sudah berisi nama dipertahankan. [Sumber dan batas gambar](MUSHAF_IMAGES.md).
