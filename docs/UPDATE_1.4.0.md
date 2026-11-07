# Nuraya 1.4.0

Tampilan baru mengikuti panduan dan aset desain penyusun, dengan palet biru, ilustrasi lanskap sesuai waktu, profil lokal, foto qari, kompas, dan latihan yang lebih jelas. Paket `com.nuraya.app`, versionCode 9; data bacaan dan fitur AI tetap dipertahankan.

- **Beranda:** jam, Hijriah, jadwal/hitung mundur, tombol azan/kiblat, lokasi otomatis/manual, pengaturan dan profil. Empat navigasi utama tetap Beranda, Al-Qur’an, Murottal dan Ibadah.
- **Profil pribadi:** nama panggilan dan lima avatar ilustrasi internal; tersimpan di HP. Kuis memakai avatar/nama, soal berukuran besar, umpan balik dan hasil pribadi, tanpa peringkat online.
- **Murottal:** daftar dua kolom foto/nama, pilihan qari di beranda, serta halaman detail rekaman. Ada 134 foto dengan 140 pemetaan ID edisi: delapan sumber Quran.com dan 126 dari dokumen penyusun. Nama/ID dicocokkan, bukan autentikasi wajah independen. Empat placeholder dan satu gambar dengan label bertentangan tidak dipasang.
- **Hafalan:** pilih pengulangan per ayat sebelum maju, atau seluruh rentang sebelum mengulang. Jumlah 1–20 kali, berhenti setelah selesai; tersedia pilihan seluruh surah. Al-Mulk 1–5 ×3 menghasilkan 15 ayat dalam tiga putaran. Teks Arab Utsmani tetap utuh.
- **Kiblat:** kompas biru dengan skala derajat, mata angin dan penunjuk Ka'bah mengikuti sensor. Tanpa sensor, hanya bearing numerik ditampilkan; tidak ada panah palsu.
- **Azan:** awalnya nonaktif, kemudian dapat diaktifkan dengan izin notifikasi dan alarm tepat waktu. Rekaman Makkah disertakan dalam APK, mengikuti volume alarm HP dan dapat dihentikan dari notifikasi.
- **Banner:** dapat digeser; [proyek bot Telegram](../backend/banner-bot/README.md) disiapkan dengan unggah/edit/hapus, aktif/nonaktif dan urutan. **Belum deploy atau terhubung ke bot**, sesuai pilihan pengguna. Banner bawaan tetap tersedia. Tidak memerlukan Hostinger; Cloudflare Workers + KV dapat dipakai ketika akun tersedia.
- **Tentang aplikasi:** tersedia dari profil/pengaturan. Wording finansial di antarmuka dihapus, atribusi sumber dan penjelasan privasi tetap ada.

Halaman qari memberi informasi edisi/rekaman dari penyedia. Biografi pribadi lengkap seluruh qari belum tersedia, sehingga tanggal lahir/negara/pendidikan tidak diisi dengan dugaan. GPS, sensor kompas, mikrofon dan bunyi azan masih perlu diperiksa pada HP fisik. AI membantu memperkirakan kata bacaan, bukan penilai makhraj/tajwid yang tervalidasi.

Bukti build, tes, gambar antarmuka dan audit ada di [BUILD_VALIDATION.md](BUILD_VALIDATION.md). Sumber desain/foto/azan dicatat di [sources](sources). Token Telegram tidak dimasukkan ke APK atau source; token yang pernah dibagikan perlu diganti sebelum bot diaktifkan.
