# Status Nuraya iOS — uji iPhone XR

Acuan: Android **1.7.1**, commit `7b9f23e0bfbc80a24b6265108deaa42785452063`. Proyek iOS berada di `ios/`, cabang `feature/nuraya-ios-xr`. Target minimum **iOS 16.0**, paket perangkat **ARM64**. Android tetap tersedia dan tidak diubah oleh port ini.

## Hasil build yang telah diperiksa

Diperiksa pada **27 November 2026 (WIB)**. Commit aplikasi `745a3752443a2ca9b8eb9000ce481818d5707d1a` berhasil pada [workflow macOS dan simulator iPhone XR](https://github.com/Naufal00487/Nuraya/actions/runs/45947277501): **35 pengujian lulus** (11 core, 13 platform, 11 UI). Build simulator, Release untuk iPhone, dan pemeriksaan isi bundle berhasil.

Build **1.7.1 (4)** memperbaiki jeda tambahan ketika audio pembaca Quran berganti ayat. Ayat berurutan dari rekaman surah yang sama menggunakan satu item pemutar tanpa pause/seek di tiap ayat; hanya informasi ayat dan sorotan yang berubah. Audio per ayat menyiapkan dua berkas berikutnya melalui antrean native. Jeda napas/waqaf asli tetap dipertahankan. Rincian ada pada [REVISI_BUILD_4.md](REVISI_BUILD_4.md).

Tes dengan rekaman Al-Fatihah Alafasy membuktikan player item tetap sama dan jam audio maju tanpa pause atau seek di batas ayat 1–3. Tes Al-Ikhlas Muhammad Jibreel membuktikan berkas ayat berikutnya sudah tersedia dalam antrean dan dipakai tanpa pemuatan ulang. Tes juga memeriksa awal/akhir rentang, pause saat persiapan, pengulangan, dan 1.254 pasangan qari/surah bertiming.

Perbaikan [build 3](REVISI_BUILD_3.md) untuk Murottal saat Home serta [build 2](REVISI_BUILD_2.md) untuk perpindahan tab, header, splash, Koleksi Saya dan lanjut baca tetap tercakup dalam pengujian.

Paket `Nuraya-iOS-XR-unsigned.ipa` berukuran **100.307.229 byte (100,3 MB)**. ZIP, ARM64, minimum iOS 16.0, 1.740 aset sumber, mode audio latar belakang, serta ketiadaan model/rekaman uji dan profil provisioning di bundle sudah diperiksa. Rincian tercatat di [VALIDATION.json](VALIDATION.json).

Unduh melalui [halaman unduh iOS di GitHub](https://github.com/Naufal00487/Nuraya/tree/downloads/nuraya-ios-preview), lalu ikuti [panduan Ubuntu/Windows dan iPhone XR](UJI_IPHONE_XR.md).

| Bagian | Bukti yang diperiksa |
| --- | --- |
| Quran | Seluruh 114 surah dan 6.236 ayat; pemisahan basmalah pembuka pada 112 surah tanpa mengubah sumber; pengecualian Al-Fatihah, At-Taubah, dan An-Naml 27:30. |
| Halaman mushaf | Dua indeks dengan seluruh 6.236 rujukan divalidasi. Uji UI memilih An-Nas dan memeriksa halaman 604, kemudian menutup pembaca. |
| Isi aplikasi | 1.740 aset Android di dalam bundle diperiksa SHA-256; seluruhnya sama byte demi byte. Font, ikon aplikasi, data hadis dan suara notifikasi juga tersedia. |
| Qari dan timing | 28 pasangan qari/foto/sumber audio. Dari 1.254 pasangan qari/surah bertiming (11 × 114), 1.231 memakai rekaman surah secara berkesinambungan. Sebanyak 23 pasangan dengan timing tidak layak memakai sumber per ayat dari qari yang sama tanpa sorotan kata. |
| Perilaku audio | Ayat berurutan bertiming tidak mengganti player item, pause atau seek. Berkas per ayat memakai antrean yang disiapkan sebelumnya. Rentang dan pengulangan tetap terbatas. Murottal maju saat Home; audio yang dijeda tetap diam; audio pembaca berhenti saat ditutup/masuk latar belakang. |
| Pencarian | Nama seluruh surah, angka terucap Indonesia, rujukan ayat, Ayat Kursi, salah ketik nama, dan topik pencarian diperiksa terhadap data lokal. |
| Belajar | 25 pelajaran, empat jenis latihan, penyimpanan sesi, XP tanpa penggandaan, hari aktif dan jadwal penilaian hafalan mandiri diperiksa. Uji UI membuktikan catatan tetap ada setelah aplikasi dibuka ulang. |
| AI lokal | Mesin C++ yang sama dengan Android diuji memakai PCM rekaman Quran yang sudah tersedia di proyek; menghasilkan teks Arab yang cocok dengan sebagian kata acuan. Keheningan tidak menghasilkan teks dan permintaan yang dibatalkan ditolak. Ini bukan pengujian mikrofon fisik. |
| Ibadah | Urutan waktu salat, arah kiblat Tangerang, dan tahun Hijriah diperiksa. |

## Screenshot simulator

Screenshot berikut berasal dari pengujian build 4, bukan gambar rancangan:

- Murottal [sebelum Home](screenshots/murottal-before-home.png) dan [setelah kembali](screenshots/murottal-after-home.png), termasuk delapan detik menunggu di Home.

- [Header Al-Qur’an](screenshots/quran-header.png), [header Murottal](screenshots/murottal-header.png), dan [Murottal setelah berpindah dari pilihan surah](screenshots/tab-switch-murottal.png).
- [Splash dua maskot](screenshots/splash.png).
- [Koleksi kosong](screenshots/collection-empty.png), [penanda tersimpan](screenshots/collection-bookmark.png), dan [unduhan](screenshots/collection-downloads.png).
- [Pembaca per ayat](screenshots/quran-per-ayat.png).

- [Beranda](screenshots/home.png) dan [tema biru gelap](screenshots/home-dark.png).
- [Mushaf Madinah halaman 604](screenshots/madinah-604.png).
- [Jalur belajar](screenshots/learning.png), [hafalan mandiri](screenshots/memorization.png), dan [progres tersimpan](screenshots/progress.png).
- [Pengaturan](screenshots/settings.png), [pengaturan gelap](screenshots/settings-dark.png), dan [pencarian suara Indonesia](screenshots/voice-search.png).

## Integritas Android

SHA-256 APK Android yang dipertahankan:

```text
793c09d7c8b5c0abb7ca5c1103141522533373efdd5fee750a01961acf3c50c6  artifacts/Nuraya-1.7.1.apk
```

## Batas hasil pengujian

Paket IPA belum ditandatangani untuk akun Apple pengguna dan bukan rilis App Store. Pemasangan pribadi dilakukan melalui iloader di Ubuntu atau AltStore/AltServer di Windows. Lihat [panduan iPhone XR](UJI_IPHONE_XR.md).

Simulator belum membuktikan penandatanganan/pemasangan pada iPhone XR milik pengguna, mikrofon fisik, sensor kompas, kinerja/baterai AI pada chip A12, ketahanan audio pada layar terkunci, atau penerimaan notifikasi perangkat. Suara azan notifikasi menggunakan cuplikan 28 detik karena batas iOS; jadwal pemberitahuan perlu diisi ulang saat aplikasi dibuka.

Gambar Quran, sampul, foto dan ikon sumber dipertahankan. Antarmuka SwiftUI belum diklaim identik piksel demi piksel dengan Android; kontrol dan font antarmuka mengikuti iOS. Bahasa utama Indonesia, sedangkan penerjemahan seluruh antarmuka English masih belum lengkap. AI membandingkan perkiraan kata dan belum memvalidasi tajwid/makhraj.
