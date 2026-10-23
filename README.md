# Nuraya 1.0.1 — APK pengembangan

Unduh **Nuraya-1.0.1.apk** dari daftar file, lalu pilih tombol **Download raw file** di GitHub. Source code ada dalam **Nuraya-1.0.1-source.zip**.

## Perubahan

1. Tulisan Arab Tanzil Utsmani memakai font Amiri Quran yang dibundel offline.
2. Pilihan suara qari bertambah dari 3 menjadi 12, masing-masing dengan katalog 114 surah. Sampel Al-Fatihah seluruh qari berhasil diunduh dan didekode pada host.
3. Disclaimer: kami, penyusun Nuraya, tidak mengambil keuntungan finansial dari aplikasi ini. Nuraya gratis, tanpa iklan, langganan, atau pembelian dalam aplikasi.
4. Basmalah pembuka 112 surah dipisahkan tanpa nomor ayat. Ayat 1 menampilkan teks ayat sebenarnya. Al-Fatihah tetap memiliki basmalah sebagai 1:1; At-Taubah tanpa pembuka; An-Naml 27:30 tetap utuh. Jumlah dan nomor ayat serta terjemahan tidak bergeser.

## Pemasangan

- Gunakan HP Android 8.0 atau lebih baru. Jika diunduh di laptop, pindahkan APK ke HP.
- Buka APK dan pilih **Perbarui/Instal**. Izinkan pemasangan dari aplikasi pembuka file jika diminta.
- Package dan sertifikat signing sama dengan 1.0.0, versionCode meningkat menjadi 2.
- Untuk audio, pilih **Murottal → qari → surah**. Streaming memerlukan internet. Gunakan tombol unduh untuk mendengarkan offline setelah unduhan selesai.

## Validasi dan batasan

Build APK, 34 test JVM, 8 test pembuka Quran Python, 8 test backend, lint, integritas sumber, signature/alignment APK, dan 12 sampel audio host telah diperiksa. Lihat **BUILD_VALIDATION.md**. Gambar dalam `reader-previews` adalah render komposisi UI asli melalui Robolectric, bukan screenshot HP.

Pengujian fisik Android, pemutaran speaker/Bluetooth, dan unduhan Android menyeluruh belum selesai. Dialog lompat ayat tersedia sebagai test perangkat yang sudah dikompilasi; host memverifikasi posisi baca tersimpan. Ini edisi pengembangan; AI/akun belum aktif dan katalog video masih kosong.

Sumber serta lisensi ada di **DATA_SOURCES.md**, **QURAN_FONT.md**, dan source ZIP. Rekaman tetap milik qari/pemegang hak masing-masing.
