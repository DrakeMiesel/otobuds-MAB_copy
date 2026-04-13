# Contributing to Otobuds

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop) installed
- Git

That's it -- everything else (JDK, Android SDK, NDK) lives inside the dev container.

## Getting Started

```bash
git clone https://github.com/blukeg/otobuds.git
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
