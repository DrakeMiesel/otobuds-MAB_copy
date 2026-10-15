"""Offline subset of viyer's python-debug-tool DSP (source commit 4bc7b27).

Mirrors Signal.java's FFT, time-domain averaging and artifact rejection.
Scoring follows main's ResultScoring: finite, unrounded SNR >= threshold.
Spectrum levels use the app's 16-bit sample units, not calibrated SPL.
"""

from dataclasses import dataclass, field
from typing import Dict, List, Optional, Tuple

import numpy as np

SAMPLING_RATE = 48000
INIT_SIGNAL_TRIM_SECONDS = 1.0
ART_THRESH = 10
SNR_DIP_THRESH = 1
F2_F1_RATIO = 1.22
FULL_SCALE = 32000
PEAK_SEARCH_HZ = 20
SNR_THRESHS_OLDER = [6, 6, 11, 10]
SNR_THRESHS_INFANT = [5, 4, 4, 4]
BAND_PASS_THRESH = 3
NOISE_THRESH_BASE = 80


@dataclass(frozen=True)
class ProbeFrequencies:
    f2: int

    @property
    def f1(self) -> int:
        return int(self.f2 / F2_F1_RATIO)

    @property
    def dp(self) -> int:
        return 2 * self.f1 - self.f2

    @property
    def dp2(self) -> int:
        return 2 * self.f2 - self.f1


def generate_stimulus(freqs: ProbeFrequencies, seconds: float, base_vol: float,
                      f1_vol: float, f2_vol: float, single_speaker: bool = False,
                      fs: int = SAMPLING_RATE) -> np.ndarray:
    """Synthetic stereo stimulus; never opens an audio device."""
    t = np.arange(int(seconds * fs)) / fs
    tone1 = np.sin(2 * np.pi * freqs.f1 * t)
    tone2 = np.sin(2 * np.pi * freqs.f2 * t)
    if single_speaker:
        total = f1_vol + f2_vol
        if total > 1:
            f1_vol, f2_vol = f1_vol / total, f2_vol / total
        left = f1_vol * tone1 + f2_vol * tone2
        right = np.zeros(len(t))
    else:
        left, right = f1_vol * tone1, f2_vol * tone2
    return (np.stack([left, right], axis=1) *
            (base_vol * FULL_SCALE / 32768.0)).astype(np.float32)


def power_spectrum(x: np.ndarray) -> np.ndarray:
    return np.abs(np.fft.fft(x)) ** 2


def calc_snr(spec: np.ndarray, seglen: int, tone: int,
             fs: int = SAMPLING_RATE) -> Tuple[float, float]:
    """Signal.calcSNR noise windows, including their original endpoints."""
    bin_hz = fs // seglen
    b = tone // bin_hz
    fwindow = int(np.ceil(200 / bin_hz))
    lo = spec[b - fwindow - 2:b - 2 + 1]
    hi = spec[b + 2:b + 2 + fwindow + 1]
    noise = np.concatenate([lo, hi]).mean()
    with np.errstate(divide="ignore"):
        return float(10 * np.log10(spec[b])), float(10 * np.log10(noise))


def _calc_art(spec: np.ndarray) -> float:
    with np.errstate(divide="ignore"):
        return float(np.mean(10 * np.log10(spec[:6000])))


@dataclass
class ToneLevel:
    name: str
    expected_hz: int
    level_db: float
    noise_db: float
    peak_hz: float
    peak_db: float

    @property
    def snr_db(self) -> float:
        return self.level_db - self.noise_db


@dataclass
class Analysis:
    freqs: ProbeFrequencies
    seglen: int
    spectrum_db: np.ndarray
    spectrum_hz: np.ndarray
    tones: Dict[str, ToneLevel]
    segments_used: int
    segments_total: int
    segment_snrs: List[float] = field(default_factory=list)
    ambient_db: float = 0.0

    @property
    def snr(self) -> float:
        return self.tones["dp"].snr_db


def validate_frequencies(freqs: ProbeFrequencies, fs: int = SAMPLING_RATE) -> None:
    if type(freqs.f2) is not int or any(
            not 202 <= tone < fs / 2 - 202
            for tone in (freqs.f1, freqs.f2, freqs.dp, freqs.dp2)):
        raise ValueError("f2 must place every tone and noise window within Nyquist")


