# Offline scoring and WAV replay

Engineering regression tool, **not a validated hearing-screening or diagnostic
device**. It never opens an audio device, changes calibration, or writes recordings.
No real patient audio is included.

This is a curated subset of viyer's `python-debug-tool` DSP at commit
`4bc7b27`, not a merge of that branch. It reuses the one-second trim,
time-domain segment averaging, FFT noise windows, and artifact-rejection
behavior ported from Android `Signal.java`. The GUI, live single-tone work,
calibration, CSV export, error bars, and experimental detectors remain out of
scope. There are no Android runtime changes.

## Run

Use Python 3.9-3.12 from the repository root:

```console
python -m venv .venv
# Activate .venv using your shell's activation command.
python -m pip install -r tools/dpoae_debug/requirements.txt
python tools/dpoae_debug/replay.py test-data/replay/manifest.json
python -m unittest discover -s tools/dpoae_debug -v
```

NumPy is the only dependency and is pinned for repeatable replay. The Java
scoring regressions run without an Android SDK:

```console
./gradlew -p tools/scoring-tests test
```

Use `gradlew.bat` on Windows.

## Manifest and output

`test-data/replay/manifest.json` is the schema-version-1 example:

- `age_months`: nonnegative integer **total** age in months.
- `tone_seconds`: integer >= 2, including the initial discarded second.
- `bands`: exactly four ordered `{ "f2": <integer Hz>, "wav": <path or null> }`
  entries. Their order selects the four threshold positions. Use distinct f2
  frequencies; every primary, DP, and noise window must fit below Nyquist.
  The example uses the app defaults `[2016, 2953, 3985, 4969]`.

Paths resolve relative to the manifest (absolute paths also work). A null
recording means unmeasured, never a passing band. At least one WAV is required.
Unknown manifest fields are errors. Recordings must be mono signed 16-bit
little-endian PCM WAV, 48000 Hz, with **exactly** `tone_seconds * 48000`
frames. No resampling, downmixing, cropping, or duration guessing is performed.
Malformed, truncated, silent post-trim, or non-finite-DP recordings fail
explicitly; CLI failures use stderr and exit code 2 without result JSON.

Output is sorted JSON without timestamps or absolute paths. It includes the
WAV byte SHA-256, NumPy version, sample rate, age, duration, raw unrounded DP
signal/noise/SNR, per-band threshold/decision, segment counts, measured and
passing counts, completeness, and overall decision. Repeated replay in the
same environment is byte-identical. Numeric regression comparisons across
platforms use a tolerance (1e-8 dB); this is not a claim of bit-identical FFTs
on every architecture.

Scoring matches `main`'s `ResultScoring`: a measured band passes only for a
**finite, unrounded** SNR >= its threshold; three passing bands are required.
Age <= 6 months uses `[5, 4, 4, 4]` dB; older uses `[6, 6, 11, 10]`.
An incomplete replay can still have three passing bands, as in the app;
`complete` reports separately whether all four were supplied.

The noise flag uses the **default-phone** policy
`80 - 10*log10(tone_seconds - 1) + 25`, only on failing bands. It does not
model phone-specific overrides. Spectrum levels are app FFT/PCM units, **not
calibrated dB SPL**. This tool does not validate probe fit, primary-tone quality,
microphone routing, or clinical sensitivity/specificity. Preserved segment
rejection quirks are not corrected here.

## Fixtures and parity boundaries

`test-data/scoring/cases.csv` is consumed by both the actual Java
`ResultScoring` and Python `screen`. It covers just below/at/above each
threshold, the infant/older age boundary, NaN, both infinities, and unmeasured
bands. Java tests consume the threshold column; Python additionally verifies
age-to-threshold selection. These are **scoring** parity tests, not a
cross-language end-to-end FFT equivalence claim.

`test-data/replay/synthetic-2953.wav` is a fixed 192044-byte, two-second
synthetic signal with f1/f2 primaries, phase-seeded broadband noise, and a DP
near 4.5 dB SNR. Its hash and expected raw metrics are in `expected.json`.
The same bytes fail band 1's older threshold of 6 dB and pass its infant
threshold of 4 dB; the incomplete one-band test still returns `Refer`.
The test asserts these decisions, metrics, hash, relative-path resolution,
and deterministic CLI JSON, along with malformed-input rejection.

To deliberately regenerate the synthetic WAV:

```console
python tools/dpoae_debug/generate_fixture.py
```

Review any resulting byte/hash/metric changes rather than automatically
refreshing expected values. CI replays the checked-in WAV, not a newly
generated substitute.
