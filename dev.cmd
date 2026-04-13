@echo off
REM Launch the Otobuds development container
REM Usage: dev.cmd [build]
REM   dev.cmd        - Start the container (builds image if needed)
REM   dev.cmd build  - Rebuild the container image from scratch

echo === Otobuds Dev Environment ===

REM Check if Docker is running
docker info >nul 2>&1
if not errorlevel 1 goto :docker_ready

echo Docker is not running. Starting Docker Desktop...
start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"
echo Waiting for Docker to start...

:wait_loop
timeout /t 5 /nobreak >nul
docker info >nul 2>&1
if errorlevel 1 goto :wait_loop
echo Docker is ready.

:docker_ready

REM Build image if "build" argument passed
if "%1"=="build" (
    echo Rebuilding container image...
    docker build -t otobuds-dev .devcontainer
    goto :run
)

REM Build image if it doesn't exist yet
docker image inspect otobuds-dev >nul 2>&1
if errorlevel 1 (
    echo First run - building container image...
    docker build -t otobuds-dev .devcontainer
)

:run
echo Starting dev container...
docker run -it --rm -v "%CD%:/workspace" -w /workspace otobuds-dev bash
