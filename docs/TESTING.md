# Otobuds Unit Testing Plan

## Goal
Build a test framework that validates DPOAE signal processing against synthetic audio data -- no hardware needed. Verify both positive cases (OAE present = healthy ear) and negative cases (no OAE = hearing loss).

## Why Refactor + Pure Unit Tests (Option 1)
- Tests run the **actual production code**, not mocks or substitutes
- Fast execution (milliseconds, no emulator or container needed)
- Best code coverage -- can test every math path including edge cases
- Refactors are small and low-risk (extract methods, no logic changes)

## Phase 1: Refactor for Testability

The core problem: `Signal.work()` mixes pure math with Android/JNI calls. Extract the pure math so it can be called independently.

### 1a. Extract `Signal.calcSNR()` to be independently callable
- Currently depends on class-level state (spectrum arrays)
- Refactor to accept spectrum data as a parameter
- No logic changes -- just make inputs explicit

### 1b. Extract `Signal.calcArt()` to be independently callable
- Same pattern -- make artifact detection a pure function
- Input: current spectrum + previous spectrum
- Output: artifact level (double)

### 1c. Extract segment processing from `Signal.work()`
- Create a method like `processSegment(double[] segment, double f1, double f2)`
- Accepts raw audio samples, returns SNR/signal/noise
- Internally calls FFT, but the **interface** is testable
- For unit tests, we can create an overridable FFT method (or pass an FFT interface)

### 1d. Create an FFT interface
- `interface FFTProvider { double[] fft(double[] input, int N); }`
- Production code uses `NativeFFTProvider` (FFTW3 via JNI)
- Tests use `JavaFFTProvider` (pure Java, e.g., Apache Commons Math or simple DFT)
- This is the one place we use a substitute -- but the math being tested (SNR, artifact rejection, thresholding) is all real production code

### 1e. Decouple Constants from Android Context
- `Constants.init()` requires Android Context for device ID
- Extract static config (frequencies, thresholds, volumes) into a data class or builder
- Tests can construct config directly without Context

## Phase 2: Test Infrastructure

### 2a. Add test dependencies
- Add JUnit 5 to `app/build.gradle` (test scope only)
- Optionally add Apache Commons Math for pure-Java FFT in tests

### 2b. Create synthetic audio test data generator
- `TestSignalFactory` class in test sources
- Methods:
  - `createCleanDPOAE(f1, f2, oaeAmplitude, noiseFloor, durationSec, sampleRate)` -- positive case
  - `createNoDPOAE(f1, f2, noiseFloor, durationSec, sampleRate)` -- negative case
  - `createNoisyDPOAE(f1, f2, oaeAmplitude, highNoise, durationSec, sampleRate)` -- edge case
  - `createWithArtifact(f1, f2, artifactAtTime, durationSec, sampleRate)` -- artifact rejection
- These generate `short[]` arrays identical in format to what `AudioRecord` produces

### 2c. Optionally: include recorded .wav test fixtures
- If you have real recordings from known-good and known-bad ears, save as test resources
- More realistic than synthetic data but harder to control precisely
- Can use both: synthetic for unit tests, real recordings for integration tests

## Phase 3: Write Tests

### 3a. SignalGeneratorTest (no refactoring needed)
- Verify sine wave frequency accuracy (FFT the output, check peak)
- Verify stereo interleaving (L/R channels correct)
- Verify chirp frequency sweep (start/end frequencies)
- Verify amplitude scaling (volume parameter)
- Verify duration (sample count)

### 3b. AngularMathTest (no refactoring needed)
- Normalize positive angles
- Normalize negative angles
- Normalize multiple wraps (e.g., 15*pi)
- Boundary: exactly 0, exactly 2*pi

### 3c. SignalSNRTest (requires Phase 1 refactors)
- **Positive:** Synthetic audio with OAE at 900Hz, SNR=20dB -> calcSNR returns SNR > 6
- **Negative:** Synthetic audio with no OAE -> calcSNR returns SNR < 0
- **Threshold boundary:** OAE at exactly SNR=6dB -> verify pass/refer decision
- **Multiple frequencies:** Test all 4 frequency pairs (f1/f2 combos)

