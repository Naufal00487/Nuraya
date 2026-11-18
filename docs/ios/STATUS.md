# Status Nuraya iOS — uji iPhone XR

Acuan: Android **1.7.1**, commit `7b9f23e0bfbc80a24b6265108deaa42785452063`. Proyek iOS berada di `ios/`, cabang `feature/nuraya-ios-xr`. Target minimum **iOS 16.0**, paket perangkat **ARM64**. Android tetap tersedia dan tidak diubah oleh port ini.

## Hasil build yang telah diperiksa

Diperiksa pada **18 November 2026 (WIB)**. Commit aplikasi `0c3dab2c0874bd1542ef837a486e59f2b3fcec48` berhasil pada [workflow macOS dan simulator iPhone XR](https://github.com/Naufal00487/Nuraya/actions/runs/44146205960): **19 pengujian lulus**, terdiri dari 10 pengujian core, empat pengujian platform, dan lima pengujian UI. Build simulator, build Release untuk iPhone, serta pemeriksaan isi bundle berhasil.

Lima uji UI mencakup profil/navigasi awal, pembukaan An-Nas pada halaman 604, pengaturan dan pergantian tema, penyimpanan hafalan setelah aplikasi dibuka ulang, serta pembersihan transkrip pencarian yang dibatalkan.

Paket `Nuraya-iOS-XR-unsigned.ipa` berukuran **100.171.923 byte (100,2 MB)**. Pemeriksaan ZIP, arsitektur ARM64, minimum iOS 16.0, sumber aset, serta ketiadaan model rekaman uji dan profil provisioning di bundle berhasil. Rincian tercatat di [VALIDATION.json](VALIDATION.json).

Unduh melalui [halaman unduh iOS di GitHub](https://github.com/Naufal00487/Nuraya/tree/downloads/nuraya-ios-preview), lalu ikuti [panduan Windows dan iPhone XR](UJI_IPHONE_XR.md).

| Bagian | Bukti yang diperiksa |
| --- | --- |
| Quran | Seluruh 114 surah dan 6.236 ayat; pemisahan basmalah pembuka pada 112 surah tanpa mengubah sumber; pengecualian Al-Fatihah, At-Taubah, dan An-Naml 27:30. |
| Halaman mushaf | Dua indeks dengan seluruh 6.236 rujukan divalidasi. Uji UI memilih An-Nas dan memeriksa halaman 604, kemudian menutup pembaca. |
| Isi aplikasi | 1.740 aset Android di dalam bundle diperiksa SHA-256; seluruhnya sama byte demi byte. Font, ikon aplikasi, data hadis dan suara notifikasi juga tersedia. |
| Qari dan timing | 28 pasangan identitas qari, foto, audio surah, dan audio ayat tersedia. Sebelas qari terkurasi memiliki timing kata yang cocok dengan identitas rekamannya untuk 114 surah. |
| Perilaku audio | Menutup bacaan menghentikan audio Quran tanpa menghentikan konteks murottal. Rentang ayat berhenti setelah pengulangan terbatas meskipun mode ulang murottal aktif. |
| Pencarian | Nama seluruh surah, angka terucap Indonesia, rujukan ayat, Ayat Kursi, salah ketik nama, dan topik pencarian diperiksa terhadap data lokal. |
| Belajar | 25 pelajaran, empat jenis latihan, penyimpanan sesi, XP tanpa penggandaan, hari aktif dan jadwal penilaian hafalan mandiri diperiksa. Uji UI membuktikan catatan tetap ada setelah aplikasi dibuka ulang. |
| AI lokal | Mesin C++ yang sama dengan Android diuji memakai PCM rekaman Quran yang sudah tersedia di proyek; menghasilkan teks Arab yang cocok dengan sebagian kata acuan. Keheningan tidak menghasilkan teks dan permintaan yang dibatalkan ditolak. Ini bukan pengujian mikrofon fisik. |
| Ibadah | Urutan waktu salat, arah kiblat Tangerang, dan tahun Hijriah diperiksa. |

## Screenshot simulator

Screenshot berikut berasal dari pengujian aplikasi, bukan gambar rancangan:

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

Paket IPA belum ditandatangani untuk akun Apple pengguna dan bukan rilis App Store. Pemasangan pribadi dilakukan melalui AltStore/AltServer di Windows. Lihat [panduan iPhone XR](UJI_IPHONE_XR.md).

Simulator belum membuktikan penandatanganan/pemasangan pada iPhone XR milik pengguna, mikrofon fisik, sensor kompas, kinerja/baterai AI pada chip A12, ketahanan audio pada layar terkunci, atau penerimaan notifikasi perangkat. Suara azan notifikasi menggunakan cuplikan 28 detik karena batas iOS; jadwal pemberitahuan perlu diisi ulang saat aplikasi dibuka.

Gambar Quran, sampul, foto dan ikon sumber dipertahankan. Antarmuka SwiftUI belum diklaim identik piksel demi piksel dengan Android; kontrol dan font antarmuka mengikuti iOS. Bahasa utama Indonesia, sedangkan penerjemahan seluruh antarmuka English masih belum lengkap. AI membandingkan perkiraan kata dan belum memvalidasi tajwid/makhraj.
