"""Offline regression checks for the pinned Quran's opening/ayah boundary."""
from __future__ import annotations

import copy
import importlib.util
import json
from pathlib import Path
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "import_quran.py"
spec = importlib.util.spec_from_file_location("import_quran", SCRIPT)
import_quran = importlib.util.module_from_spec(spec)
spec.loader.exec_module(import_quran)


class QuranOpeningTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.corpus = json.loads(import_quran.ASSET.read_text())

    def test_all_114_source_openings_and_6236_verses_validate(self):
        import_quran.validate(self.corpus)
        self.assertEqual(6236, len(self.corpus["verses"]))

    def test_112_openings_are_lossless_and_no_numbering_or_translation_changes(self):
        count = 0
        for verse in self.corpus["verses"]:
            before = copy.deepcopy(verse)
            raw = verse["arabic"]
            body, opening = import_quran.split_opening_basmalah(verse["surah"], verse["ayah"], raw)
            if opening is not None:
                count += 1
                self.assertEqual(1, verse["ayah"])
                self.assertNotIn(verse["surah"], (1, 9))
                self.assertEqual(raw, opening + " " + body)
                self.assertTrue(body)
            else:
                self.assertEqual(raw, body)
            self.assertEqual(before, verse)
        self.assertEqual(112, count)

    def test_known_first_verses_have_the_actual_text(self):
        expected = {
            2: "الٓمٓ", 3: "الٓمٓ", 55: "ٱلرَّحْمَٰنُ",
            95: "وَٱلتِّينِ وَٱلزَّيْتُونِ",
            97: "إِنَّآ أَنزَلْنَٰهُ فِى لَيْلَةِ ٱلْقَدْرِ",
            112: "قُلْ هُوَ ٱللَّهُ أَحَدٌ",
            113: "قُلْ أَعُوذُ بِرَبِّ ٱلْفَلَقِ",
            114: "قُلْ أَعُوذُ بِرَبِّ ٱلنَّاسِ",
        }
        for verse in self.corpus["verses"]:
            if verse["ayah"] == 1 and verse["surah"] in expected:
                body, _ = import_quran.split_opening_basmalah(verse["surah"], 1, verse["arabic"])
                self.assertEqual(expected[verse["surah"]], body)

    def test_fatiha_and_naml_remain_exact_and_tawbah_has_no_intro(self):
        for reference in ((1, 1), (9, 1), (27, 30)):
            verse = next(v for v in self.corpus["verses"] if (v["surah"], v["ayah"]) == reference)
            self.assertEqual((verse["arabic"], None), import_quran.split_opening_basmalah(*reference, verse["arabic"]))

    def test_replacing_actual_first_verse_with_only_basmalah_is_rejected(self):
        corrupt = copy.deepcopy(self.corpus)
        verse = next(v for v in corrupt["verses"] if (v["surah"], v["ayah"]) == (2, 1))
        verse["arabic"] = import_quran.BASMALAH
        with self.assertRaisesRegex(AssertionError, "2:1"):
            import_quran.validate(corrupt)

    def test_duplicate_basmalah_is_rejected(self):
        corrupt = copy.deepcopy(self.corpus)
        verse = next(v for v in corrupt["verses"] if (v["surah"], v["ayah"]) == (112, 1))
        verse["arabic"] = import_quran.BASMALAH + " " + verse["arabic"]
        with self.assertRaisesRegex(AssertionError, "Duplicate basmalah"):
            import_quran.validate(corrupt)

    def test_tawbah_cannot_gain_an_intro(self):
        corrupt = copy.deepcopy(self.corpus)
        verse = next(v for v in corrupt["verses"] if (v["surah"], v["ayah"]) == (9, 1))
        verse["arabic"] = import_quran.BASMALAH + " " + verse["arabic"]
        with self.assertRaisesRegex(AssertionError, "At-Tawbah"):
            import_quran.validate(corrupt)

    def test_already_separated_and_unrecognized_input_is_not_rewritten(self):
        for raw in ("الٓمٓ", "بسم الله الرحمن الرحيم الم", import_quran.BASMALAH):
            self.assertEqual((raw, None), import_quran.split_opening_basmalah(2, 1, raw))


if __name__ == "__main__":
    unittest.main()
