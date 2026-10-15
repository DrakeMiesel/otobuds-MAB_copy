"""Regenerate the synthetic engineering fixture; never records audio."""

from pathlib import Path
import wave

import numpy as np

import dsp


def main():
    f = dsp.ProbeFrequencies(2953)
    n = dsp.SAMPLING_RATE
    rng = np.random.default_rng(15)
    phase = rng.uniform(0, 2 * np.pi, n // 2 + 1)
    spectrum = np.sqrt(n * 100.0 ** 2) * np.exp(1j * phase)
    spectrum[0] = spectrum[-1] = 0
    spectrum[f.f1] = 800 * n / 2
    spectrum[f.f2] = 400 * n / 2
    spectrum[f.dp] = np.sqrt(n * 100.0 ** 2 * 10 ** (4.5 / 10))
    pcm = np.tile(np.rint(np.fft.irfft(spectrum, n)).astype("<i2"), 2)
    path = Path(__file__).resolve().parents[2] / "test-data/replay/synthetic-2953.wav"
    with wave.open(str(path), "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(n)
        wav.writeframes(pcm.tobytes())


if __name__ == "__main__":
    main()
