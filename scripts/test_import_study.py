"""Regression tests for source-coordinate mapping, using synthetic fragments."""
import unittest
from collections import Counter
from import_study import map_tajwid, map_words, utf16


def source_word(position, arabic, language):
    return {'position': position, 'text_uthmani': arabic, 'char_type_name': 'word',
            'translation': {'text': 'meaning', 'language_name': language}}


class StudyMappingTests(unittest.TestCase):
    def test_changed_source_span_is_omitted_not_shifted(self):
        stats = Counter()
        mapped = map_tajwid('abcXdef', 'abcYZdef', [
            {'start': 3, 'end': 4, 'rule': 'madd_2'},
            {'start': 4, 'end': 7, 'rule': 'qalqalah'}], 0, stats)
        self.assertEqual(mapped, [{'start': 5, 'end': 8, 'rule': 'qalqalah'}])
        self.assertEqual(stats['changedTextSpansOmitted'], 1)

    def test_opening_offset_and_utf16_conversion(self):
        stats = Counter()
        mapped = map_tajwid('a😀bc', 'a😀bc', [
            {'start': 7, 'end': 8, 'rule': 'madd_2'}], 4, stats)
        self.assertEqual(mapped, [{'start': 4, 'end': 5, 'rule': 'madd_2'}])
        self.assertEqual(utf16('a😀bc'), 5)

    def test_grouped_source_word_keeps_position_and_both_tokens(self):
        arabic = ['ب ت', 'ج']
        result = map_words('ب ت ج', [source_word(i + 1, w, 'indonesian') for i, w in enumerate(arabic)],
                           [source_word(i + 1, w, 'english') for i, w in enumerate(arabic)], Counter())
        self.assertEqual([(w['position'], w['start'], w['end']) for w in result], [(1, 0, 3), (2, 4, 5)])
        self.assertEqual(result[0]['arabic'], 'ب ت')

    def test_unmatched_word_cannot_highlight_and_later_word_does_not_shift(self):
        source = ['ب', 'ز', 'ج']
        result = map_words('ب ت ج', [source_word(i + 1, w, 'indonesian') for i, w in enumerate(source)],
                           [source_word(i + 1, w, 'english') for i, w in enumerate(source)], Counter())
        self.assertIsNone(result[1]['start'])
        self.assertIsNone(result[1]['end'])
        self.assertEqual((result[2]['position'], result[2]['start'], result[2]['end']), (3, 4, 5))

    def test_wrong_language_is_not_silently_labeled_indonesian(self):
        with self.assertRaises(AssertionError):
            map_words('ب', [source_word(1, 'ب', 'english')], [source_word(1, 'ب', 'english')], Counter())


if __name__ == '__main__':
    unittest.main()
