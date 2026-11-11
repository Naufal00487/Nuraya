# Build dan validasi Nuraya 1.5.0

Diperiksa 11 November 2026. Paket `com.nuraya.app`, versionCode **10**, minimum Android 8/API 26, target API 35. [Perubahan dan batas fitur](UPDATE_1.5.0.md).

## Hasil akhir

**BUILD SUCCESSFUL. 177 tes JVM/Compose lulus, 0 gagal/error/dilewati.** Tujuh tes backend banner lulus. Lint: 0 error/fatal, 44 peringatan. APK aplikasi dan instrumentasi berhasil dibangun. Instrumentasi belum dijalankan pada HP/emulator dalam rilis ini.

```sh
bash scripts/gradle.sh assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue
node --test backend/banner-bot/worker.test.mjs
python3 scripts/audit-quran-corpus.py
python3 scripts/audit-reading-guidance.py
python3 scripts/audit-audio-identity.py
python3 scripts/import_audio_timing.py
python3 scripts/verify_qud_timing.py
python3 scripts/verify-native-optimization.py
```

Bukti: [ringkasan](validation/1.5.0/summary.json), [JUnit](validation/1.5.0/junit), [log build akhir](validation/1.5.0/gradle-build.log), [lint](validation/1.5.0/lint.txt), [tes bot](validation/1.5.0/banner-tests.log). Percobaan awal memerlukan pembaruan tes untuk menu mushaf baru, koreksi jadwal bawaan dua menit, urutan qualifier lanskap dan tunggu pemuatan gambar host. Setelah perbaikan, seluruh 177 tes dijalankan ulang dalam satu suite dan lulus; tidak ada tes yang dihapus/dilewati. Peringatan lint meliputi saran KTX/resource, penyimpanan, konteks aplikasi, dan false positive `ImplicitSamInstance` pada `Context.stopService(Intent)` yang memakai pencocokan komponen layanan.

## Cakupan revisi

- Izin/lokasi, pemilihan nama kecamatan tanpa mengarang alamat, fallback saat Geocoder tidak tersedia, serta keberlanjutan koreksi lokasi/jadwal.
- Lima pilihan pengingat terpisah; 10 menit sebelum dan tepat waktu salat; satu alarm aktif; peralihan hari; tidak ada Terbit; replay tidak menggandakan notifikasi; suara azan independen; pencabutan izin membatalkan pengingat.
- Splash tunggal di luar navigasi, gambar terang/gelap dan sampul benar-benar selesai dimuat, ikon pengaturan, dan batas navigasi tiga tombol setinggi 48 px.
- Mushaf asli, navigasi halaman/ayat/juz, simpan sumber offline dengan checksum, kontrol ketuk/dobel-ketuk, lanskap bergulir, ukuran teks 160%, panel terjemahan dan pencarian qari. Tes lama navigasi Quran/AI/hafalan/profil/koleksi tetap berjalan.
- Waktu kata tambahan memakai identitas/URL tiap qari dan indeks kata sumber yang sesuai. Audit memeriksa seluruh 114 bab × 11 qari tambahan, batas klip, interval tanpa overlap dan hash aset. Fallback sumber audio mempertahankan qari serta urutan pengulangan dan menghapus timing yang tidak sesuai.

Cuplikan render host yang diperiksa: [splash](validation/1.5.0/launch.png), [Quran terang](validation/1.5.0/quran-light.png), [Quran gelap](validation/1.5.0/quran-dark.png), [Ibadah](validation/1.5.0/prayer-header.png), [sakelar salat](validation/1.5.0/prayer-switches.png), [Murottal](validation/1.5.0/murottal-dark.png), [mushaf lanskap](validation/1.5.0/madinah-landscape.png), [huruf besar](validation/1.5.0/madinah-large-text.png), [panel arti](validation/1.5.0/madinah-translation.png), [pilihan qari](validation/1.5.0/madinah-qari.png). Ini render Compose/Robolectric, bukan tangkapan HP fisik.

## Quran, rekaman, dan sumber

Seluruh 6.236 ayat Arab, terjemahan Indonesia/Inggris dan catatan Indonesia cocok persis dengan snapshot sumber yang dipatok; 112 pembuka tetap terpisah, tanpa ID ayat ganda. SHA-256 Quran tidak berubah: `f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c`. [Audit corpus](audit/1.5.0/quran-corpus.json), [bantuan baca](audit/1.5.0/reading-guidance.json), [identitas metadata audio/foto](audit/1.5.0/audio-identity.json).

Sorotan per kata mencakup **12 dari 17 qari**, bukan seluruhnya. Sebelas tambahan mempunyai 68.528 klip ayat yang lolos pemetaan dan 68 fallback; Alafasy mempertahankan 6.142 ayat bertiming/94 fallback. Lima qari tanpa pasangan sumber terverifikasi tetap per ayat. [Batas dan audit](audit/1.5.0/word-timing.json).

Empat Al-Fatihah baru berhasil diunduh lengkap dan didekode; tujuh host lain dibatasi proxy cloud termasuk setelah retry dengan akses tambahan. Pemeriksaan metadata/batas klip tidak membuktikan identitas suara dengan mendengar seluruh rekaman. [Bukti sampel baru](audit/1.5.0/paired-audio-samples.json). Audit juga mempertahankan bukti 138 + 22 sampel dari rilis sebelumnya dengan tanggal aslinya, bukan mengklaim unduhan ulang semuanya.

Delapan belas aset terpasang cocok dengan byte gambar dokumen pengguna; [manifest desain](sources/revision-1.5.0-assets.json) mencatat hash dan pemetaan. Gambar halaman Quran kanonis tidak digambar ulang atau diwarnai secara dugaan. Sampul adalah ilustrasi navigasi; Bahriyah masih memakai PDF pengguna. Data timing QUD berlisensi CC BY 4.0 dengan atribusi di aplikasi dan berkas notice; hak rekaman tetap pada sumber/qari.

## APK dan batas perangkat

Ukuran **65.726.400 byte**. SHA-256 APK: `4e636620b32a74826928708affd33c09160446da1f440e5b93b14fc20834059e`.

`apksigner verify --verbose --print-certs` dan `zipalign -c -v -P 16 4` lulus. Sertifikat sama dengan distribusi sebelumnya: `e2aa60317632bc6aea65f83c2247e54111941d0e0725875a2c9db3ffb93ac264`. [Tanda tangan](validation/1.5.0/apk-signature.txt), [alignment](validation/1.5.0/apk-alignment.txt), [metadata](validation/1.5.0/apk-metadata.txt). Pengemasan memeriksa setiap aset APK terhadap source dan optimasi native tiga ABI.

GPS/Geocoder layanan nyata, mikrofon, kompas, suara azan, pengiriman alarm saat Doze dan streaming pada HP fisik belum diuji. AI lokal dipertahankan; hasilnya perkiraan kata, bukan penilaian tajwid/makhraj tervalidasi. Bot banner tetap belum di-deploy dan tidak ada pesan Telegram yang dikirim.
