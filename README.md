# Nuraya

Source aplikasi Android Nuraya **1.5.0** untuk dibuka dan direvisi langsung. Tampilan baru mengikuti panduan desain Nuraya.

**[Unduh APK terbaru — 1.5.0](https://raw.githubusercontent.com/Naufal00487/Nuraya/HEAD/artifacts/Nuraya-1.5.0.apk)** · [Perubahan](docs/UPDATE_1.5.0.md) · [Audit Quran dan audio](docs/AUDIT_1.3.1.md) · [Validasi](docs/BUILD_VALIDATION.md)

Android 8.0 atau lebih baru. APK memakai paket dan sertifikat distribusi yang sama, sehingga dapat dipasang sebagai pembaruan. Folder `artifacts/` hanya menyediakan APK versi terbaru; source dapat diunduh melalui **Code → Download ZIP**.

## Revisi manual

1. Clone repo dengan `git clone https://github.com/Naufal00487/Nuraya.git`, atau unduh ZIP source.
2. Buka folder proyek yang berisi `settings.gradle.kts` melalui Android Studio, lalu tunggu Gradle Sync.
3. Ubah source, jalankan pengujian yang relevan, lalu build APK baru. APK yang disertakan adalah hasil rilis 1.5.0 dan tidak berubah otomatis ketika source diedit.

| Lokasi | Isi |
| --- | --- |
| `app/src/main/java/com/nuraya/app/` | Tampilan, Quran, murottal, AI, lokasi, salat dan penyimpanan |
| `app/src/main/assets/` | Data Quran, tafsir, tajwid dan metadata sumber |
| `app/src/test/` | Tes JVM/Compose |
| `app/src/androidTest/` | Tes instrumentasi Android |
| `app/build.gradle.kts` | Versi aplikasi dan konfigurasi Android |
| `scripts/` | Setup, build, impor/verifikasi data dan pengemasan |
| `docs/` | Dokumentasi fitur, sumber, audit dan bukti validasi terkini |
| `artifacts/` | APK 1.5.0 beserta checksum |

Toolchain: JDK 17, Android SDK 35, Gradle 8.11.1, NDK 27.2.12479018 dan CMake 3.22.1. Android Studio dapat memasang komponen SDK yang diperlukan. Build lokal memakai Gradle Wrapper:

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug
```

Di Windows, gunakan `gradlew.bat` menggantikan `./gradlew`. Build APK berada di `app/build/outputs/apk/debug/`. Keystore distribusi tidak disertakan dalam repo; build lokal memakai kunci pengembang masing-masing. Jangan mengubah data ayat tanpa memeriksa sumber dan menjalankan audit.

Untuk cloud Linux yang menyediakan toolchain proyek, tersedia `bash scripts/setup-android.sh` dan `bash scripts/gradle.sh`. Setelah build, `python3 scripts/package-artifacts.py` membuat paket di `artifacts/`; folder tersebut dikecualikan dari ZIP source agar APK tidak ikut terkemas kembali.

## Fitur dan batasnya

- Navigasi Beranda, Al-Qur’an, Murottal dan Ibadah dengan tombol berlabel. Video/Shorts sudah dihapus.
- Lokasi otomatis setelah izin Android disetujui; jam, Hijriah, jadwal/hitung mundur salat dan kiblat. Koordinat tersimpan di HP tanpa pelacakan latar belakang.
- Quran Utsmani, tajwid berwarna, arti per kata, Latin Indonesia, tafsir, Madinah, hafalan delapan kelompok, Tikrar, waqaf dan terjemah tematik. Bahriyah menggunakan PDF milik pengguna.
- 137 pilihan rekaman pada daftar; 138 ID lama tetap didukung. 134 foto tersedia: delapan dari Quran.com dan 126 dari dokumen penyusun; pemetaan menggunakan nama/ID edisi.
- Profil lokal, kuis pribadi, pengulangan per ayat/rentang (1–20 kali), serta pilihan pengingat masing-masing salat: 10 menit sebelumnya dan saat waktunya. Suara azan dapat dimatikan terpisah.
- Tampilan kitab Madinah: ketuk untuk menyembunyikan tombol, ketuk dua kali untuk lanskap, simpan halaman offline, lompat halaman/surah/juz, serta panel arti, tajwid dan pilihan qari.
- 17 qari audio per ayat; 12 memiliki data sorotan kata berpasangan dengan rekaman. Lima lainnya tetap per ayat. Rincian dan pengecualian ada di catatan rilis.
- Banner dapat digeser; [proyek bot Telegram](backend/banner-bot/README.md) sudah disiapkan, belum diaktifkan/deploy. Tidak memerlukan Hostinger.
- AI bacaan lokal, cek mikrofon, hafalan, kuis dan progres. AI memperkirakan kata, dapat keliru, dan belum menilai tajwid/makhraj secara tervalidasi. Model sekitar 59,7 MB diunduh sekali melalui aplikasi.

Rincian ada di [fitur dan batas tiap mode](docs/FEATURES.md), [AI bacaan](docs/RECITATION_ASSISTANT.md), [model AI](docs/QURAN_AI_MODEL.md), [lokasi/salat/kiblat](docs/PRAYER.md), [sumber Quran](docs/DATA_SOURCES.md), [audio](docs/AUDIO_CATALOG.md), dan [gambar mushaf](docs/MUSHAF_IMAGES.md).

Rilis 1.5.0 diperiksa dengan tes JVM/Compose, lint, audit data, dan pemeriksaan APK. Cakupan serta hasil akhir ada di [laporan validasi](docs/BUILD_VALIDATION.md). GPS, mikrofon, kompas dan suara azan belum diuji pada HP fisik dalam rilis ini. Perubahan manual berikutnya perlu diuji ulang.