def analyze(recording: np.ndarray, freqs: ProbeFrequencies,
            fs: int = SAMPLING_RATE) -> Analysis:
    """Analyze mono int16 PCM or normalized floating-point samples.

    Drop the first second, then average one-second segments in the time
    domain with the original Android rejection behavior.
    """
    if fs != SAMPLING_RATE:
        raise ValueError("sample rate must be 48000 Hz")
    validate_frequencies(freqs, fs)
    samples = np.asarray(recording)
    if samples.ndim != 1:
        raise ValueError("recording must be mono")
    is_pcm = samples.dtype.kind == "i" and samples.dtype.itemsize == 2
    if not is_pcm and samples.dtype.kind != "f":
        raise ValueError("recording must contain int16 PCM or normalized floats")
    x = samples.astype(np.float64)
    if not np.all(np.isfinite(x)):
        raise ValueError("recording contains non-finite samples")
    if not is_pcm:
        if np.max(np.abs(x), initial=0) > 1:
            raise ValueError("floating-point samples must be in [-1, 1]")
        x *= 32768.0
    seglen = fs
    trim = int(INIT_SIGNAL_TRIM_SECONDS * fs)
    numsegs = (len(x) - trim) // seglen
    if numsegs < 1:
        raise ValueError("recording too short: need at least 2 seconds")
    if not np.any(x[trim:trim + numsegs * seglen]):
        raise ValueError("recording is silent after the initial trim")

    total = np.zeros(seglen)
    avg = np.zeros(seglen)
    count = 0
    snrs: List[float] = []
    arts: List[float] = []
    for i in range(numsegs):
        seg = x[trim + i * seglen:trim + (i + 1) * seglen]
        total += seg
        count += 1
        avg = total / count
        spec = power_spectrum(avg)
        sig, noise = calc_snr(spec, seglen, freqs.dp, fs)
        snr = sig - noise
        art = _calc_art(spec)
        reject = snr < 0
        if arts:
            reject = reject or art - arts[-1] >= ART_THRESH or (
                snr < snrs[-1] and abs(snr - snrs[-1]) >= SNR_DIP_THRESH)
        if reject:
            total -= seg
            count -= 1
            # Preserve Signal.work's previous-average behavior on rejection.
            if count > 0:
                avg = total / count
            spec = power_spectrum(avg)
            sig, noise = calc_snr(spec, seglen, freqs.dp, fs)
            snr = sig - noise
            art = _calc_art(spec)
            if snrs:
                snrs[-1], arts[-1] = snr, art
                continue
        snrs.append(snr)
        arts.append(art)

    with np.errstate(divide="ignore"):
        spec_db = 10 * np.log10(spec)
    half = seglen // 2
    hz = np.arange(half) * fs / seglen
    tones = {}
    for name, tone in (("f1", freqs.f1), ("f2", freqs.f2),
                       ("dp", freqs.dp), ("dp2", freqs.dp2)):
        level, noise = calc_snr(spec, seglen, tone, fs)
        lo = tone - PEAK_SEARCH_HZ
        pk = lo + int(np.argmax(spec_db[lo:tone + PEAK_SEARCH_HZ + 1]))
        tones[name] = ToneLevel(name, tone, level, noise,
                                float(hz[pk]), float(spec_db[pk]))
    dp = tones["dp"]
    if not np.all(np.isfinite([dp.level_db, dp.noise_db, dp.snr_db])):
        raise ValueError("recording has non-finite DP signal, noise or SNR")
    return Analysis(freqs, seglen, spec_db[:half], hz, tones, count, numsegs,
                    snrs, float(np.mean(spec_db[:1000])))


def simulate_recording(freqs: ProbeFrequencies, seconds: float, base_vol: float,
                       f1_vol: float, f2_vol: float, dp_level: float = 3e-5,
                       noise_level: float = 1e-3, seed: Optional[int] = 0,
                       fs: int = SAMPLING_RATE) -> np.ndarray:
    rng = np.random.default_rng(seed)
    stim = generate_stimulus(freqs, seconds, base_vol, f1_vol, f2_vol, fs=fs)
    t = np.arange(len(stim)) / fs
    mic = 0.3 * stim.sum(axis=1)
    mic += dp_level * np.sin(2 * np.pi * freqs.dp * t)
    mic += noise_level * rng.standard_normal(len(t))
    return mic.astype(np.float32)


def thresholds_for_age(months: int) -> List[int]:
    if type(months) is not int or months < 0:
        raise ValueError("age_months must be a nonnegative integer")
    return list(SNR_THRESHS_INFANT if months <= 6 else SNR_THRESHS_OLDER)


def noise_threshold(tone_seconds: int) -> float:
    if type(tone_seconds) is not int or tone_seconds < 2:
        raise ValueError("tone_seconds must be an integer >= 2")
    return float(NOISE_THRESH_BASE - 10 * np.log10(tone_seconds - 1) + 25)


@dataclass
class ScreeningResult:
    thresholds: List[int]
    passes: List[bool]
    noisy: List[bool]
    result: str
    noise_status: str


def screen(f2: List[int], analyses: List[Optional[Analysis]], age_months: int,
           tone_seconds: int) -> ScreeningResult:
    """Default-phone noise policy and main's finite, unrounded scoring."""
    if len(f2) != 4 or len(analyses) != 4:
        raise ValueError("screen requires exactly four ordered bands")
    thresholds = thresholds_for_age(age_months)
    nthresh = noise_threshold(tone_seconds)
    passes, noisy = [], []
    for i, a in enumerate(analyses):
        ok = a is not None and np.isfinite(a.snr) and a.snr >= thresholds[i]
        passes.append(bool(ok))
        noisy.append(bool(a is not None and
                          a.tones["dp"].noise_db >= nthresh and not ok))
    return ScreeningResult(thresholds, passes, noisy,
                           "Pass" if sum(passes) >= BAND_PASS_THRESH else "Refer",
                           "retry" if any(noisy) else "noise-pass")
