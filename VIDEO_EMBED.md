# Pemutar YouTube dan pencarian video 1.2.1

Nuraya memakai pemutar resmi **YouTube IFrame Player API** dalam Android WebView. Video tidak diekstrak menjadi MP4, diunduh, disalin, atau diputar melalui API tidak resmi. `license: link-only` pada katalog tetap berarti Nuraya tidak memperoleh izin redistribusi konten; penayangan tertanam mengikuti pengaturan penerbit dan platform YouTube.

## Alur pengguna

- Pencarian memakai `VideoCatalog.searchWithGuidance`: kalimat kebutuhan belajar, sinonim, salah eja ringan, dan hasil berperingkat dari katalog lokal. Saran topik dapat diketuk; filter tingkat/format/topik tetap tersedia. Materi terkait diberi penjelasan ketika belum ada video khusus yang terverifikasi. Ini bukan pencarian seluruh YouTube.
- **Shorts** membuka feed vertikal dalam aplikasi; hanya halaman aktif memiliki WebView. Pencarian/filter tetap berlaku, judul dan kontrol berikutnya/sebelumnya berada di luar pemutar. Saat halaman ditinggalkan, pemutar dihancurkan sehingga audio berhenti. Pembatasan video ditampilkan di dalam feed dengan pilihan lewati/coba lagi, tanpa mengharuskan pindah ke YouTube.
- Detail video menyediakan **Tonton di aplikasi** untuk URL video, Shorts dan playlist yang valid. Pemutar baru dibuat setelah tombol dipilih. Tombol Putar di aplikasi atau kontrol resmi YouTube memulai video. Membuka Shorts adalah permintaan menonton: aplikasi mencoba pemutaran otomatis pada halaman aktif, dan menampilkan Putar bila YouTube menolaknya.
- Halaman kanal/kursus seperti `@Arabic101/courses` tidak diklaim sebagai video; ia tetap dibuka melalui **Buka sumber asli**. Tidak ada ID/playlist pengganti yang ditebak.
- Pemutar mempertahankan kontrol, branding, navigasi resmi, dukungan layar penuh, dan ukuran minimal 220 dp. `rel=0` hanya membatasi rekomendasi sesuai perilaku YouTube, bukan meniadakannya. Ikon/teks/kontrol Nuraya tidak menutupi konten atau iklan pemutar.
- YouTube tetap membutuhkan internet dan dapat menampilkan iklan atau memproses data pemutaran. Pemberitahuan ini ada di detail. Domain `youtube-nocookie.com` dipakai sebagai host iframe dan cookie pihak ketiga dinonaktifkan di WebView; ini bukan klaim bebas pengumpulan data.

## Integrasi dan batas keamanan

`YouTubeEmbed` menerima hanya HTTPS dan host YouTube yang tepat, tanpa kredensial atau port khusus. ID video harus tepat 11 karakter, ID playlist dibatasi karakter/ukuran, parameter duplikat ditolak. HTML hanya menyisipkan ID tervalidasi dan konstanta aplikasi. Halaman kanal, pencarian web dan URL berbahaya tidak menjadi embed.

Wrapper dimuat dengan `loadDataWithBaseURL` pada origin HTTPS identitas aplikasi `https://com.nuraya.app/embedded-video/`, parameter `origin` yang sama, serta kebijakan referrer `strict-origin-when-cross-origin`. Ini menyediakan konteks origin/Referer; tidak membuat klaim bahwa domain tersebut adalah situs yang dioperasikan Nuraya. Kesalahan identitas YouTube 153 tetap ditangani bila server/pemutar tidak menerimanya.

Tidak ada `addJavascriptInterface`, akses file/content lokal, mixed content HTTP, jendela popup otomatis, kredensial, atau pengabaian error TLS. Status pemutar dibaca dari wrapper tetap melalui `evaluateJavascript`; judul/sumber katalog tidak pernah diinterpolasi ke HTML. Navigasi top-level dari tautan pengguna dibuka di aplikasi/peramban sumber, sementara wrapper tetap di WebView. Audio murottal berhenti saat video mulai; pemutar dipause ketika aplikasi ke latar, audio murottal dimulai, atau layar ditinggalkan. WebView dilepas/dihancurkan saat detail ditutup. Riwayat video hanya dicatat setelah pemutar melaporkan status playing, bukan saat tautan/detail dibuka.

Error 100 (video tidak tersedia), 101/150 (penerbit menolak embed), 153 (identitas pemutar), jaringan dan waktu muat lama memiliki pesan serta pilihan muat ulang/sumber asli. Pembatasan penerbit tidak dicoba dilewati.

## Verifikasi

`YouTubeEmbedTest` menguji bentuk URL, playlist tanpa ID tebakan, penolakan URL/ID berbahaya, konfigurasi identitas, kontrol dan non-autoplay. Pengujian UI dan build final dicatat oleh laporan build. Test parser/configuration bukan bukti pemutaran YouTube sebenarnya.

Akses YouTube dan dokumentasi Google dibatasi HTTP 403 dari cloud. Upaya eskalasi dokumentasi tidak menghasilkan konten dan dibatalkan; tidak ada klaim halaman dokumentasi terbaru berhasil diunduh. Implementasi mengikuti antarmuka publik yang diketahui:

- https://developers.google.com/youtube/iframe_api_reference
- https://developers.google.com/youtube/player_parameters
- https://developers.google.com/youtube/terms/required-minimum-functionality

Pemutaran nyata, penerimaan origin/Referer, cookie/persetujuan, fullscreen, dan ketersediaan tiap video masih harus diuji pada perangkat dengan WebView terkini dan akses YouTube. Video yang pemiliknya melarang embedding tetap memerlukan sumber asli.

## Perbaikan berdasarkan laporan pengguna 1.2.0

Koneksi IFrame API (`onReady`) tidak lagi dianggap bukti bahwa video sudah siap. Wrapper memasang iframe berukuran viewport, memanggil `cuePlaylist`/`cueVideoById` secara eksplisit, dan menunggu perubahan state media. Pemutar yang berhenti saat memuat menampilkan pesan dan muat ulang; penolakan autoplay menyediakan tombol Putar di aplikasi. Seluruh kontrol resmi tetap terlihat dan tidak ditutup overlay.

Test HTML dijalankan dengan layanan IFrame terkontrol: ini memeriksa ukuran/command/status dan bukan klaim streaming YouTube. Test feed host memakai pemutar terkontrol untuk memeriksa pergantian halaman/disposal. Sentuhan/geser tepat di atas WebView pada tiap vendor Android tetap perlu diuji; tombol berikutnya/sebelumnya dan area judul menyediakan navigasi native. Bukti yang berhasil dijalankan dicatat di BUILD_VALIDATION.md.
