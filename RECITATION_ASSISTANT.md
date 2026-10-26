# AI bacaan Nuraya 1.2.0

Menu **AI bacaan Al-Qur’an** berada dekat bagian atas Beranda. Mode utama memakai model pengenal suara Whisper melalui mesin native `whisper.cpp`, dijalankan pada HP. Mode **Pengenal Android** tetap tersedia sebagai pilihan terpisah. Ini adalah pengenalan suara dan pencocokan kata yang dapat keliru, **bukan penilai tajwid, makhraj, harakat, atau panjang mad**.

## Alur pengguna dan model

1. Buka **Beranda → AI bacaan Al-Qur’an**.
2. Pilih **Unduh & aktifkan AI**. Model berukuran **59.707.625 byte** (sekitar 59,7 MB), diunduh sekali. Tidak perlu akun atau biaya layanan. Unduhan model memerlukan internet; sesudahnya inferensi berjalan lokal tanpa mengirim audio ke server.
3. Setelah model lolos checksum dan ditandai siap, pilih surah/ayat lalu tekan **Mulai membaca**. Izin mikrofon diminta saat tindakan ini, bukan ketika membuka halaman.
4. Kata yang cocok dapat disorot selama membaca. Tekan **Selesai membaca** untuk hasil akhir dan daftar kata yang perlu diperiksa.

File model mempunyai URL GitHub HTTPS yang dipin ke commit, ukuran, dan SHA-256 tetap di `QuranAiModel.spec`. Aplikasi memeriksa seluruh isi sebelum status READY. Unduhan parsial tetap memakai `.part`, tidak pernah dianggap model siap. Download mendukung jeda dan Range yang divalidasi; bila server mengirim ulang berkas penuh, aplikasi memulai ulang penyimpanan dengan pemeriksaan ruang. Redirect, ukuran berlebih, checksum salah, dan filename di luar folder privat ditolak. Model disimpan dalam `noBackupFilesDir`, bukan direktori bersama/backup cloud.

Asal model, lisensi, konversi, checksum, dan hasil inferensi yang benar-benar dijalankan dicatat di [QURAN_AI_MODEL.md](QURAN_AI_MODEL.md). Model telah dipublikasikan dan berkas unduhannya diperiksa ulang dengan ukuran/SHA-256 yang sesuai. Bobot tidak disisipkan ke APK agar pengguna dapat memilih kapan mengunduhnya.

## Pemrosesan mikrofon lokal

- Audio PCM mono 16 kHz, 16-bit, disimpan sementara dalam RAM; tidak ada berkas rekaman, transkrip yang disimpan, atau unggahan suara.
- Capture dan inferensi berada di worker, bukan thread UI. Buffer dibatasi 90 detik. Timer terpisah juga menghentikan mikrofon setelah 90 detik jika pembacaan perangkat macet atau tidak menghasilkan sampel.
- Inferensi sementara pertama dipicu setelah sekitar 6 detik audio. Percobaan berikutnya memerlukan tambahan setidaknya 6 detik; hanya satu inferensi berjalan, sehingga HP lambat tidak menumpuk pekerjaan. Angka ini adalah batas pemicu, **bukan jaminan hasil muncul dalam 6 detik**. Kecepatan model bergantung pada CPU/memori HP.
- Saat pengguna menekan Selesai, capture berhenti dan satu hasil akhir dihitung dari audio yang terkumpul. Pemrosesan akhir dapat memerlukan waktu. Tombol Batal tetap tersedia.
- Sinyal kosong atau satu klik singkat tidak dikirim ke model. Ini pemeriksaan amplitudo sederhana, bukan detektor ucapan atau jaminan bebas halusinasi.
- Tidak ada teks ayat target, prompt jawaban, atau bias kalimat yang dikirim ke model. Decoder menggunakan konteks baru setiap permintaan. Model tetap dapat salah mengenali atau melengkapi kata dari pola yang dipelajarinya.
- Membuka halaman, mengganti mode/ayat, atau kembali ke aplikasi tidak otomatis menyalakan mikrofon. Audio murottal dijeda sebelum pengguna memulai latihan.
- Meninggalkan layar atau aktivitas masuk keadaan STOP membatalkan sesi. Start/stop recorder diserialisasi; hasil generasi lama diabaikan. Model yang selesai dimuat setelah pembatalan segera ditutup. Mesin dilepas ketika layar ditinggalkan.
- Native library dikompilasi untuk ARM64, ARMv7, dan x86_64; ukuran halaman 16 KiB diaktifkan. Dukungan ABI tidak membuktikan bahwa setiap HP mempunyai memori/kecepatan yang memadai.

