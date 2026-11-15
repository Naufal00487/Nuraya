# Build dan validasi Nuraya 1.6.3

Paket `com.nuraya.app`, versionCode **17**, Android 8/API 26+, target API 35. Diperiksa `2026-11-15T10:27:14.090186+07:00`. [Perubahan](UPDATE_1.6.3.md).

## Hasil

**238 tes JVM/Compose lulus; 0 gagal, error atau dilewati.** Lint debug/release: **0 error/fatal**, masing-masing 65 / 65 peringatan. APK release dan APK instrumentasi berhasil dibangun; instrumentasi belum dijalankan pada HP.

```sh
bash scripts/gradle.sh testDebugUnitTest lintDebug assembleRelease lintRelease assembleDebugAndroidTest
python3 scripts/import_quran.py --verify
python3 scripts/import_printed_mushaf.py --verify
python3 scripts/import_semantic_index.py --verify
python3 scripts/verify-native-optimization.py --build-type release
python3 scripts/package-artifacts.py --build-type release
```

Bukti: [ringkasan](validation/1.6.3/summary.json), [JUnit](validation/1.6.3/junit), [log build](validation/1.6.3/gradle-build.log), [lint debug](validation/1.6.3/lint-debug.txt), [lint release](validation/1.6.3/lint-release.txt).

## Pemeriksaan perubahan

| Bagian | Yang diperiksa |
| --- | --- |
| Pencarian surah | Semua 114 nama Latin/Arab kanonis dan nomor surah kembali ke surahnya sendiri. Alias Indonesia, variasi ejaan, angka lisan, nomor di luar batas, dan topik yang tidak boleh ditebak sebagai nama surah diuji. Rujukan memakai ayat kanonis tanpa modifikasi. |
| Dikte Indonesia | Adapter simulasi memverifikasi intent `id-ID`, alternatif hasil, teks sementara, final, provider offline/sistem, bahasa tidak tersedia, izin, pembatalan, callback terlambat, dan timeout. Tidak ada model bacaan Arab di jalur pencarian suara. |
| Navigasi pencarian | Melalui Beranda, panel suara menjelaskan bahasa Indonesia tanpa unduhan model Quran. `surat Yasim` menampilkan pilihan Yasin. `surah enam puluh tujuh ayat satu` membuka gambar Al-Mulk halaman 562. |
| Audio | Keluar dari pembaca membatalkan audio ayat yang menunggu koneksi. Pemanggilan siklus hidup MainActivity ketika aplikasi masuk latar belakang menghentikan audio pembaca, dan mempertahankan pilihan Murottal. Koneksi layanan terlambat tidak boleh memulihkan antrean pembaca yang telah ditutup. |
| Tema | Render host memeriksa gambar lanskap siang/malam, menu Quran biru, Pengaturan, pencarian suara dan hasil rujukan. Tes bilah sistem tetap memeriksa warna transparan dan kontras ikon. Regresi tata letak font besar, gambar kitab, 30 qari, hadis, izin, profil dan AI bacaan tetap lulus. |

Render native Robolectric, bukan foto HP: [Beranda siang](validation/1.6.3/home-day.png), [Beranda malam](validation/1.6.3/home-night.png), [Pengaturan](validation/1.6.3/settings-blue.png), [menu Quran](validation/1.6.3/quran-blue.png), [suara Indonesia](validation/1.6.3/indonesian-voice-search.png), [saran Yasin](validation/1.6.3/yasin-suggestion.png). Jam pada dua render lanskap dikendalikan lewat zona waktu pengujian.

## Integritas dan distribusi

Teks kanonis Quran tidak berubah: SHA-256 `f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c`. Verifikasi mencakup 114 surah/6.236 ayat, tujuh edisi 604 halaman, enam peta ayat, 42 PNG bundel serta 115 hash korpus indeks pencarian. Seluruh aset, termasuk timing, tafsir, gambar, qari dan model, tetap sama dengan source rilis sebelumnya. Audit suara daring 1.6.2 tidak diulang karena pemetaan qari tidak berubah.

APK **101,854,702 byte**, SHA-256 `d0d4c9c1f7da47a9763e98c92ac19323cbd7d0795369ee5b6520e64364415a70`. `apksigner verify` dan `zipalign -c -P 16 4` lulus; sertifikat distribusi `e2aa60317632bc6aea65f83c2247e54111941d0e0725875a2c9db3ffb93ac264` tetap sama. Native tiga ABI teroptimasi. Pengemasan membandingkan semua aset APK dengan source secara byte. Hanya APK terbaru disimpan pada tree GitHub utama.

[Tanda tangan](validation/1.6.3/apk-signature.txt), [alignment](validation/1.6.3/apk-alignment.txt), [metadata](validation/1.6.3/apk-metadata.txt), [native](validation/1.6.3/native-optimization.json).

## Batas pemeriksaan

Belum diuji pada HP fisik untuk hasil dikte mikrofon, streaming dan output suara, GPS, kompas, atau azan. Pengujian adapter suara memakai callback simulasi; tidak membuktikan akurasi pengenal bahasa Indonesia pada semua perangkat. Pengenal Android dapat membutuhkan internet; penjelasan ditampilkan sebelum perekaman. Uji audio antrean/lifecycle tidak membuktikan keluaran suara pada speaker HP. AI bacaan tetap memperkirakan kata dan bukan penilai tajwid/makhraj tervalidasi.
