# Model AI bacaan Nuraya 1.2.0

Nuraya memakai **Tarteel AI whisper-base-ar-quran**, dikonversi menjadi GGML Q5_1 dan dijalankan dengan whisper.cpp di perangkat. Bobot asli telah diunduh dan cocok dengan checksum penyedia. Model hasil konversi berhasil menjalankan inferensi melalui wrapper JNI yang sama dengan aplikasi pada host. Ini bukti model dapat dijalankan, **bukan jaminan akurasi bacaan atau kecepatan real-time pada HP**.

## Model dan distribusi

- Sumber: [`tarteel-ai/whisper-base-ar-quran`](https://huggingface.co/tarteel-ai/whisper-base-ar-quran/tree/5c3c53fdf9272c4f6ee0bee09a1e5a4a615ee25c).
- Revisi sumber: `5c3c53fdf9272c4f6ee0bee09a1e5a4a615ee25c`.
- Bobot PyTorch asli: **290.458.721 byte**, SHA-256 terverifikasi **`ec75894993a4570a237b2b1c19fd8a448f1e8f6e083cfab699574f7be53874fa`**.
- Model Q5_1: **59.707.625 byte** (sekitar 59,7 MB), SHA-256 **`1de4498dc16fdee2045e67a97d73cda4624017640dbe5ddf809f3130da7c234a`**.
- [Unduhan model immutable](https://raw.githubusercontent.com/Naufal00487/Nuraya/1d5a31aa2e60dbb9af70981e606a9c06b1b18c3e/nuraya-quran-whisper-base-q5_1.bin). Seluruh berkas publik telah diunduh ulang melalui HTTP 200 dan cocok ukuran/SHA-256-nya; bukti ada di [model-download.json](validation/1.2.0/model-download.json). Unduhan satu kali dilakukan terpisah dari APK; aplikasi memeriksa ukuran dan checksum sebelum menggunakannya.
- Runtime: whisper.cpp **v1.7.6**, commit `a8d002cfd879315632a579e73f0148d06959de36`, MIT. Library Android dikompilasi untuk arm64-v8a, armeabi-v7a, dan x86_64.

Model card menyatakan Apache-2.0. Snapshot kartu, Apache-2.0, MIT OpenAI Whisper, catatan perubahan, checksum, dan metadata konversi dipertahankan di `sources/quran-ai/`; pemberitahuan lisensi juga dibundel di asset aplikasi.

Kartu model melaporkan WER 5,7544 dari evaluasi pembuatnya, tetapi bagian data, tujuan penggunaan, dan batasannya belum lengkap. Angka tersebut bukan hasil tes Nuraya dan tidak mengukur koreksi tajwid/makhraj. Nuraya tidak membuat klaim kepakaran dari angka tersebut.

## Pengujian yang benar-benar dilakukan

Tanggal pemeriksaan: **26 Oktober 2026** (Asia/Jakarta). Bukti mesin di [model-inference.json](validation/1.2.0/model-inference.json), [log JNI](validation/1.2.0/native-ai-jni.log), dan [log probe](validation/1.2.0/native-ai-probes.log).

**Tujuh pemeriksaan wrapper JNI host lulus:** senyap digital menghasilkan teks kosong, input nonfinite ditolak, pembatalan antrean, inferensi berikutnya setelah pembatalan, pembatalan saat inferensi berjalan, pemakaian ulang konteks setelah pembatalan aktif, serta pelepasan konteks yang aman/idempoten. Sampel nyata 12 detik menghasilkan basmalah; waktu inferensi host sekitar **11,856 detik** dengan empat thread CPU.

Probe tambahan menggunakan rekaman pasangan Alafasy yang telah dicocokkan checksum dan timestamp sumber. Teks ayat yang diharapkan **tidak diberikan sebagai prompt**.

| Input | Hasil yang diamati | Waktu host |
| --- | --- | ---: |
| Al-Fatihah 1:1 lengkap, 6,025 detik | Keempat kata basmalah dikenali | 11,736 dtk |
| Rentang waktu kata Ar-Rahman dibisukan | Hanya “Bismillah” dikenali; kata Ar-Rahim yang masih ada ikut terlewat | 11,523 dtk |
| Dipotong sebelum Ar-Rahim | Tiga kata pertama dikenali, Ar-Rahim tidak ditambahkan | 11,980 dtk |
| Senyap digital enam detik | Teks kosong melalui pemeriksaan energi native | <0,001 dtk |

Interval pembisuan mengikuti metadata kata penyedia, bukan segmentasi fonetik yang diperiksa ahli. Probe ini menunjukkan model dapat mengenali bacaan sekaligus **dapat salah menganggap kata yang sebenarnya dibaca sebagai tidak terdengar**. Empat contoh tidak cukup untuk mengukur tingkat akurasi umum. Hasil senyap digital berasal dari pemeriksaan energi sebelum model, bukan klaim ketepatan model terhadap semua jenis hening atau kebisingan.

**Belum diuji dengan mikrofon pada HP fisik.** Latensi host di atas bukan latensi Android; pembaruan sementara mengikuti kecepatan perangkat dan dapat terlambat. Tidak ada jaminan koreksi seketika. Model ini tidak menilai harakat, panjang mad, ghunnah atau makhraj secara tervalidasi, dan hasil pencocokan kata bukan skor benar-salah bacaan agama.

## Konversi yang dapat diulang

Dependensi yang dipakai: PyTorch **2.6.0+cpu**, NumPy **2.3.5**. Script menggunakan `torch.load(..., weights_only=True)`, tanpa kode model jarak jauh atau pemuatan pickle bebas. Semua sumber wajib cocok dengan ukuran dan checksum yang dipin.

```bash
bash scripts/build-quran-model-tools.sh
PYTHONPATH=/workspace/.tools/quran-model-python python3 scripts/prepare-quran-model.py \
  --fetch --convert --quantizer /workspace/.tools/whisper-host/bin/quantize
bash scripts/test-native-ai.sh /workspace/.tools/quran-model/nuraya-quran-whisper-base-q5_1.bin
python3 scripts/probe-quran-model.py /workspace/.tools/quran-model/nuraya-quran-whisper-base-q5_1.bin
```

Konversi menulis F16 dan Q5_1 beserta `conversion.json` dengan ukuran/hash aktual. F16 yang dihasilkan berukuran 147.951.465 byte dengan SHA-256 `0a30efbd25c2464360809accb85d8492d48bce2945939405fb800b8281ff2f3c`.

Adaptasi konversi menggunakan **448 posisi decoder**, sesuai `max_target_positions` dan bentuk tensor. `config.max_length=1024` adalah opsi generasi, bukan ukuran posisi decoder. Tensor kecil tetap FP32 tanpa pembulatan FP16 perantara. Filter mel OpenAI dipin pada commit `86098128c0b4f24f0e2aa2994de830614b474227`; checksum tersimpan pada provenance.

## Riwayat akses sumber

Unduhan awal terhalang proxy **Tunnel 403** pada `us.aws.cdn.hf.co`, meski metadata `huggingface.co` tersedia. Pada 26 Oktober 2026 jalur unduhan resmi kembali dapat diakses; bobot lengkap lalu diunduh dan diverifikasi. Hambatan ini sudah teratasi untuk artefak di atas. Tidak digunakan proxy perantara atau endpoint demo untuk mengambil model maupun mengirim suara pengguna.

Setelah model tersimpan, pemrosesan AI berjalan lokal. Suara dan transkrip tidak dikirim ke server atau disimpan sebagai rekaman oleh fitur AI ini. Mode pengenal Android terpisah tetap mengikuti persetujuan pengguna dan kebijakan penyedia suara yang dipilih.
