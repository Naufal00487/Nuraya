# Nuraya 1.2.1 — perbaikan video, Shorts dan mikrofon

[Unduh APK 1.2.1 — 41,3 MB](Nuraya-1.2.1.apk?raw=1) · [Kode sumber](Nuraya-1.2.1-source.zip?raw=1)

Android 8.0 atau lebih baru. Pasang sebagai pembaruan; tidak perlu menghapus aplikasi lama. Application ID dan sertifikat tetap sama, versionCode naik menjadi 5. Model AI 59,7 MB tidak berubah dan tidak perlu diunduh ulang jika sudah tersedia.

- **Video → Shorts** membuka feed vertikal di aplikasi. Geser atau gunakan tombol berikutnya/sebelumnya. Hanya video aktif yang mempunyai pemutar. Video yang dibatasi penerbit dapat dilewati di dalam aplikasi.
- **Pemutar video** memperbaiki ukuran iframe dan inisialisasi playlist. Status API terhubung tidak lagi langsung dianggap video siap. Putar/Muat ulang tersedia di aplikasi, termasuk ketika autoplay ditolak.
- **AI bacaan → Cek mikrofon · 5 detik** memeriksa suara masuk tanpa menunggu model. Meter audio, pesan input dibisukan/tanpa frame dan hitungan proses AI ditambahkan.
- Kernel AI dalam APK sekarang dioptimalkan. Probe host yang sama berubah dari 77,058 menjadi 15,665 detik; ini bukan benchmark HP atau jaminan real-time. Hasil AI masih perkiraan kata, bukan penilaian tajwid/makhraj.

115 tes aplikasi dan tiga skenario wrapper browser terkontrol lulus. APK/test APK berhasil dipasang di emulator software, tetapi pengujian Android terhenti oleh startup ANR sebelum test selesai. **Streaming YouTube dan mikrofon fisik belum terverifikasi oleh pengembang**; tidak ada klaim semua perangkat sudah bebas bug. Pembatasan video oleh YouTube/publisher tetap berlaku. Test browser memakai IFrame API lokal terkontrol, bukan streaming nyata.

[Laporan dan batas pengujian](BUILD_VALIDATION.md) · [Ringkasan mesin](validation/1.2.1/summary.json) · [SHA-256](SHA256SUMS) · [Metadata sumber](build-info.json)

Nuraya gratis; penyusun tidak mengambil keuntungan finansial. Nuraya tidak memasang iklan sendiri, sedangkan YouTube dapat menampilkan iklan platform. Teks Quran, tafsir, qari, hafalan/kuis dan data versi sebelumnya dipertahankan.

Versi terdahulu tetap tersedia: [1.2.0](Nuraya-1.2.0.apk?raw=1), [1.1.0](Nuraya-1.1.0.apk?raw=1), [1.0.1](Nuraya-1.0.1.apk?raw=1).
