# Nuraya

**Versi terbaru: 1.3.1 · Android 8.0+**

[Unduh APK — 43,5 MB](https://raw.githubusercontent.com/Naufal00487/Nuraya/44b694346980eefdfbb2f6fd668ebfcf1c92bedb/Nuraya-1.3.1.apk) · [Catatan pembaruan](docs/UPDATE_1.3.1.md) · [Audit Quran dan audio](docs/AUDIT_1.3.1.md) · [Hasil 152 tes](docs/BUILD_VALIDATION.md)

Branch `main` memuat source lengkap versi 1.3.1 yang cocok dengan arsip rilis terverifikasi; tambahan pada README ini menyediakan tautan unduhan. APK memakai sertifikat yang sama agar dapat dipasang sebagai pembaruan.

Aplikasi Android untuk membaca, mendengarkan, dan mempelajari Al-Qur’an. Gratis, tanpa iklan, langganan atau akun wajib. Penyusun tidak mengambil keuntungan finansial dari aplikasi ini.

## Pembaruan 1.3.1

Lokasi otomatis saat penggunaan pertama dan audit ayat, bantuan baca, identitas rekaman serta data ganda. Pilihan qari yang tidak dikenal ditolak, riwayat audio per ayat dipulihkan, dan penjelasan hamzah wasal serta waqaf mu‘anaqah diperjelas. [Catatan pembaruan](docs/UPDATE_1.3.1.md).

Navigasi utama memakai empat tujuan berlabel: **Beranda, Al-Qur’an, Murottal, dan Ibadah**. Menu Beranda menyediakan tombol besar untuk AI bacaan, membaca Quran, murottal, hafalan/tajwid/kuis, Teman Belajar, Koleksi dan Pengaturan. Ukuran huruf dan area sentuh diperbesar; pengaturan ukuran Arab menyediakan tombol Kecilkan/Besarkan serta contoh bacaan. Skala tulisan sistem tetap dihormati.

**Seluruh fitur video dihapus**, termasuk YouTube, Shorts, katalog dan pencarian video. Riwayat video lama tidak lagi ditawarkan di Koleksi. AI bacaan, audio, Quran, penanda, unduhan, pelajaran tajwid, kuis dan progres tetap dipertahankan.

Menu **Ibadah** menampilkan jam, tanggal/tahun Hijriah, jadwal salat, hitung mundur salat berikutnya dan arah kiblat. Pada pemakaian pertama, aktifkan izin lokasi agar koordinat HP digunakan otomatis. Jika izin ditolak atau GPS belum tersedia, kota/manual tetap dapat dipilih. Nuraya tidak melacak lokasi di latar belakang; gunakan Ibadah → Lokasi untuk memperbarui posisi saat bepergian. Perhitungan berjalan offline dengan sudut Subuh 20°, Isya 18° dan Asar Syafii, serta koreksi menit yang dapat diatur. Jadwal merupakan perhitungan aplikasi, bukan unduhan jadwal resmi Kemenag. Kalender Umm al-Qura dapat berbeda dari rukyat setempat; koreksi −2 sampai +2 hari tersedia. Kompas mengikuti sensor perangkat jika tersedia; angka arah terhadap utara sejati tetap ditampilkan bila sensor tidak tersedia.

Al-Qur’an memiliki pilihan per ayat, per kata/tajwid, Latin/tajwid, Indonesia, Indonesia/tajwid, Madinah, Madinah/tajwid, hafalan 8 blok, Tikrar, waqaf/ibtida, Bahriyah/pojok dan terjemah tematik. Pilihan bantuan membaca tidak mengubah teks kanonis atau penomoran. Hafalan 8 blok membagi bacaan menjadi kelompok latihan; ini bukan klaim menyalin tata letak edisi cetak delapan blok. Latin Indonesia 6.236 ayat dari eQuran.id tersedia offline sebagai bantuan di samping teks Arab bertajwid; Latin tetap tanpa warna. Tanda waqaf bersumber ditambahkan sebagai anotasi tampilan hanya ketika ayat dan batas kata selaras, tanpa mengubah teks kanonis. Rincian perilaku dan status sumber gambar terdapat di [catatan pembaruan](docs/UPDATE_1.3.0.md).

## Memakai AI bacaan

1. Buka **Beranda → AI bacaan Al-Qur’an → Unduh & aktifkan AI**. Model Tarteel Whisper Quran sekitar **59,7 MB** diunduh sekali dan diperiksa SHA-256.
2. Setelah model siap, pilih surah dan ayat, lalu tekan **Mulai membaca**. Izinkan mikrofon ketika diminta. Tombol **Cek mikrofon · 5 detik** dapat digunakan untuk memeriksa suara masuk sebelum model siap.
3. Baca ayat, lalu tekan **Selesai membaca** untuk melihat perkiraan perbedaan kata. Setelah model tersimpan, pengenalan lokal dapat digunakan tanpa internet.

AI **belum menilai tajwid, makhraj, harakat atau panjang mad secara tervalidasi**. Model dapat salah mengenali atau melewatkan kata. Hasil sementara tidak dinilai sebagai kesalahan final. Kecepatan bergantung CPU dan memori HP; koreksi seketika tidak dijamin. Nuraya tidak menyimpan atau mengunggah rekaman/transkrip. Pilihan pengenal Android terpisah meminta persetujuan jika penyedia memproses online.

Teman Belajar mencari kutipan ayat, tafsir Kemenag dan pelajaran tajwid lokal. Hasil adalah rujukan sumber, bukan jawaban generatif, dan pertanyaan tidak dikirim ke server.

## Data yang dipertahankan

Tulisan Utsmani dengan font Amiri Quran offline, warna tajwid aktif secara default, arti per kata Indonesia/Inggris, tafsir Kemenag untuk 6.236 ayat, 137 pilihan rekaman pada daftar (138 ID tetap didukung untuk data lama) dan 17 qari per ayat, sinkronisasi kata Alafasy pada timing yang lolos pemeriksaan, hafalan/pengulangan, kuis dan progres lokal.

Pembuka 112 surah tetap terpisah tanpa nomor. Al-Fatihah 1:1 tetap basmalah; At-Taubah tidak mempunyai pembuka. Menambah mode membaca tidak mengubah ketentuan ini.

Gambar Madinah tersedia untuk 604 halaman: halaman 1, 2, 3 dan 604 disertakan offline, sedangkan halaman lain diunduh saat dibuka dan disimpan di cache. Mode Madinah/tajwid membuka bantuan teks ayat berwarna dari halaman yang dibaca; gambar halaman asli tetap tanpa warna tajwid. **Bahriyah menggunakan PDF lokal yang dipilih pengguna; berkas edisi Bahriyah belum disertakan.** Penampil tidak mengganti tata letak Bahriyah dengan Madinah.

Delapan foto qari asli dari profil Quran.com disertakan, dengan pemetaan identitas dan checksum sumber. Qari lain memakai inisial. Gambar mushaf dan wajah tidak dibuat dengan AI. Sumber serta cakupan lengkap dicatat di [catatan pembaruan](docs/UPDATE_1.3.0.md).

## Build dan verifikasi

```bash
cd /workspace/Nuraya
bash scripts/setup-android.sh
bash scripts/gradle.sh assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest
python3 scripts/verify-native-optimization.py
python3 scripts/import_quran.py --verify
python3 scripts/import_study.py --verify
python3 scripts/import_audio_timing.py --verify
python3 scripts/import_learning_index.py --verify
python3 scripts/import_latin.py --verify
python3 -m unittest discover -s scripts/tests -v
python3 -m unittest discover -s scripts -p 'test_import_study.py' -v
python3 -m unittest discover -s backend/tests -v
```

Toolchain di `/workspace/.tools`: JDK 17, Gradle 8.11.1, Android SDK 35, NDK 27.2.12479018 dan CMake 3.22.1. Whisper native dipin dalam sumber dengan lisensi MIT, untuk `arm64-v8a`, `armeabi-v7a` dan `x86_64`. Perhitungan salat memakai Adhan Java 1.2.1.

Setelah pemeriksaan lulus, `python3 scripts/package-artifacts.py` membuat APK, ZIP sumber, metadata dan SHA-256. Script menolak APK yang mendahului perubahan input build, aset APK yang berbeda dari sumber, atau runtime native tanpa optimasi. Keystore, cache build dan bobot model tidak masuk ZIP sumber.

APK memerlukan **Android 8.0 atau lebih baru**, memakai applicationId `com.nuraya.app`, versionCode **8**, dan sertifikat distribusi yang sama dengan versi terdahulu agar dapat dipasang sebagai pembaruan. Windows dapat dipakai untuk mengunduh dan memindahkan APK; pemasangan dilakukan pada Android.

Hasil build/tes yang benar-benar dijalankan dicatat di [BUILD_VALIDATION.md](docs/BUILD_VALIDATION.md). Pengujian host tidak membuktikan mikrofon, sensor kompas, latensi AI atau perilaku semua vendor HP. Laporan versi terdahulu tetap tersedia sebagai arsip; hasilnya tidak otomatis dinyatakan sebagai pengujian ulang 1.3.1.

## Rujukan

- [Perubahan 1.3.0 dan batas tiap mode](docs/UPDATE_1.3.0.md)
- [AI bacaan dan privasi](docs/RECITATION_ASSISTANT.md), [model dan konversi](docs/QURAN_AI_MODEL.md)
- [Teman Belajar lokal](docs/LOCAL_LEARNING_ASSISTANT.md)
- [Jam, Hijriah, salat dan kiblat](docs/PRAYER.md)
- [Gambar Madinah dan sumber halaman](docs/MUSHAF_IMAGES.md), [transliterasi Latin](docs/LATIN_READING.md)
- [Data Quran](docs/DATA_SOURCES.md), [data belajar](docs/STUDY_DATA.md), [audio](docs/AUDIO_CATALOG.md), [hafalan/kuis](docs/PRACTICE.md)
- [Validasi terkini](docs/BUILD_VALIDATION.md), [arsip 1.2.2](docs/BUILD_VALIDATION_1.2.2.md)
