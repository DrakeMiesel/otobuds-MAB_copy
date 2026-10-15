"""Deterministic, offline replay of a four-band WAV manifest."""

import argparse
import hashlib
import io
import json
from pathlib import Path
import sys
import wave

import numpy as np

import dsp


def _keys(value, expected, name):
    if not isinstance(value, dict) or set(value) != set(expected):
        raise ValueError("%s must contain exactly %s" % (name, ", ".join(expected)))


def _read_wav(path, tone_seconds):
    data = path.read_bytes()
    with wave.open(io.BytesIO(data), "rb") as wav:
        if (wav.getnchannels(), wav.getsampwidth(), wav.getframerate(),
                wav.getcomptype()) != (1, 2, dsp.SAMPLING_RATE, "NONE"):
            raise ValueError("WAV must be mono, 16-bit PCM at 48000 Hz")
        frames = tone_seconds * dsp.SAMPLING_RATE
        if wav.getnframes() != frames:
            raise ValueError("WAV duration must equal tone_seconds exactly")
        pcm = wav.readframes(frames)
        if len(pcm) != frames * 2:
            raise ValueError("WAV sample data is truncated")
    return np.frombuffer(pcm, dtype="<i2"), hashlib.sha256(data).hexdigest()


def replay(manifest_path):
    path = Path(manifest_path)
    manifest = json.loads(path.read_text(encoding="utf-8"))
    _keys(manifest, ("schema_version", "age_months", "tone_seconds", "bands"),
          "manifest")
    if type(manifest["schema_version"]) is not int or manifest["schema_version"] != 1:
        raise ValueError("schema_version must be 1")
    age = manifest["age_months"]
    seconds = manifest["tone_seconds"]
    dsp.thresholds_for_age(age)
    dsp.noise_threshold(seconds)
    bands = manifest["bands"]
    if not isinstance(bands, list) or len(bands) != 4:
        raise ValueError("bands must be an ordered list of exactly four bands")
    f2, analyses, hashes = [], [], []
    for i, band in enumerate(bands):
        _keys(band, ("f2", "wav"), "band %d" % i)
        dsp.validate_frequencies(dsp.ProbeFrequencies(band["f2"]))
        if band["wav"] is not None and (
                not isinstance(band["wav"], str) or not band["wav"].strip()):
            raise ValueError("band %d wav must be a nonempty path or null" % i)
        f2.append(band["f2"])
    if len(set(f2)) != 4:
        raise ValueError("bands must have distinct f2 frequencies")
    for band in bands:
        if band["wav"] is None:
            analyses.append(None)
            hashes.append(None)
        else:
            samples, digest = _read_wav(path.parent / band["wav"], seconds)
            analyses.append(dsp.analyze(samples, dsp.ProbeFrequencies(band["f2"])))
            hashes.append(digest)
    measured = sum(a is not None for a in analyses)
    if not measured:
        raise ValueError("manifest must supply at least one WAV recording")
    scoring = dsp.screen(f2, analyses, age, seconds)
    output_bands = []
    for i, a in enumerate(analyses):
        dp = a.tones["dp"] if a is not None else None
        output_bands.append({
            "index": i, "f2_hz": f2[i], "measured": a is not None,
            "wav_sha256": hashes[i], "threshold_db": scoring.thresholds[i],
            "dp_signal_db": None if dp is None else dp.level_db,
            "dp_noise_db": None if dp is None else dp.noise_db,
            "snr_db": None if a is None else a.snr,
            "passes": scoring.passes[i], "noisy": scoring.noisy[i],
            "segments_used": None if a is None else a.segments_used,
            "segments_total": None if a is None else a.segments_total,
        })
    return {
        "schema_version": 1, "age_months": age, "tone_seconds": seconds,
        "sample_rate_hz": dsp.SAMPLING_RATE,
        "scoring_policy": "main-finite-unrounded-snr",
        "noise_policy": "android-default-phone",
        "numpy_version": np.__version__,
        "bands": output_bands, "measured_bands": measured,
        "passing_bands": sum(scoring.passes), "required_passing_bands": 3,
        "complete": measured == 4, "result": scoring.result,
        "noise_status": scoring.noise_status,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest", type=Path)
    args = parser.parse_args()
    try:
        output = replay(args.manifest)
        print(json.dumps(output, indent=2, sort_keys=True, allow_nan=False))
    except (OSError, ValueError, EOFError, wave.Error) as error:
        print("replay: %s" % error, file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
