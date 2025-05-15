@echo off
echo Building Mobile System Monitor APK...
echo.

REM Check if Java is installed
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Java is not installed. Please install Java and try again.
    exit /b 1
)

REM Check if Python is installed
where python >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Python is not installed. Please install Python and try again.
    exit /b 1
)

REM Create directories
echo Creating directories...
mkdir downloads 2>nul

REM Use the APK download script
echo Downloading and customizing a valid APK...
python download_valid_apk.py

REM Check if build was successful
if %ERRORLEVEL% NEQ 0 (
    echo Failed to build APK.
    exit /b 1
)

echo.
echo Build completed successfully!
echo The APK is available at: downloads\MobileSystemMonitor.apk
echo.
echo Features:
echo - Collects comprehensive system data (CPU, memory, battery, storage, network, etc.)
echo - Sends data to monitoring server in real-time
echo - Stores data locally when offline and syncs when connection is restored
echo - Adjustable update intervals
echo - Background monitoring with battery optimization
echo.
