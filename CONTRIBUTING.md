# Contributing to Otobuds

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop) installed
- Git

That's it -- everything else (JDK, Android SDK, NDK) lives inside the dev container.

## Getting Started

```bash
git clone https://github.com/otobuds/otobuds.git
cd otobuds
dev
```

The `dev` script will:
1. Start Docker Desktop if it's not running
2. Build the container image on first run (~5 min)
3. Drop you into a shell with all build tools ready

## Building

Inside the container:

```bash
./gradlew assembleDebug       # Build the debug APK
./gradlew test                # Run unit tests
./gradlew clean               # Clean build outputs
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Daily Workflow

```
cd otobuds
dev                           # Enter the container
./gradlew assembleDebug       # Build
exit                          # Leave the container
```

## Rebuilding the Container

Only needed if the `Dockerfile` changes (e.g., SDK version upgrade):

```
dev build
```

## CI

GitHub Actions automatically builds and tests on every push to `main` and on pull requests. See `.github/workflows/build.yml`.

## Committing Changes

- **Author commits under your own GitHub alias** for this project (e.g. `blukeg`),
  not a default corporate alias:

  ```bash
  git config user.name  "blukeg"
  git config user.email "271641181+blukeg@users.noreply.github.com"
  ```

  The `<id>+<login>@users.noreply.github.com` form attributes the commit to your
  GitHub account without exposing a private email.
- **Do not include AI co-author trailers** (e.g. `Co-authored-by: Copilot ...`).
  Remove them before pushing.
- Write a clear subject line plus a body explaining *what* changed and *why*.

## Build Identification

So an installed APK can be traced back to the exact commit/calibration it came
from, every build stamps identifying info into the app, shown on the **Settings**
screen:

1. **Provenance** -- injected from git into `BuildConfig` in `app/build.gradle`:
   short SHA, branch, a readable redaction of the last commit subject, and build
   time. `versionName` includes the SHA, e.g. `1.0 (0b8a4ba)`.
   (Requires `buildFeatures.buildConfig = true` under Android Gradle Plugin 8.x.)
2. **Active calibration** -- resolved at runtime and shown in Settings: phone
   profile, f2 probe frequencies, and per-frequency volume defaults, e.g.
   `kassPhone | f2=1900/2900/3900/4900 Hz | vol=0.15/0.45/0.45/0.50`.

When changing per-device calibration, build and confirm the Settings summary
reflects the expected profile on the target device.

## Pull Requests

- Build succeeds (native ndkBuild + Java) and the Settings build/calibration
  summary is correct before opening the PR.
- Use the correct authenticated GitHub account
  (e.g. `gh auth switch --user blukeg`).
- Target the default branch (`main`) unless intentionally isolating onto a
  feature base.
- The PR description should state what it contains, why, and how it was verified.

## Probe presets

The Probe screen can save the current settings under a name, load a preset, or
reset only the probe settings to the existing phone defaults. Press **Done** to
apply each manual edit before saving. Editing a loaded preset shows **Custom**
until the current values are saved again. Preset actions are unavailable while
probe audio or a measurement is active. Saving is not calibration verification.

Storage reuses the `probe_profiles` JSON preference from `settings-volume-levels`:
each name maps to four frequencies and Overall/F1/F2 gains, optional reference
metadata, `onlyLeft`, `savedAt`, `formatVersion: 1`, and a stable UUID `id`.
Updating a name keeps its ID; saving under another name creates a new ID.
Legacy presets without a version/ID remain readable and gain an ID when saved;
ones without channel mode leave the current mode unchanged when loaded.
Unsupported or corrupt presets report an error rather than replacing stored
data. Older app builds can still drop fields they do not understand when saving.

Reference metadata is retained for unchanged settings, but cleared when the
same preset is saved with different frequencies, gains, or recorded channel
mode. Saving presets does not enable reference comparisons or automatic optimization.

Run the preset regressions with `./gradlew testDebugUnitTest`. They use
Robolectric with Android preferences and do not require a physical probe.

## Project Structure

```
otobuds/
+-- app/                      # Android app (Java + C++ NDK)
|   +-- src/main/java/        # Java source code
|   +-- src/main/cpp/         # C++ native code (FFTW3 signal processing)
|   +-- src/main/res/         # Android resources (layouts, icons, etc.)
|   +-- fftw3/                # Prebuilt FFTW3 static libraries
|   +-- Android.mk            # NDK build config
|   +-- build.gradle          # App-level Gradle config
+-- cad/                      # OpenSCAD earbud hardware designs
+-- .devcontainer/            # Dev container config (Docker)
+-- .github/workflows/        # CI pipeline
+-- build.gradle              # Root Gradle config
+-- dev.cmd                   # Dev environment launcher (Windows)
```
