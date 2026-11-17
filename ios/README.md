# Nuraya untuk iPhone

Port native SwiftUI dari Nuraya Android 1.7.1, dengan target minimum iOS 16.0. iPhone XR didukung oleh target perangkat ini. Kode Android dan APK tetap berada di repositori; proyek iOS berada di direktori `ios/` dan dikerjakan di cabang `feature/nuraya-ios-xr`.

Versi ini masih untuk pengujian. IPA yang dibuat workflow **belum ditandatangani**. Mengunduh IPA saja tidak memasang aplikasi ke iPhone. Kesamaan visual dan perilaku harus diperiksa dengan hasil pengujian; keberhasilan kompilasi saja tidak membuktikan seluruh fitur sama persis.

## Build tanpa Mac pribadi

1. Buka tab **Actions → Nuraya iOS** pada GitHub.
2. Pilih **Run workflow**, gunakan cabang `feature/nuraya-ios-xr`.
3. Setelah seluruh pemeriksaan berhasil, unduh artifact **Nuraya-iOS-XR-unsigned**.
4. Ekstrak ZIP untuk memperoleh `Nuraya-iOS-XR-unsigned.ipa`.

Workflow memakai runner macOS GitHub dan Xcode. Tidak memerlukan sertifikat Apple, kata sandi Apple ID, atau akun Developer di GitHub. File model dan rekaman uji hanya masuk target pengujian; model AI pengguna tetap diunduh sekali dari dalam aplikasi.

## Uji di iPhone XR menggunakan Windows

Gunakan [panduan resmi AltStore Classic untuk Windows](https://faq.altstore.io/altstore-classic/how-to-install-altstore-windows), termasuk versi iTunes/iCloud yang diminta panduan tersebut.

1. Pasang AltServer pada Windows dan sambungkan iPhone melalui USB. Pilih **Trust/Percayai** pada iPhone.
2. Dari AltServer, pasang AltStore ke iPhone. Masukkan Apple ID milik sendiri **hanya pada alat penandatanganan di komputer sendiri**; jangan kirim kredensial melalui chat atau memasukkannya ke repositori.
3. Ikuti petunjuk trust profil dan **Developer Mode** jika diminta iOS, kemudian buka AltStore.
4. Simpan IPA di Files pada iPhone. Di AltStore, buka **My Apps → +**, lalu pilih IPA Nuraya.
5. Setelah penandatanganan dan pemasangan selesai, buka Nuraya, buat profil, dan berikan izin yang dipilih untuk lokasi, mikrofon, atau notifikasi.

Akun Apple gratis biasanya memberi masa berlaku penandatanganan tujuh hari dan batas jumlah aplikasi aktif. Perbarui melalui AltStore/AltServer sebelum masa berlaku habis. AltServer perlu tersedia ketika melakukan penyegaran. Ketersediaan dan persyaratan alat mengikuti panduan resminya. Ubuntu dapat dipakai untuk mengubah sumber, tetapi panduan pemasangan pertama ini menggunakan Windows.

Ini jalur pengujian pribadi, bukan publikasi App Store. Publikasi App Store membutuhkan Apple Developer Program, identitas penerbit, konfigurasi signing, App Store Connect, kebijakan privasi, serta pemeriksaan perangkat dan proses review Apple.

## Build pada Mac/Xcode

```sh
brew install xcodegen cmake ninja ffmpeg
python3 ios/scripts/prepare_assets.py
python3 ios/scripts/prepare_test_fixtures.py
bash ios/scripts/prepare_macos.sh
cd ios/NurayaCore
swift test
cd ../..
bash ios/scripts/test_simulator.sh
bash ios/scripts/build_unsigned.sh
```

Untuk pengembangan lewat Xcode, buka `ios/Nuraya.xcodeproj` yang dihasilkan XcodeGen. Gunakan `project.yml` untuk perubahan konfigurasi yang akan disimpan di Git. Jangan menambahkan sertifikat atau profil provisioning ke Git. Versi Xcode acuan CI adalah Xcode 16.4; uji simulator memakai runtime iOS 18 dan jenis iPhone XR.

## Konten dan perilaku

- Pipeline menyalin 1.740 aset sumber Android tanpa mengubah byte gambar, teks Quran, data tajwid, atau rekaman sumber. Daftar hash disertakan dalam bundle.
- Corpus 114 surah/6.236 ayat, 11 cara membaca, tujuh sumber halaman cetak, arti per kata, latin, tafsir, dan indeks hadis memakai sumber Android.
- 28 qari dipasangkan secara eksplisit antara audio surah, audio ayat, nama, dan foto. Sorotan kata hanya memakai sebelas rekaman qari terkurasi yang memiliki timing sumber sesuai; qari lain memakai audio per ayat.
- Audio bacaan Quran berhenti ketika pembaca ditutup atau aplikasi meninggalkan keadaan aktif. Murottal memakai audio latar belakang, tombol media layar terkunci, dan kontrol headset.
- Pencarian suara Indonesia terpisah dari latihan bacaan Arab. Model AI lokal memakai Whisper Quran dan pemeriksaan hash model. Hasilnya merupakan perkiraan kata, bukan sertifikasi makhraj/tajwid.
- Profil, penanda, koleksi, dan progres disimpan di direktori pribadi aplikasi, dikecualikan dari backup. Rekaman dan transkrip latihan lokal tidak disimpan. Menghapus aplikasi dapat menghapus data dan unduhan.

## Perbedaan iOS yang perlu diketahui

Suara notifikasi khusus saat aplikasi tertutup dibatasi kurang dari 30 detik oleh iOS. Nuraya menggunakan cuplikan azan 28 detik, default mati. iOS juga membatasi jumlah permintaan notifikasi lokal yang menunggu; jadwal diisi ulang saat aplikasi dibuka, sehingga belum boleh dijanjikan alarm tanpa batas waktu jika aplikasi tidak pernah dibuka lagi. Izin notifikasi, suara perangkat, dan mode Fokus tetap berlaku.

Pengenal ucapan Apple bergantung dukungan bahasa/model pada perangkat. Jika model lokal Apple belum tersedia, pengguna dapat memilih layanan daring secara eksplisit atau mengetik. AI Whisper lokal merupakan jalur terpisah dan tidak mengunggah suara.

Uji simulator tidak membuktikan kualitas mikrofon fisik, akurasi sensor kompas, konsumsi baterai, ketahanan audio latar belakang, maupun kinerja AI pada chip A12 iPhone XR. Hal-hal tersebut harus diperiksa di perangkat sebelum rilis umum.
