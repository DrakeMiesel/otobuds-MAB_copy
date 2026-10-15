# Scoring bootstrap

This small desktop suite compiles the app's actual `ResultScoring.java`. It
requires JDK 11 or newer, but no Android SDK, phone, microphone, Python, or FFT.
It does not compile or replace the Android signal-processing pipeline.

From the repository root:

```powershell
.\gradlew.bat -p tools\scoring-tests test
```

On macOS/Linux:

```sh
./gradlew -p tools/scoring-tests test
```

## Baseline and intended change

The immutable comparison baseline is
[`baseline/kass-calibration-2026-10-15`](https://github.com/otobuds/otobuds/tree/baseline/kass-calibration-2026-10-15)
at `e388da6`. It contains Kassandra's calibration/frequency changes (`b02075a`)
and PR #2's build identification.

The intended scoring correction comes from Vikram's
[`eb6463c`](https://github.com/otobuds/otobuds/commit/eb6463c): compare the
unrounded SNR with the effective threshold. For example, 3.65 dB must not pass
a 4 dB threshold merely because rounding upward produces 4.

`ResultScoring` also rejects non-finite SNRs as passing bands. This is a
defensive addition to the finite-input rule, not a new detection algorithm.
The existing age thresholds and required passing-band count are unchanged.
Chart labels use one decimal, and chart pass flags use the original double
SNR rather than a rounded or float-converted display value. CSV band outcomes
use the same decision rule.

No FFT, artifact rejection, averaging, recording, volume calibration, or CSV
schema changes are included. Existing successful-band/early-stop state is
preserved; this suite is not a redesign of measurement state.

## Confidence gates

1. **Known-number regressions first.** Test values immediately below, exactly
   at, and above all current threshold values; both age-profile examples;
   non-finite input; and counting three successful bands versus two or none.
   The tests must fail with the legacy upward-rounding rule and pass with the
   unrounded rule. They exercise production decision code, not a Python port.
2. **Fixed WAV replay next.** Use the preserved desktop analysis work or the
   Python diagnostic tool as an external bench. Save raw WAVs with sample
   rate, f1/f2, age/effective thresholds, enabled bands, and recording settings.
   Replaying a file must give repeatable results. A scoring-only change must
   not alter calculated SNR; only the documented decision boundaries change.
   Correct the Python comparison's scoring inconsistencies before using it
   for parity checks. No WAV replay runner is added by this PR.
3. **Laptop microphone smoke checks.** Use an existing recorder to capture
   quiet input, background noise/speech, and a played tone, then save/replay
   WAVs. These establish acquisition/replay behavior, not calibrated probe
   performance or genuine OAE detection. No new capture UI or drivers are
   needed for this step.
4. **Android integration checks.** CI builds the app separately. On-device
   checks of UI, CSV parity, native FFT behavior, and calibrated hardware are
   still required before claiming end-to-end equivalence. Passing this
   desktop suite alone does not establish them.

Missing primary tones and microphone failure cannot be diagnosed from a
scalar SNR alone. Full recording-validity outcomes, the new probe checker,
automatic volume optimization, error bars, and the expanded CSV format remain
separate integration work.

## Preserved work

The original broad PR #1 implementation is retained at
[`user/blukeg/phase0-analysis-tool-backup-20261015`](https://github.com/otobuds/otobuds/tree/user/blukeg/phase0-analysis-tool-backup-20261015)
(`a3812e4`). It is not part of the narrowed PR's change relative to `main`.
See [PR #3](https://github.com/otobuds/otobuds/pull/3) for the broader Android
and Python integration findings.
