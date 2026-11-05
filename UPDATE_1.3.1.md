# Perubahan Nuraya 1.3.1

Versi **1.3.1**, versionCode **8**, memperbarui instalasi lama dengan paket dan sertifikat yang sama. AI bacaan, Quran, murottal, hafalan, kuis, koleksi dan progres tetap tersedia. Video tetap dihapus.

## Lokasi otomatis

Pada penggunaan pertama, Nuraya menampilkan penjelasan dan tombol **Aktifkan izin lokasi**. Setelah pengguna menyetujui dialog Android, koordinat HP diisi otomatis untuk jadwal salat dan kiblat. Aplikasi tidak dapat mengaktifkan izin Android tanpa tindakan pengguna.

Izin lokasi perkiraan maupun akurat didukung. Jika layanan lokasi mati, tersedia tombol pengaturan; jika izin ditolak atau posisi belum ditemukan, tersedia coba lagi dan pilihan kota manual. Quran tetap dapat digunakan. Pencarian berhenti setelah 25 detik atau saat aplikasi ditinggalkan. Cache lokasi hanya diterima jika berumur paling lama dua menit dan mempunyai koordinat serta akurasi yang valid.

Koordinat disimpan lokal. Tidak ada pengunggahan koordinat atau pelacakan lokasi di latar belakang. Zona waktu mengikuti pengaturan HP. Lokasi yang pernah dipilih tidak ditimpa otomatis saat memperbarui aplikasi; gunakan **Ibadah → Lokasi → Gunakan lokasi HP** untuk memperbaruinya. Koreksi Hijriah dan menit salat tetap dipertahankan. [Metode dan batas perhitungan](PRAYER.md).

## Perbaikan hasil audit

- ID qari yang tidak tersedia kini menghentikan pemutaran dengan pesan untuk memilih ulang. Sebelumnya jalur audio ayat dapat diam-diam menggunakan Alafasy. Tidak ada penggantian ke suara qari lain.
- Riwayat audio per ayat kembali tampil di Koleksi dan memutar qari, surah serta nomor ayat yang tepat. Kunci yang tidak valid atau bertentangan ditolak. Pemutaran berulang memperbarui satu entri riwayat.
- Dua entri Alafasy yang mewakili orang yang sama diringkas menjadi satu kartu pada daftar: **137 pilihan tampil, 138 ID lama tetap didukung**. Unduhan/favorit lama tidak dipindahkan ke ID lain. Sampel Al-Fatihah kedua entri identik; audit tidak menyatakan semua surah keduanya telah dibandingkan.
- Penjelasan hamzah wasal kini membedakan awal bacaan dan menyambung bacaan. Penjelasan waqaf mu‘anaqah diperbaiki: boleh melewati keduanya; bila berhenti, pilih salah satu dari pasangan tanda.
- Teks Arab kanonis dan nomor ayat tidak diubah. Pengulangan lafaz yang sah dalam Al-Qur’an tetap dipertahankan.

[Hasil audit dan pengecualian](AUDIT_1.3.1.md) · [Build, tes dan batas validasi](BUILD_VALIDATION.md).