## Makna hasil dan progres

Perbandingan kata memakai alignment edit-distance terbatas untuk menangani kata berulang, hilang, tambahan, dan berbeda. Hasil parsial hanya menandai kecocokan sementara: kata belum terdengar atau berbeda tidak dinilai sebagai kesalahan final. Hasil akhir memberi **perkiraan bagian yang perlu diperiksa**, karena pengenal suara sendiri dapat salah.

Pencocokan mengabaikan tanda vokal/harakat dan menormalkan variasi alif/hamzah serta beberapa ejaan Utsmani. Alif biasa tidak dihapus sembarangan. Dagger-alif dapat muncul sebagai alif biasa atau hilang dalam tulisan hasil suara, misalnya `ٱلْعَٰلَمِينَ` → `العالمين` dan `ذَٰلِكَ` → `ذلك`. Variasi `صلوة`/`زكوة`/`حيوة` dengan dagger-alif ditangani melalui daftar ejaan terbatas. Karena normalisasi ini, hasil **tidak dapat digunakan untuk menilai panjang mad, ghunnah, harakat, atau makhraj**. Huruf muqatta‘ah yang dikenali sebagai nama huruf juga bisa menghasilkan perbedaan teks.

Ayat target menggunakan teks kanonis setelah kebijakan basmalah pembuka. Untuk 2:1, target adalah `الٓمٓ`; basmalah pembuka tidak dimasukkan ke pencocokan. Al-Fatihah 1:1 tetap mempunyai basmalah sebagai ayatnya.

Yang disimpan hanyalah jumlah percobaan selesai dan himpunan referensi ayat yang pernah dilatih. Jumlah kata cocok bukan nilai kualitas pelafalan; jumlah percobaan bukan bukti hafal. Bacaan yang penting untuk dinilai tetap perlu ditinjau bersama guru Al-Qur’an.

## Pilihan pengenal Android

Mode kedua menggunakan Android `SpeechRecognizer`. Pada API 31+, pengenal di perangkat diutamakan bila tersedia; perangkat/penyedia tetap harus mempunyai model bahasa Arab. Penyedia sistem yang dapat memproses online baru dipakai setelah pengguna mencentang persetujuan eksplisit. Penyedia tersebut dapat mengirim audio ke servernya; mode ini tidak dijanjikan offline. Nuraya tetap tidak menyimpan rekaman/transkrip.

Pengenal Android mempunyai timeout percobaan 90 detik dan batas menunggu hasil akhir 15 detik. Kecepatan/frekuensi hasil parsial bergantung pada penyedia. Tidak ada auto-restart atau perekaman di latar belakang.

## Verifikasi dan batasnya

- `RecitationMatcherTest`: normalisasi, bagian parsial, kata hilang/tambahan/berulang, rentang tulisan asli dan perbedaan akhir.
- `LocalQuranCaptureTest`: batas buffer, konversi PCM tanpa overflow, snapshot terpisah, pembersihan rekaman, serta penolakan input baca tidak valid/sinyal kosong.
- `QuranModelStoreTest`: sumber HTTPS immutable, checksum/filename/ukuran yang aman, validasi isi berkas, dan penolakan model parsial. Tes kontrak publikasi menolak konfigurasi model sementara.
- `LocalQuranSessionLifecycleTest`: izin yang belum diberikan dan sesi yang telah ditutup tidak boleh mengaktifkan model/mikrofon atau mencatat hasil.
- `AiRecitationEntryPresentationTest`: tombol AI terlihat tanpa menggulir Beranda; halaman awal menampilkan mode/model dan belum mengaktifkan mikrofon. Render Robolectric ini bukan pengujian mikrofon atau native AI pada HP.

Hasil build/test final dicatat di `BUILD_VALIDATION.md`, dan hasil inferensi rekaman host di dokumentasi model. Belum ada klaim evaluasi klinis/akademis ketepatan tajwid, pengujian mikrofon HP fisik, atau jaminan semua perangkat bekerja sama cepatnya. Uji host tidak menggantikan pengujian izin, interupsi telepon, pelepasan mikrofon, dan performa inferensi pada HP Android.

Sumber platform: [SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer), [RecognitionListener](https://developer.android.com/reference/android/speech/RecognitionListener), [RecognizerIntent](https://developer.android.com/reference/android/speech/RecognizerIntent), dan [AudioRecord](https://developer.android.com/reference/android/media/AudioRecord). Versi dokumentasi lama dipertahankan di [RECITATION_ASSISTANT_1.1.0.md](RECITATION_ASSISTANT_1.1.0.md).
