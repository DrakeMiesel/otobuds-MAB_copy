"""Offline regressions, adapted from viyer's original DSP tests."""

import csv
from pathlib import Path
import unittest

import numpy as np

import dsp

ROOT = Path(__file__).resolve().parents[2]
F2 = [2016, 2953, 3985, 4969]


def analysis_with_snr(f2, snr, noise=0.0):
    tone = dsp.ToneLevel("dp", 0, snr + noise, noise, 0, 0)
    return dsp.Analysis(dsp.ProbeFrequencies(f2), 48000, np.array([]),
                        np.array([]), {"dp": tone}, 1, 1)


class DspTests(unittest.TestCase):
    def test_probe_frequencies(self):
        f = dsp.ProbeFrequencies(2016)
        self.assertEqual((f.f1, f.dp, f.dp2), (1652, 1288, 2380))
        f = dsp.ProbeFrequencies(4969)
        self.assertEqual(f.dp, 2 * int(4969 / 1.22) - 4969)

    def test_stimulus(self):
        f = dsp.ProbeFrequencies(2016)
        stereo = dsp.generate_stimulus(f, 2, .5, 1, 1)
        self.assertEqual(stereo.shape, (96000, 2))
        self.assertEqual(stereo.dtype, np.float32)
        self.assertLessEqual(np.max(np.abs(stereo)), 1)
        for channel, frequency in ((0, f.f1), (1, f.f2)):
            self.assertEqual(np.argmax(np.abs(np.fft.rfft(stereo[:, channel]))) / 2,
                             frequency)
        single = dsp.generate_stimulus(f, 2, .5, 1, 1, single_speaker=True)
        self.assertTrue(np.all(single[:, 1] == 0))
        self.assertTrue(np.allclose(dsp.generate_stimulus(f, 2, .25, 1, 1),
                                    stereo / 2))

    def test_calc_snr_synthetic_spectrum(self):
        spec = np.ones(48000)
        spec[1288] = 1000
        self.assertEqual(dsp.calc_snr(spec, 48000, 1288), (30, 0))

    def test_analyze_simulation_and_no_emission(self):
        f = dsp.ProbeFrequencies(2016)
        a = dsp.analyze(dsp.simulate_recording(f, 6, .5, 1, .2), f)
        b = dsp.analyze(dsp.simulate_recording(f, 6, .5, 1, .2, dp_level=0), f)
        self.assertEqual(a.segments_total, 5)
        self.assertGreater(a.snr, 10)
        self.assertLess(b.snr, a.snr / 2)
        self.assertEqual(a.tones["f1"].peak_hz, f.f1)
        self.assertEqual(a.tones["f2"].peak_hz, f.f2)
        self.assertEqual(len(a.spectrum_db), 24000)

    def test_int16_and_normalized_samples_agree_even_at_one_lsb(self):
        f = dsp.ProbeFrequencies(2016)
        rng = np.random.default_rng(5)
        for pcm in (rng.integers(-1, 2, 96000, dtype=np.int16),
                    (dsp.simulate_recording(f, 2, .5, 1, .2) * 32768).astype(np.int16)):
            a = dsp.analyze(pcm, f)
            b = dsp.analyze(pcm.astype(np.float64) / 32768, f)
            self.assertEqual(a.tones["dp"].level_db, b.tones["dp"].level_db)
            self.assertEqual(a.snr, b.snr)

    def test_invalid_audio(self):
        f = dsp.ProbeFrequencies(2016)
        for samples, message in (
                (np.ones(48000), "too short"),
                (np.zeros(96000, dtype=np.int16), "silent"),
                (np.ones(96000, dtype=np.int16), "non-finite DP"),
                (np.ones((96000, 2)), "mono"),
                (np.full(96000, np.nan), "non-finite"),
                (np.full(96000, np.inf), "non-finite"),
                (np.full(96000, 2.0), "in \\[-1, 1\\]"),
                (np.ones(96000, dtype=np.int32), "int16")):
            with self.subTest(message=message), self.assertRaisesRegex(ValueError, message):
                dsp.analyze(samples, f)
        with self.assertRaisesRegex(ValueError, "48000"):
            dsp.analyze(np.ones(96000), f, fs=44100)
        with self.assertRaisesRegex(ValueError, "Nyquist"):
            dsp.analyze(np.ones(96000), dsp.ProbeFrequencies(100))

    def test_artifact_rejection(self):
        f = dsp.ProbeFrequencies(2016)
        rec = dsp.simulate_recording(f, 6, .5, 1, .2)
        rec[4 * 48000:5 * 48000] += .5 * np.random.default_rng(42).standard_normal(48000)
        rec = np.clip(rec, -1, 1)
        a = dsp.analyze(rec, f)
        self.assertLess(a.segments_used, a.segments_total)

    def test_shared_java_scoring_vectors(self):
        with (ROOT / "test-data/scoring/cases.csv").open(newline="") as file:
            for row in csv.DictReader(file):
                with self.subTest(case=row["case"]):
                    index = int(row["band_index"])
                    analyses = [None] * 4
                    if row["measured"] == "true":
                        analyses[index] = analysis_with_snr(F2[index], float(row["snr"]))
                    result = dsp.screen(F2, analyses, int(row["age_months"]), 2)
                    self.assertEqual(result.thresholds[index], int(row["threshold"]))
                    self.assertEqual(result.passes[index], row["expected_pass"] == "true")
                    self.assertEqual(result.result, "Refer")

    def test_three_of_four_and_noise_policy(self):
        analyses = [analysis_with_snr(f, snr) for f, snr in zip(F2, (6, 6, 11, 0))]
        self.assertEqual(dsp.screen(F2, analyses, 7, 6).result, "Pass")
        analyses[1] = None
        self.assertEqual(dsp.screen(F2, analyses, 7, 6).result, "Refer")
        analyses[3] = analysis_with_snr(F2[3], 0, noise=110)
        result = dsp.screen(F2, analyses, 7, 6)
        self.assertEqual(result.noisy, [False, False, False, True])
        self.assertEqual(result.noise_status, "retry")
        self.assertAlmostEqual(dsp.noise_threshold(6), 98.0103, places=4)


if __name__ == "__main__":
    unittest.main()
