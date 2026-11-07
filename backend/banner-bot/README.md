# Bot banner Nuraya

Proyek siap dipasang di Cloudflare Workers + KV. Tidak perlu Hostinger/VPS. Akun Cloudflare belum dihubungkan, sehingga **bot belum aktif/deploy**. Kuis/profil/progres tetap lokal; layanan ini hanya mengelola banner publik.

## Aktifkan setelah akun tersedia

1. Ganti token yang pernah dibagikan melalui BotFather. Jangan menaruh token di aplikasi Android, source, README atau `wrangler.toml`.
2. Di folder ini jalankan `npm install`, kemudian `npx wrangler login`.
3. Jalankan `npx wrangler kv namespace create BANNERS`, lalu isi ID hasilnya pada `wrangler.toml`.
4. Simpan tiga secret melalui prompt: `npx wrangler secret put TELEGRAM_TOKEN`, `npx wrangler secret put ADMIN_CHAT_ID`, dan `npx wrangler secret put WEBHOOK_SECRET`. Yang terakhir adalah string acak minimal 32 karakter (huruf/angka/underscore/hyphen), berbeda dari token bot.
5. Jalankan `npm run deploy`. Simpan URL HTTPS worker.
6. Pada lingkungan lokal yang aman, set `TELEGRAM_TOKEN` dan `WEBHOOK_SECRET`, lalu jalankan `node setup-webhook.mjs https://NAMA-WORKER.workers.dev`. Nilai secret tidak dicetak oleh skrip. `.dev.vars` dikecualikan Git; `.dev.vars.example` hanya contoh.
7. Ubah `content/banner-service.json` di branch bawaan repo: isi `baseUrl` dengan URL worker tanpa `/webhook`. Publikasikan perubahan. Jangan isi token atau chat ID dalam berkas publik itu.
8. Buka bot secara privat dari akun pemilik dan kirim `/help`. Kirim foto JPEG dengan caption `Judul | Keterangan | quran`. Aplikasi memperbarui banner saat beranda dibuka, maksimal sekali 15 menit. KV juga memiliki jeda propagasi.

Tujuan internal: `quran`, `murottal`, `practice`, `recitation`, `prayer`, `library`, `learn`. Tujuan eksternal harus URL HTTPS. Maksimal 12 banner; foto maksimal 1,5 MB dan 4096 piksel. Perintah: `/list`, `/hapus ID`, `/aktif ID`, `/nonaktif ID`, `/urutan ID ANGKA`, `/ubah ID | Judul | Keterangan | Tujuan`.

Pesan grup dan chat selain `ADMIN_CHAT_ID` diabaikan. Webhook memakai secret header Telegram. Token tidak pernah dikirim ke aplikasi; endpoint `/banners` dan `/images/ID` hanya menyajikan konten publik. Gambar yang dinonaktifkan tidak dilayani lagi setelah cache habis. Banner bawaan tetap tersedia saat belum dikonfigurasi atau belum ada banner aktif; cache membantu ketika jaringan terputus.

`npm test` menjalankan tes lokal tanpa menghubungi Telegram/Cloudflare. Sesudah deploy, periksa `/banners`, coba upload/edit/hapus melalui akun admin, dan pastikan akun lain tidak dapat mengubahnya. Ketersediaan dan kuota paket Cloudflare mengikuti ketentuan layanan saat pendaftaran; tidak ada jaminan hosting tanpa biaya selamanya.
