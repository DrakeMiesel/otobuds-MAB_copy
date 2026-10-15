import copy
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import wave

import replay

FIXTURE = Path(__file__).resolve().parents[2] / "test-data/replay"


class ReplayTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.manifest = json.loads((FIXTURE / "manifest.json").read_text())
        self.manifest["bands"][1]["wav"] = str(FIXTURE / "synthetic-2953.wav")
        self.path = self.root / "manifest.json"

    def run_manifest(self, manifest=None):
        self.path.write_text(json.dumps(self.manifest if manifest is None else manifest))
        return replay.replay(self.path)

    def test_fixed_wav_metrics_hash_and_deterministic_replay(self):
        output = replay.replay(FIXTURE / "manifest.json")
        self.assertEqual(output, replay.replay(FIXTURE / "manifest.json"))
        expected = json.loads((FIXTURE / "expected.json").read_text())
        band = output["bands"][1]
        for key in ("dp_signal_db", "dp_noise_db", "snr_db"):
            self.assertAlmostEqual(band[key], expected[key], places=8)
        self.assertEqual(band["wav_sha256"], expected["wav_sha256"])
        self.assertEqual(band["wav_sha256"],
                         hashlib.sha256((FIXTURE / "synthetic-2953.wav").read_bytes()).hexdigest())
        self.assertEqual((band["segments_used"], band["segments_total"]), (1, 1))
        self.assertEqual(output["passing_bands"], 0)
        self.assertEqual(output["measured_bands"], 1)
        self.assertFalse(output["complete"])
        self.assertEqual(output["result"], "Refer")
        self.assertFalse(any(b["passes"] for b in output["bands"]))
        self.assertIsNone(output["bands"][0]["snr_db"])
        self.assertFalse(band["noisy"])

    def test_same_wav_age_boundary_changes_decision_not_raw_snr(self):
        older = self.run_manifest()
        self.manifest["age_months"] = 6
        infant = self.run_manifest()
        self.assertEqual(older["bands"][1]["snr_db"], infant["bands"][1]["snr_db"])
        self.assertEqual((older["bands"][1]["threshold_db"], infant["bands"][1]["threshold_db"]),
                         (6, 4))
        self.assertFalse(older["bands"][1]["passes"])
        self.assertTrue(infant["bands"][1]["passes"])
        self.assertEqual(infant["passing_bands"], 1)

    def test_invalid_manifest_fields(self):
        for field, values in (
                ("schema_version", [True, 2, "1"]),
                ("age_months", [True, -1, 6.0, None]),
                ("tone_seconds", [True, 1, 2.5, "2"]),
                ("bands", [None, [], self.manifest["bands"][:3]])):
            for value in values:
                bad = copy.deepcopy(self.manifest)
                bad[field] = value
                with self.subTest(field=field, value=value), self.assertRaises(ValueError):
                    self.run_manifest(bad)
        for f2 in (True, 100, 30000, 2953.0, None):
            bad = copy.deepcopy(self.manifest)
            bad["bands"][1]["f2"] = f2
            with self.subTest(f2=f2), self.assertRaises(ValueError):
                self.run_manifest(bad)
        for wav in ("", 5, True):
            bad = copy.deepcopy(self.manifest)
            bad["bands"][1]["wav"] = wav
            with self.subTest(wav=wav), self.assertRaises(ValueError):
                self.run_manifest(bad)
        for bad in ([], dict(self.manifest, extra=True)):
            with self.assertRaises(ValueError):
                self.run_manifest(bad)
        self.manifest["bands"][1]["wav"] = None
        with self.assertRaisesRegex(ValueError, "at least one"):
            self.run_manifest()

    def test_missing_wav(self):
        self.manifest["bands"][1]["wav"] = "missing.wav"
        with self.assertRaises(FileNotFoundError):
            self.run_manifest()

    def test_invalid_wav_formats_lengths_and_silence(self):
        for channels, width, rate, frames, silent in (
                (2, 2, 48000, 96000, False), (1, 1, 48000, 96000, False),
                (1, 2, 44100, 96000, False), (1, 2, 48000, 95999, False),
                (1, 2, 48000, 96001, False), (1, 2, 48000, 96000, True)):
            path = self.root / "bad.wav"
            with wave.open(str(path), "wb") as wav:
                wav.setnchannels(channels)
                wav.setsampwidth(width)
                wav.setframerate(rate)
                wav.writeframes(bytes(frames * channels * width))
            self.manifest["bands"][1]["wav"] = "bad.wav"
            with self.subTest(channels=channels, width=width, rate=rate,
                              frames=frames, silent=silent), self.assertRaises(ValueError):
                self.run_manifest()

    def test_truncated_and_corrupt_wav(self):
        data = (FIXTURE / "synthetic-2953.wav").read_bytes()
        for content in (data[:-2], b"not a WAV"):
            (self.root / "bad.wav").write_bytes(content)
            self.manifest["bands"][1]["wav"] = "bad.wav"
            with self.assertRaises((ValueError, wave.Error, EOFError)):
                self.run_manifest()

    def test_cli_json_and_failure_exit(self):
        command = [sys.executable, str(Path(replay.__file__)), str(FIXTURE / "manifest.json")]
        first = subprocess.run(command, capture_output=True, check=True)
        second = subprocess.run(command, capture_output=True, check=True)
        self.assertEqual(first.stdout, second.stdout)
        self.assertEqual(json.loads(first.stdout)["result"], "Refer")
        self.path.write_text("{broken")
        bad = subprocess.run(command[:2] + [str(self.path)], capture_output=True)
        self.assertEqual(bad.returncode, 2)
        self.assertEqual(bad.stdout, b"")
        self.assertIn(b"replay:", bad.stderr)


if __name__ == "__main__":
    unittest.main()
