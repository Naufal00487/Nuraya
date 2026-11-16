# Build dan validasi Nuraya 1.7.1

Paket `com.nuraya.app`, versionCode **20**, Android 8/API 26+, target API 35. Diperiksa `2026-11-16T12:57:25.700552+07:00`. [Perubahan](UPDATE_1.7.1.md).

## Hasil

**284 tes JVM/Compose lulus; 0 gagal, error atau dilewati.** Lint debug/release: **0 error/fatal**, masing-masing 79 / 79 peringatan. APK release dan APK instrumentasi berhasil dibangun; instrumentasi belum dijalankan pada HP.

```sh
bash scripts/gradle.sh testDebugUnitTest --no-parallel --max-workers=2
bash scripts/gradle.sh lintDebug assembleRelease lintRelease assembleDebugAndroidTest --no-parallel --max-workers=2
python3 scripts/import_quran.py --verify
python3 scripts/import_printed_mushaf.py --verify
python3 scripts/import_semantic_index.py --verify
python3 scripts/verify_curated_reciters.py
python3 scripts/verify-native-optimization.py --build-type release
python3 scripts/package-artifacts.py --build-type release
```

Bukti: [ringkasan](validation/1.7.1/summary.json), [JUnit](validation/1.7.1/junit), [log build](validation/1.7.1/gradle-build.log), [lint debug](validation/1.7.1/lint-debug.txt), [lint release](validation/1.7.1/lint-release.txt).

## Pemeriksaan perubahan

| Bagian | Yang diperiksa |
| --- | --- |
| Qari dan foto | Tepat 28 pilihan surah dan ayat; ID Ali Jaber/Abdullah Al-Matrood dikecualikan. Semua pasangan ID memakai foto/nama yang sama. 28 gambar terdekode dan hash/jumlah byte cocok, termasuk 20 gambar dokumen asli. |
| Pemutar | Membuka pemutar dari mini player menghasilkan layar penuh. Mengecilkan mempertahankan state audio; Hentikan audio menghapus sesi. Waktu tampil benar; durasi kosong menonaktifkan seek. Pesan kegagalan dan Coba lagi tetap ada. |
| Integrasi | Dengarkan membuka pemutar; Baca & dengarkan dari awal mempertahankan surah 55 dan pemetaan Ahmed Al-Ajmi ke ID ayatnya, dengan rentang 1–78. |
| Tampilan | Render native tema biru pada 411×891 dan gelap 320×640 dengan font 1,6×; kontrol dapat dijangkau tanpa melampaui lebar layar. Pilihan kecepatan dapat dibuka. Tidak ada kredit foto di player; kredit Naufal dan sumber foto tersedia di panel atribusi. |
| Regresi | Seluruh tes lama Quran/mushaf, pencarian Indonesia, hadis, AI, belajar/kuis, profil, pengingat, lokasi dan pemisahan audio pembaca/Murottal tetap lulus. |

Tes navigasi lama diperbarui untuk pilihan qari yang masih tersedia; halaman 562 memakai fixture asli terverifikasi, sehingga hasil tidak bergantung pada unduhan langsung. Kondisi prompt notifikasi dibuat eksplisit dan menu halaman dibuka melalui aksi aksesibilitas untuk menghindari persaingan gesture dengan pemuatan bitmap. Tidak ada perubahan produksi pada fitur-fitur tersebut.

Render native Robolectric, bukan foto HP: [pemutar biru](validation/1.7.1/player-blue.png), [font besar gelap setelah digulir](validation/1.7.1/player-dark-large-font.png), [pesan gagal](validation/1.7.1/player-error.png), [atribusi desain](validation/1.7.1/design-attribution.png).

## Data dan audio

Teks kanonis Quran tidak berubah: SHA-256 `f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c`. Audit memverifikasi 114 surah/6.236 ayat, tujuh edisi 604 halaman, enam peta ayat, 42 PNG bundel serta 115 hash korpus indeks pencarian. Tidak ada perubahan teks, gambar mushaf, URL audio atau timing.

[201/201 sampel HTTP Range](validation/1.7.1/audio-probes.json) berisi header MP3: enam rujukan per 28 qari dan tiga rekaman pasangan per 11 qari bertiming. Nomor global diambil dari data Quran kanonis. Audit ini menguji respons sebagian berkas, bukan dekode penuh, mendengar suara, atau menjalankan pemutar Android. [Script audit](validation/1.7.1/probe-audio.py). Pemetaan foto mengikuti label dokumen penyusun, bukan pengenalan/verifikasi wajah independen.

## Distribusi

APK **104,123,507 byte**, SHA-256 `793c09d7c8b5c0abb7ca5c1103141522533373efdd5fee750a01961acf3c50c6`. `apksigner verify` dan `zipalign -c -P 16 4` lulus; sertifikat distribusi `e2aa60317632bc6aea65f83c2247e54111941d0e0725875a2c9db3ffb93ac264` tetap sama. Native tiga ABI teroptimasi. Pengemasan membandingkan semua aset APK dengan source secara byte. Hanya APK terbaru disimpan pada tree GitHub utama.

[Tanda tangan](validation/1.7.1/apk-signature.txt), [alignment](validation/1.7.1/apk-alignment.txt), [metadata](validation/1.7.1/apk-metadata.txt), [native](validation/1.7.1/native-optimization.json).

## Batas pemeriksaan

Tes interaksi memakai Android host Robolectric dengan service audio inert. Instalasi/pembaruan, suara qari, seek/kecepatan pada rekaman nyata, mikrofon, GPS, kompas, azan dan pengiriman notifikasi saat layar mati belum diuji pada HP fisik dalam rilis ini. DDMusic 2.12.2 diperiksa statis; tidak dijalankan dan tidak ada kode/asetnya yang dibundel. Pelajaran, AI dan kemampuan penilaian bacaannya tidak diubah: AI masih memperkirakan kata, bukan penilai tajwid/makhraj tervalidasi.
