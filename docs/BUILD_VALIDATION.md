# Build dan validasi Nuraya 1.6.2

Paket `com.nuraya.app`, versionCode **16**, Android 8/API 26+, target API 35. Diperiksa `2026-11-15T04:53:35.465558+07:00`. [Perubahan](UPDATE_1.6.2.md).

## Hasil

**220 tes JVM/Compose lulus; 0 gagal, error atau dilewati.** Lint debug/release: **0 error/fatal**; masing-masing 65 / 65 peringatan. APK release dan APK instrumentasi berhasil dibangun. Instrumentasi belum dijalankan pada HP.

```sh
bash scripts/gradle.sh testDebugUnitTest lintDebug assembleRelease lintRelease assembleDebugAndroidTest
python3 scripts/verify_curated_reciters.py
python3 scripts/import_hadith.py --verify
python3 scripts/import_quran.py --verify
python3 scripts/import_printed_mushaf.py --verify
python3 scripts/import_semantic_index.py --verify
python3 scripts/verify-native-optimization.py --build-type release
python3 scripts/package-artifacts.py --build-type release
```

Bukti: [ringkasan](validation/1.6.2/summary.json), [JUnit lengkap](validation/1.6.2/junit), [log build](validation/1.6.2/gradle-build.log), [lint debug](validation/1.6.2/lint-debug.txt), [lint release](validation/1.6.2/lint-release.txt).

## Cakupan

| Bagian | Pemeriksaan |
| --- | --- |
| Qari | 30 identitas berbeda, pasangan ID surah/ayat eksplisit, metadata 114 surah tiap pilihan. Penomoran URL EveryAyah diuji pada semua 6.236 ayat. Memilih Abdul Basit di Murottal menyimpan ID suara Quran `ar.abdulsamad`. |
| Audio daring | 120 sampel Al-Fatihah/EveryAyah dan 45 klip Islamic Network berhasil didekode; 30 berkas Al-Fatihah berbeda hash. Direktori EveryAyah mencantumkan 6.236 berkas tiap qari. Sebelas qari tetap memakai timing rekaman sumber; qari lain mengikuti ayat. |
| Hadis | Nomor persis/pecahan, filter kitab, frasa Indonesia/Arab, pagination tanpa duplikasi, teks kosong, input tidak dikenal dan teks Arab asli diperiksa. Pembaca teks lengkap terbuka melalui Cari Hadis di Beranda. Indeks diperiksa terhadap setiap entri. |
| Tampilan | Beranda, Settings, Ibadah dan pemilih qari dirender dengan grafis native Robolectric; tema gelap pada skala font 1,3 serta pengujian ukuran besar lama. Tanggal tidak menimpa strip waktu, baris Settings tidak bertumpuk, tombol tetap terjangkau. |
| Regresi | Mushaf layar penuh, halaman/surah jauh, pemilihan edisi, sorotan, arti, hafalan/lintas surah, AI lifecycle/mikrofon simulasi, izin lokasi, profil wajib, kuis dan penyimpanan tetap diuji. Tes lanjut baca menunggu penyimpanan Room selesai setelah debounce sebelum kembali. |

Render host, bukan foto HP: [Beranda](validation/1.6.2/home-light.png), [Pengaturan](validation/1.6.2/settings-dark-large.png), [Ibadah](validation/1.6.2/prayer-dark-large.png), [pemilih qari](validation/1.6.2/qari-picker.png), [hasil hadis](validation/1.6.2/hadith-results.png), [pembaca hadis](validation/1.6.2/hadith-reader.png).

## Integritas dan distribusi

Teks kanonis Quran tidak berubah: SHA-256 `f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c`. Seluruh 114 surah, 6.236 ayat, tujuh indeks edisi 604 halaman, enam peta ayat dan 42 PNG bundel tetap terverifikasi. Gambar halaman, tafsir, latin, tajwid, timing, avatar serta banner tidak diubah. Seluruh 115 hash sumber indeks pencarian makna tetap cocok.

APK **101,838,318 byte**, SHA-256 `a6502519ed5d75a7709cea2cba2f1cbc91ac58b5994c67938376f32e15d6a750`. `apksigner verify` dan `zipalign -c -P 16 4` lulus. Sertifikat distribusi tetap `e2aa60317632bc6aea65f83c2247e54111941d0e0725875a2c9db3ffb93ac264`. Build release tidak debuggable; native tiga ABI tetap teroptimasi. Pengemasan membandingkan setiap aset APK dengan source secara byte. Folder artifacts hanya memuat APK terbaru pada tree GitHub.

[Tanda tangan](validation/1.6.2/apk-signature.txt), [alignment](validation/1.6.2/apk-alignment.txt), [metadata](validation/1.6.2/apk-metadata.txt), [native](validation/1.6.2/native-optimization.json).

## Batas pemeriksaan

Belum diuji pada HP fisik untuk streaming, output suara, mikrofon/AI, GPS, kompas atau azan. Pemetaan nama qari mengikuti metadata penyedia; decode sampel tidak mengautentikasi suara lewat pendengaran manusia atau menilai tajwid semua rekaman. Foto terverifikasi tersedia untuk delapan qari, sisanya memakai inisial. Ketersediaan server dapat berubah. Sumber hadis mempunyai entri tanpa terjemahan; aplikasi tidak melengkapinya dengan teks rekaan. AI bacaan tetap merupakan perkiraan kata, bukan penilaian tajwid/makhraj yang tervalidasi.
