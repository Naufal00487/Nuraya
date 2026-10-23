# Katalog qari Nuraya 1.0.1

Pilihan qari bertambah dari 3 menjadi 12. Ketiga ID lama tetap tersedia sehingga favorit, daftar putar, dan unduhan lama dapat digunakan kembali. Audio memakai HTTPS dari Islamic Network / Al Quran Cloud, 128 kbps, satu berkas untuk satu surah. Rekaman tidak dibundel dalam APK; streaming membutuhkan internet dan pemutaran offline membutuhkan unduhan yang sudah selesai.

## Pemeriksaan sumber

- Manifest: <https://cdn.islamic.network/quran/info/by-surah/info.json>.
- Snapshot manifest: `docs/sources/islamic-network-surah-manifest.json`.
- SHA-256 manifest: `6f183510806ab226e3df8be6675cd222e35209123f5153f51aa1b8b08005e25a`.
- Seluruh 12 qari memiliki tepat 114 nama berkas surah (`1.mp3` hingga `114.mp3`) dalam manifest sumber.
- Pemeriksaan online: `2026-10-23T17:52:27.642875+00:00` (UTC).
- Sampel Al-Fatihah dari setiap qari berhasil diunduh lengkap melalui HTTPS dan seluruh stream audio MP3 berhasil didekode oleh ffmpeg di host.

| Qari | Sampel Al-Fatihah | Ukuran | Durasi |
| --- | --- | ---: | ---: |
| Mishary Rashid Alafasy | HTTP 200 · MP3 decode lulus | 1,420,155 byte | 51.91 detik |
| Abdul Basit Abdus Samad | HTTP 200 · MP3 decode lulus | 1,645,693 byte | 46.79 detik |
| Saud Al-Shuraim | HTTP 200 · MP3 decode lulus | 1,274,538 byte | 37.75 detik |
| Muhammad Siddiq Al-Minshawi | HTTP 200 · MP3 decode lulus | 2,939,194 byte | 177.43 detik |
| Ahmed Al-Ajmi | HTTP 200 · MP3 decode lulus | 1,405,850 byte | 42.74 detik |
| Abdullah Basfar | HTTP 200 · MP3 decode lulus | 1,423,369 byte | 49.43 detik |
| Ali Abdurrahman Al-Hudhaify | HTTP 200 · MP3 decode lulus | 2,105,596 byte | 60.43 detik |
| Fares Abbad | HTTP 200 · MP3 decode lulus | 1,553,761 byte | 44.07 detik |
| Muhammad Ayyub | HTTP 200 · MP3 decode lulus | 911,449 byte | 59.01 detik |
| Nasser Al-Qatami | HTTP 200 · MP3 decode lulus | 1,085,091 byte | 39.72 detik |
| Yasser Al-Dossari | HTTP 200 · MP3 decode lulus | 1,302,438 byte | 47.99 detik |
| Hani Ar-Rifai | HTTP 200 · MP3 decode lulus | 1,705,816 byte | 48.04 detik |

Hasil tersebut memeriksa manifest dan 12 rekaman sampel, bukan seluruh 1.368 rekaman atau pengujian speaker/pemutar Android. Ketersediaan layanan pihak ketiga dapat berubah. Hak cipta tetap pada qari/pemegang hak, dan ketentuan penyedia hanya digunakan sesuai izin nonkomersial pribadi/pendidikan yang dicatat dalam [DATA_SOURCES.md](DATA_SOURCES.md).

## Mengulang pemeriksaan

```sh
python3 scripts/verify_audio_catalog.py           # offline, snapshot dan katalog
python3 scripts/verify_audio_catalog.py --online  # manifest live + 12 unduhan dan decode
```

ffprobe/ffmpeg diperlukan hanya untuk pemeriksaan audio online pada mesin build, bukan untuk pengguna APK. Sampel sementara dibersihkan otomatis. Laporan terstruktur dibuat di `artifacts/validation/audio-catalog.json`.
