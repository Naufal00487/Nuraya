# Audit identitas sumber audio Nuraya 1.4.0

Audit dapat diulang dengan `python3 scripts/audit-audio-identity.py`. Script ini membaca snapshot sumber dan bukti unduhan yang sudah dilakukan, tanpa mengubah aset aplikasi atau mengklaim pemeriksaan suara oleh manusia.

| Pemeriksaan | Hasil |
| --- | --- |
| Katalog murottal per surah | 138 ID edisi unik; masing-masing 114 nama berkas sesuai manifest; seluruh ID tercantum pada metadata penyedia |
| Sampel murottal | 138 Al-Fatihah diunduh lengkap, HTTP 200, MP3 berhasil didekode; 0 gagal |
| Katalog audio per ayat | 17 ID edisi unik; masing-masing 6.236 nama berkas sesuai manifest |
| Sampel audio per ayat/sinkronisasi | 22 unduhan lengkap dan decode berhasil: 17 ayat 1:1, 3 surah dengan timing, 2 ayat jalur fallback qari yang sama |
| Foto qari | 134 foto, 140 pemetaan ID; 8 foto dari Quran.com dan 126 dari dokumen penyusun; checksum seluruh foto cocok; tidak ada pemetaan ID ganda |
| Nama tampilan | 129 sama persis dengan nama Inggris penyedia; 9 label ejaan/nama lengkap tercatat di JSON tanpa mengganti ID rekaman |

Pemeriksaan 138 sampel surah dilakukan pada `2026-11-04T11:55:26.354674+00:00`. Pemeriksaan 22 sampel ayat dilakukan pada `2026-11-05T06:30:25.354540+00:00`. Ini adalah bukti pemeriksaan pada waktu tersebut, bukan jaminan ketersediaan layanan selamanya.

## Temuan dan perbaikan

- `ar.alafasy` dan `ar.misharyrashidalafasy` mempunyai sampel Al-Fatihah dengan byte dan SHA-256 identik. Daftar sekarang menampilkan satu kartu Alafasy (137 pilihan terlihat), sedangkan kedua ID tetap tersedia untuk riwayat, favorit, dan unduhan lama. Tidak disimpulkan bahwa seluruh 114 berkas kedua edisi identik.
- Pemutar ayat sebelumnya dapat memilih Alafasy diam-diam ketika ID qari tidak ditemukan. Perilaku itu dihapus: audio/persiapan dihentikan dan pengguna diminta memilih ulang qari. Pengujian regresi terdapat pada `PlaybackIdentityTest`.
- Edisi murattal, mujawwad, dan riwayat berbeda tetap dipertahankan. Lima edisi selain Hafs diberi keterangan eksplisit: Abul-Harith dari al-Kisa’i, ad-Doori dari Abu Amr, Shu’bah dari Asim, as-Soosi (Abdur-Rasheed Sufi), dan Qaloon (Al-Hudhaify).
- Sinkronisasi kata hanya diaktifkan pada Alafasy yang mempunyai pasangan audio/timing; qari lain tidak dipasangkan dengan timing suara Alafasy.

## Batas pemeriksaan

Pemetaan nama → ID penyedia → URL rekaman telah diperiksa. Identitas suara setiap qari belum diautentikasi melalui pendengaran manusia. Pemeriksaan decode MP3 juga tidak menilai makhraj, tajwid, atau ketepatan semua bacaan. Yang diunduh lengkap adalah 138 Al-Fatihah dan 22 sampel tambahan, bukan seluruh 15.732 berkas surah atau seluruh klip ayat. Pemutaran pada HP fisik belum menjadi bagian audit ini.

Detail ID, ejaan nama, pemetaan foto, hash masukan, dan kelompok sampel identik ada dalam [audio-identity.json](audio-identity.json). Bukti mentah: [audio-full-samples.json](../1.3.1/audio-full-samples.json) dan [verse-audio-samples.json](../1.3.1/verse-audio-samples.json).
