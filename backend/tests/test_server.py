import json
from pathlib import Path
import sys
import threading
import unittest
from unittest.mock import patch
from urllib import request, error
from http.server import ThreadingHTTPServer

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from server import LearningService, QuranSources, ServiceError, make_handler


class LearningGatewayTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.sources = QuranSources()

    def test_corpus_and_explicit_source_retrieval(self):
        self.assertEqual(6236, len(self.sources.by_ref))
        found = self.sources.retrieve("Jelaskan rujukan 1:7 dan 114:6")
        self.assertEqual([(1, 7), (114, 6)], [(v["surah"], v["ayah"]) for v in found[:2]])
        self.assertTrue(found[0]["arabic"])

    def test_first_verse_context_uses_numbered_text_without_chapter_opening(self):
        found = self.sources.retrieve("Explain 2:1")
        self.assertEqual("الٓمٓ", found[0]["arabic"])
        self.assertTrue(self.sources.by_ref[(1, 1)]["arabic"].startswith("بِسْمِ"))
        self.assertTrue(self.sources.by_ref[(9, 1)]["arabic"].startswith("بَرَآءَةٌ"))
        self.assertIn("بِسْمِ ٱللَّهِ", self.sources.by_ref[(27, 30)]["arabic"])
        self.assertEqual("قُلْ هُوَ ٱللَّهُ أَحَدٌ", self.sources.by_ref[(112, 1)]["arabic"])

    def test_unconfigured_never_calls_provider(self):
        service = LearningService(sources=self.sources, enabled=False, api_key="")
        with patch.object(service, "call_provider") as provider:
            with self.assertRaises(ServiceError) as ctx:
                service.ask({"question": "Explain 1:1", "language": "en"})
            self.assertEqual(503, ctx.exception.status)
            provider.assert_not_called()

    def test_reference_cannot_escape_supplied_verified_context(self):
        service = LearningService(sources=self.sources, enabled=True, api_key="test-only-placeholder")
        invalid = ({"surah": 1, "ayah": 8}, {"surah": 2, "ayah": 286}, {"surah": True, "ayah": 1})
        for item in invalid:
            with self.subTest(item=item), self.assertRaises(ServiceError):
                service.validate_answer({"answer": "Unreviewed explanation", "citations": [item]}, {(1, 1)})

    def test_answer_strips_untrusted_quotation_and_url_fields(self):
        service = LearningService(sources=self.sources)
        result = service.validate_answer({"answer": "Study this source with a qualified teacher.", "citations": [
            {"surah": 1, "ayah": 1, "quote": "NOT REAL SOURCE TEXT", "sourceUrl": "https://invalid.example"}
        ]}, {(1, 1)})
        self.assertEqual([{"surah": 1, "ayah": 1}], result["citations"])

    def test_enabled_mock_provider_receives_sources_but_key_stays_out_of_payload(self):
        service = LearningService(sources=self.sources, enabled=True, api_key="test-only-placeholder")
        with patch.object(service, "call_provider", return_value={"answer": "Please consult the source.", "citations": [{"surah": 1, "ayah": 1}]}) as provider:
            result = service.ask({"question": "Explain 1:1", "language": "en"})
        self.assertEqual(1, len(result["citations"]))
        payload = provider.call_args.args[0]
        self.assertFalse(payload["store"])
        self.assertNotIn("test-only-placeholder", json.dumps(payload))
        self.assertEqual(1, json.loads(payload["input"])["sources"][0]["ayah"])

    def test_invalid_question_does_not_call_provider(self):
        service = LearningService(sources=self.sources, enabled=True, api_key="test-only-placeholder")
        with patch.object(service, "call_provider") as provider:
            for body in ({"question": "x", "language": "en"}, {"question": "valid words", "language": "fr"}, {"question": ["bad"], "language": "en"}, {"question": "valid words", "language": []}):
                with self.assertRaises(ServiceError) as ctx:
                    service.ask(body)
                self.assertEqual(400, ctx.exception.status)
            provider.assert_not_called()

    def test_http_health_and_unconfigured_response(self):
        service = LearningService(sources=self.sources, enabled=False, api_key="")
        with ThreadingHTTPServer(("127.0.0.1", 0), make_handler(service)) as server:
            thread = threading.Thread(target=server.serve_forever, daemon=True)
            thread.start()
            try:
                base = f"http://127.0.0.1:{server.server_port}"
                with request.urlopen(base + "/health") as response:
                    self.assertEqual({"status": "ok", "aiConfigured": False}, json.load(response))
                body = json.dumps({"question": "Explain 1:1", "language": "en"}).encode()
                req = request.Request(base + "/v1/learn", data=body, headers={"Content-Type": "application/json"})
                with self.assertRaises(error.HTTPError) as ctx:
                    request.urlopen(req)
                self.assertEqual(503, ctx.exception.code)
                self.assertEqual({"error": "unconfigured"}, json.load(ctx.exception))
            finally:
                server.shutdown()
                thread.join(timeout=2)


if __name__ == "__main__":
    unittest.main()