### 3d. ArtifactRejectionTest (requires Phase 1 refactors)
- Clean signal -> artifact level below threshold
- Signal with sudden amplitude spike -> artifact detected, segment rejected
- Signal with gradual noise increase -> verify SNR dip detection

### 3e. CalibrationTest (Utils.compute)
- Known input amplitude -> verify computed TX level
- Different phone calibration profiles -> verify correct mapping
- Edge case: very low/very high input levels

### 3f. End-to-End Processing Test (requires Phase 1 refactors)
- Feed synthetic audio through full `processSegment()` pipeline
- Verify: clean OAE signal -> PASS result
- Verify: no OAE signal -> REFER result
- Verify: noisy signal -> segment rejected, test continues
- Verify: all 4 frequencies produce correct independent results

## Phase 4: CI Integration
- Tests run automatically via `./gradlew test` (already in GitHub Actions workflow)
- No additional CI changes needed

## Refactoring Risk Assessment
| Change | Risk | Reason |
|--------|------|--------|
| Extract calcSNR params | Very low | Same logic, explicit inputs |
| Extract calcArt params | Very low | Same logic, explicit inputs |
| FFT interface | Low | Thin wrapper, production path unchanged |
| Constants decoupling | Low | Static data extraction, no logic change |
| processSegment extraction | Medium | Largest refactor, needs careful testing |

## Dependency Order
1. Phase 2a (test deps) + Phase 3a-3b -- can start immediately, no refactoring needed
2. Phase 1a-1e (refactor) -- apply pending fixes first, then refactor
3. Phase 2b (test data generator) -- independent of refactoring
4. Phase 3c-3f (signal processing tests) -- requires Phase 1 refactors
5. Phase 4 (CI) -- already done, just works

## Testing Layers (All 3 Options)

The test suite uses three complementary layers, each catching different classes of bugs:

### Layer 1: Pure Unit Tests (refactored production code)
- **Runs:** `./gradlew test` (no container needed)
- **Speed:** Milliseconds
- **Catches:** Logic bugs in SNR calculation, thresholds, artifact rejection
- **FFT:** Uses Java FFT substitute (only for feeding data into testable methods)
- **Coverage:** ~70% of signal processing code

### Layer 2: Cross-Validation Tests (Java FFT vs FFTW3)
- **Runs:** Inside dev container (needs NDK for native lib)
- **Speed:** Seconds
- **Catches:** FFT integration bugs, precision differences, JNI data marshalling errors
- **Key test:** Feed identical synthetic audio through both Java FFT and FFTW3 native FFT, assert results match within tolerance
- **Why this matters:** If Java FFT says SNR=15dB and FFTW3 says SNR=15dB, you have high confidence. If they disagree, you found a real bug.

### Layer 3: Full Pipeline Instrumented Tests (real NDK path)
- **Runs:** Inside dev container or Android emulator
- **Speed:** Seconds to minutes
- **Catches:** JNI memory bugs, FFTW3 edge cases, end-to-end regressions
- **Key test:** Feed synthetic .wav files through the complete Signal.work() pipeline using real FFTW3, verify PASS/REFER decisions
- **Also validates:** Segment accumulation, multi-frequency independence, the full artifact rejection flow

### Cross-Validation Test Example
```
Given: synthetic DPOAE audio (f1=1900, f2=2900, OAE at 900Hz)

Java FFT path:   spectrum -> calcSNR -> SNR = 18.3 dB -> PASS
FFTW3 path:      spectrum -> calcSNR -> SNR = 18.3 dB -> PASS

Assert: |javaResult - nativeResult| < 0.1 dB  (tolerance for floating point)
Assert: both produce same PASS/REFER decision
```

## Notes
- Apply pending fixes from other contributors BEFORE starting Phase 1 refactors
- Phase 3a + 3b can be written immediately (no refactoring needed)
- The FFT interface (1d) is the key architectural decision -- it lets you swap FFT implementations for all 3 test layers
- Layer 1 runs in CI on every push (fast, no container). Layers 2+3 can run in CI too but are heavier.
- Real recorded audio files can supplement synthetic tests later but are not required to start
