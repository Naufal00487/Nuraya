# Madinah page images

Nuraya displays original 604-page Madinah images rather than fabricating a cover or laying reflowed text out as a claimed printed edition. Source: [GovarJabbar/Quran-PNG](https://github.com/GovarJabbar/Quran-PNG), pinned commit `b189391148dfb4d7b44e7de40fe2cf942e7c84ac`, using Madinah page artwork of the same font family documented by [quran/quran.com-images](https://github.com/quran/quran.com-images). Quran glyph/page artwork belongs to the King Fahd Quran Complex. No independent Creative Commons license for these page images is asserted.

`assets/mushaf/madinah-images.json` lists all 604 immutable URLs and Git blob SHA-1 values from the pinned tree. Every downloaded or cached page is checked against that value, including the Git blob header. The loader accepts only the pinned raw GitHub source, refuses redirects, bounds download size and caches up to 64 MiB of recent pages. Pages 1, 2, 3 and 604 are bundled for offline opening and validation. Other pages need an Internet connection the first time they are opened; the cache is not a promise that every downloaded page will remain offline forever.

The complete 6,236 verse-to-page mapping in `madinah-pages.json` comes from `https://api.alquran.cloud/v1/quran/quran-uthmani`. This index is for the standard 604-page Madinah layout; it is not used to claim that another publisher's Bahriyah layout is equivalent. The normal canonical Arabic text remains unchanged.

The reader opens the selected verse's page, offers page number selection, previous/next controls, enlargement from 100% to 300%, and scrolling in both directions. Original source page colors remain unchanged. **Madinah & tajwid** links to the existing colored Arabic verse reader at the first verse on the displayed page; it does not claim the monochrome source image itself has tajwid coloring. Preview art is a genuine Madinah page, not a physical book-cover photograph.

Tests check all verse/page references, offline image decode, corruption rejection, page navigation and the verse-companion callback. The current host-rendered screenshot is [madinah-page.png](validation/1.3.1/madinah-page.png). These checks do not substitute for a physical Android device test.

Page 3 uses the original JPG from the same publisher, [GovarJabbar/Quran-JPG](https://github.com/GovarJabbar/Quran-JPG), pinned to `9aa696d923843acef00a54e7c4c872414494c4ba`. The PNG version has a visibly corrupted background and is excluded. The source JPG is unchanged, independently hash-checked and bundled as `003.jpg`; the loader only permits this exact additional pinned URL. Source selection does not redraw, erase or generate Quran text.

The unchanged [complete page-download evidence](validation/reference/madinah/mushaf-all-pages-check.json) and [source download details](validation/reference/madinah/mushaf-source-downloads.json) come from checks performed before the 29 October 2026 release. These historical source checks are not presented as a new download of every page.


## Reader revision 1.5.0

The original page is framed without redrawing or recoloring its Quran text. A single tap toggles controls; a double tap requests landscape. The menu also offers an accessible orientation action. Landscape uses a width-fitting scrollable page. Page/surah/ayah/juz navigation uses the existing 6,236-reference index. Other Quran modes offer a clearly labeled Madinah book-view button; this does not turn Madinah images into a different printed edition.

The companion sheet shows the current verse, attributed Kemenag/QuranEnc translation, optional Latin, text size, word meanings, tafsir and source-mapped tajwid. Audio highlighting appears in this text panel, not at invented coordinates on the page image. The 17-reciter picker supports search, existing identity-mapped portraits, per-verse repeats, play and pause. Missing portraits retain labeled initials.

Explicit **Simpan halaman offline** validates source bytes and writes them atomically to private persistent app storage. Saved pages survive eviction from the 64 MiB recent-page cache; they are removed with app data/uninstall. Unopened/unsaved pages still require a connection. User-provided book covers are navigation illustrations, recorded in `sources/revision-1.5.0-assets.json`; they do not establish the complete text of that photographed edition.


## Indonesia ornamental pages — 1.5.1

**Al-Qur’an Indonesia** and **Indonesia & tajwid** now open the 604 original ornamental Arabic page images from [siar08888000077/mushaf-indonesia](https://github.com/siar08888000077/mushaf-indonesia), pinned to `76abbcd2ebc88433ee8b530e1611a7219919d574`. The files are 641 × 917 pixels. They are not the Islampedia edition with printed Indonesian word meanings and are not represented as an officially certified Kemenag print edition. The source repository does not declare a publisher or a separate image license; no independent license is asserted.

All 604 files were downloaded, decoded and checked against the pinned Git blob hashes on 12 November 2026. There are no identical duplicate page files. [Download evidence](audit/1.5.1/mushaf-indonesia-files.json) records each URL, hash, dimensions and indexed boundaries. Final printed ayah markers on all 604 pages were visually compared against the standard 604-page index; no page-boundary discrepancy was found. [Visual review scope](audit/1.5.1/mushaf-indonesia.json) explicitly distinguishes this pagination check from expert proofreading of every glyph, harakat or waqaf sign. The complete 6,236-reference index is stored separately in `indonesia-pages.json`.

The app checks downloaded, cached and saved bytes before decoding them. Storage namespaces and bundled paths are separate for Indonesia and Madinah; saving either edition never overwrites the other. Four unchanged images (1, 2, 3, 604) are bundled. The remaining pages download individually when opened, avoiding an additional whole-book download in the APK. A 64 MiB recent-page cache applies per edition; explicit saved pages remain in private storage until app data is removed.

Indonesian translation, per-word meanings, optional Latin, tafsir, colored tajwid and audio word highlights remain in the companion sheet. The source image is not redrawn, recolored or overlaid with guessed word positions. The word-meaning panel is expanded when opened in the Indonesia edition. Audio retains the existing 17-reciter selection and timing coverage. Madinah retains its own source. Bahriyah remains a local PDF reader; these images are not substituted as a Bahriyah print edition.

To repeat integrity checks after downloading the files as `001.png` through `604.png`:

```sh
python3 scripts/verify-indonesia-pages.py /path/to/pages --output /path/to/check.json --review-dir /path/to/review
```

The optional contact sheets support a separate visual review; running the script does not certify image text or automatically mark visual checks as passed. Runtime/UI checks cover source separation, corruption rejection, page-to-verse navigation and returning from the companion verse reader. Screenshots in `validation/1.5.1` are Compose/Robolectric host renders, not physical-device captures.
