# Qari photographs in Nuraya 1.3.0

Eight real profile photographs are bundled offline: Mishary Rashid Alafasy, Abdul Basit Abdus Samad, Abdurrahman As-Sudais, Abu Bakr Ash-Shatri, Hani Ar-Rifai, Mahmoud Khalil Al-Husary, Muhammad Siddiq Al-Minshawi, and Saud Al-Shuraim. They cover 14 exact audio edition IDs across the surah and verse catalogs. A portrait is never selected using approximate name matching. Other reciters retain their initials, with an accessible explanation that their photograph is unavailable.

The qari list, selected qari, mini player, and full player show the corresponding portrait. Photo credits appear in the selected qari and full player views. Large labeled listening, favorite, download, play/pause, previous, and next controls accompany these views.

The photographs come from the official Quran.com frontend repository, pinned to commit `aff1a035b09b66f28047b3216edcae4c5c949a49`. `app/src/main/assets/portraits/catalog.json` records each Quran.com reciter profile, original URL, identity aliases, byte length, SHA-256 digest, and rights notice. The repository's MIT declaration is not presented as an independent per-photo open license; rights remain with the respective photograph owners. These are attributed identification photographs, not generated faces.

`QariPortraitTest` checks all bundled image digests, decodability, exact edition membership, and rejection of misleading name matches (including Hani versus Nabil/Mahmood Ar-Rifai). `QariPortraitPresentationTest` checks the displayed identity and selection callback and captures the qari list. Full validation results belong in `BUILD_VALIDATION.md`; these tests do not validate physical-device audio playback.
