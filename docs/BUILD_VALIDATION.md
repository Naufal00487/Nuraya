# Build dan validasi Nuraya 1.6.4

Paket `com.nuraya.app`, versionCode **18**, Android 8/API 26+, target API 35. Diperiksa `2026-11-15T20:53:34.364727+07:00`. [Perubahan](UPDATE_1.6.4.md).

## Hasil

**260 tes JVM/Compose lulus; 0 gagal, error atau dilewati.** Lint debug/release: **0 error/fatal**, masing-masing 79 / 79 peringatan. APK release dan APK instrumentasi berhasil dibangun; instrumentasi belum dijalankan pada HP.

```sh
bash scripts/gradle.sh testDebugUnitTest lintDebug assembleRelease lintRelease assembleDebugAndroidTest
python3 scripts/import_quran.py --verify
python3 scripts/import_printed_mushaf.py --verify
python3 scripts/import_semantic_index.py --verify
python3 scripts/verify-native-optimization.py --build-type release
python3 scripts/package-artifacts.py --build-type release
```

Bukti: [ringkasan](validation/1.6.4/summary.json), [JUnit](validation/1.6.4/junit), [log build](validation/1.6.4/gradle-build.log), [lint debug](validation/1.6.4/lint-debug.txt), [lint release](validation/1.6.4/lint-release.txt).

## Pemeriksaan perubahan

| Bagian | Yang diperiksa |
| --- | --- |
| Isi pengingat | Semua 100 ID unik dan rujukan ayat/rentang ada di korpus kanonis. Pemilihan 25 template mengikuti Ramadan, Jumat, per salat, umum; placeholder terisi. |
| Jadwal Quran | 1.000 rencana acak berada dalam jam siang dengan dua ID berbeda dan jeda tiga jam. Buka ulang mempertahankan rencana; pengiriman ulang, jam mundur, zona waktu lama, perubahan DST, keterlambatan Android dan larangan setelah 20.00 diuji. |
| Notifikasi dan alarm | Simulasi Android 13 dengan izin alarm tepat waktu ditolak tetap dapat menjadwalkan notifikasi Quran/salat. Audio default mati. Pengubahan jeda 5/10/15/30 menit mengubah jadwal. Menolak notifikasi membatalkan antrean tanpa menghapus pilihan salat. |
| Navigasi | Ketukan notifikasi membuka rujukan ayat yang benar atau Ibadah; rujukan salah ditolak. Dari Beranda, notifikasi Al-Ikhlas membuka halaman 604. |
| Kartu izin | Pengingat notifikasi paling sering tujuh hari sekali, termasuk jam mundur. Pengenalan lokasi tidak menumpuk dengan notifikasi. Kiblat tetap meminta izin setelah pengenalan awal ditutup. Aksi kartu tetap terlihat pada font 1,5×. Kartu offline bisa ditutup. |
| Tampilan | Banner otomatis dan geser manual, kiblat biru, tiga avatar, migrasi pilihan avatar lama tanpa menghapus nama, Tentang Nuraya dan kontrol pengingat diuji. Menu ayat tetap membuka terjemahan; gambar kitab dan peta ayat tidak berubah. |
| Regresi | Tes pencarian Indonesia, Quran/mushaf, 30 qari, hadis, AI, profil wajib, huruf besar, izin lokasi dan pemisahan audio pembaca/Murottal tetap lulus. |

Render native Robolectric, bukan foto HP: [kiblat](validation/1.6.4/qibla-blue.png), [pengingat](validation/1.6.4/reminder-settings.png), [kartu lokasi](validation/1.6.4/location-permission-card.png), [kartu notifikasi](validation/1.6.4/notification-permission-card.png), [font besar](validation/1.6.4/notification-permission-large-font.png), [offline](validation/1.6.4/offline-card.png), [sumber](validation/1.6.4/about-sources.png).

## Integritas dan distribusi

Teks kanonis Quran tidak berubah: SHA-256 `f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c`. Audit memverifikasi 114 surah/6.236 ayat, tujuh edisi 604 halaman, enam peta ayat, 42 PNG bundel serta 115 hash korpus indeks pencarian. Data Quran, gambar mushaf, qari dan timing sama dengan versi 1.6.3; audit suara daring tidak diulang karena pemetaan qari tidak berubah. Aset baru hanya tiga avatar asli dan dua JSON pengingat; strip avatar lama dihapus. Kelima berkas baru dibandingkan byte demi byte dengan unggahan. [Provenans dan checksum](sources/finishing-1.6.4.json).

APK **103,035,619 byte**, SHA-256 `1e036358841f48a0117fc1c9fd13c6c339c412b020fae9b5f8d38002ebf75ac1`. `apksigner verify` dan `zipalign -c -P 16 4` lulus; sertifikat distribusi `e2aa60317632bc6aea65f83c2247e54111941d0e0725875a2c9db3ffb93ac264` tetap sama. Native tiga ABI teroptimasi. Pengemasan membandingkan semua aset APK dengan source secara byte. Hanya APK terbaru disimpan pada tree GitHub utama.

[Tanda tangan](validation/1.6.4/apk-signature.txt), [alignment](validation/1.6.4/apk-alignment.txt), [metadata](validation/1.6.4/apk-metadata.txt), [native](validation/1.6.4/native-optimization.json).

## Batas pemeriksaan

Pengiriman notifikasi saat layar mati/Doze, suara azan, mikrofon, streaming, GPS dan kompas belum diuji pada HP fisik. Tes receiver memakai waktu/kondisi Android simulasi; ini tidak menjamin ketepatan pengiriman pada semua perangkat. Optimasi baterai, force-stop dan pengaturan notifikasi dapat membatasi pengingat. [Perilaku lengkap](REMINDERS.md). AI bacaan tetap memperkirakan kata dan bukan penilai tajwid/makhraj tervalidasi.
