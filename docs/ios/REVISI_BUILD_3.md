# Revisi iOS 1.7.1 build 3 — audio Murottal di latar belakang

Laporan pengguna pada 24 November 2026: audio dari tab Murottal berhenti ketika kembali ke Home, meskipun aplikasi tidak ditutup paksa.

## Penyebab dan perubahan

`RootView` menghentikan layanan dikte saat aplikasi masuk latar belakang. Sebelumnya, `SpeechService.stop()` selalu memanggil `AVAudioSession.setActive(false)`, termasuk ketika mikrofon tidak pernah dinyalakan. Karena AVAudioSession digunakan bersama oleh seluruh aplikasi, pembersihan layanan dikte mematikan sesi yang sedang dipakai pemutar Murottal. Pembersihan AI lokal memiliki pola yang sama.

Sekarang pengelola sesi audio memberikan identitas sesi kepada pemutar, dikte, dan AI lokal saat masing-masing mengaktifkan audio. Layanan hanya boleh menonaktifkan sesi yang masih dimilikinya. Pembersihan layanan yang tidak merekam, pembersihan berulang, dan hasil lama setelah pemilik sesi berganti tidak lagi mematikan Murottal. Tombol putar juga mengaktifkan kembali kategori playback ketika pengguna melanjutkan audio setelah interupsi sistem.

Perilaku bacaan tetap dibedakan:

- Audio dari **Murottal** boleh terus berjalan ketika pengguna kembali ke Home atau mengunci layar.
- Audio dari **pembaca Al-Qur’an** berhenti ketika pembaca ditutup atau aplikasi meninggalkan latar depan.
- Audio yang dijeda pengguna tetap dijeda; kembali ke aplikasi tidak memicu pemutaran otomatis.
- Menutup paksa proses aplikasi tetap berbeda dari kembali ke Home.

## Pengujian

Tiga tes platform memeriksa bahwa layanan dikte/AI yang diam tidak menonaktifkan sesi Murottal, pembersihan pemilik lama tidak menonaktifkan pemutar yang baru, dan audio pembaca Quran tetap berhenti ketika masuk latar belakang.

Tes UI memutar rekaman Al-Fatihah asli oleh Mishary Rashid Alafasy melalui tab Murottal, menekan tombol Home simulator, menunggu delapan detik, lalu membuka kembali aplikasi dan memeriksa pertambahan waktu AVPlayer. Tes kemudian menjeda audio, mengulangi Home selama empat detik, dan memeriksa posisi tidak berjalan lagi. Tes menggunakan rekaman Android yang sudah ada dalam folder pengujian. Rekaman hanya disalin ke penyimpanan simulator untuk tes; tidak masuk ke paket aplikasi pengguna.

[Workflow build 3](https://github.com/Naufal00487/Nuraya/actions/runs/45427575713) selesai berhasil: **30 tes lulus** (11 core, delapan platform, sebelas UI). Screenshot menunjukkan posisi 0:03 sebelum Home dan 0:15 setelah kembali, termasuk delapan detik menunggu di Home. Build Release untuk iPhone dan pemeriksaan isi IPA juga lulus; rekaman uji tidak ikut dikemas.

Checksum IPA dan screenshot yang sudah diperiksa dicatat di [STATUS.md](STATUS.md) dan [VALIDATION.json](VALIDATION.json). Pengujian Home pada simulator tidak menggantikan pemeriksaan suara, layar terkunci, Bluetooth atau panggilan masuk pada iPhone fisik.
