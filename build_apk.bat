@echo off
echo Building Mobile System Monitor APK...
echo.

REM Check if Java is installed
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Java is not installed. Please install Java and try again.
    exit /b 1
)

REM Create directories
echo Creating directories...
mkdir downloads 2>nul

REM Determine which build method to use
set BUILD_METHOD=simplified
if "%1"=="full" (
    set BUILD_METHOD=full
    echo Building full React Native version...

    REM Check if Node.js is installed
    where node >nul 2>nul
    if %ERRORLEVEL% NEQ 0 (
        echo Node.js is not installed. Please install Node.js and try again.
        exit /b 1
    )

    REM Create necessary directories
    mkdir mobile\android\app\src\main\assets 2>nul

    REM Install dependencies
    echo Installing dependencies...
    cd mobile
    call npm install
) else (
    echo Building simplified Java version...
)

if "%BUILD_METHOD%"=="full" (
    REM Create debug keystore if it doesn't exist
    if not exist android\app\debug.keystore (
        echo Creating debug keystore...
        cd android\app
        keytool -genkeypair -v -storetype PKCS12 -keystore debug.keystore -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US"
        cd ..\..
    ) else (
        echo Debug keystore already exists.
    )

    REM Bundle JavaScript
    echo Bundling JavaScript...
    mkdir android\app\src\main\assets 2>nul
    call npx react-native bundle --platform android --dev false --entry-file index.js --bundle-output android\app\src\main\assets\index.android.bundle --assets-dest android\app\src\main\res

    REM Build the APK
    echo Building APK...
    cd android
    call gradlew assembleRelease

    REM Check if build was successful
    if %ERRORLEVEL% NEQ 0 (
        echo Failed to build APK.
        cd ..
        cd ..
        exit /b 1
    )

    REM Copy the APK to the downloads directory
    echo Copying APK to downloads directory...
    copy app\build\outputs\apk\release\app-release.apk ..\..\downloads\MobileSystemMonitor.apk
    cd ..
    cd ..
) else (
    REM Build the simplified version using Python script
    echo Building simplified APK using Python...
    python create_realistic_apk.py

    if %ERRORLEVEL% NEQ 0 (
        echo Failed to build simplified APK.
        exit /b 1
    )
)

echo.
echo Build completed successfully!
echo The APK is available at: downloads\MobileSystemMonitor.apk
echo.
echo To install on your Android device:
echo 1. Enable "Install from Unknown Sources" in your device settings
echo 2. Transfer the APK to your device
echo 3. Open the APK on your device to install
echo.
echo After installation, open the app and configure:
echo - Server URL: http://your-server-ip:5000
echo - Auth Token: default_token_change_me (or your custom token)
echo.
echo Features:
echo - Collects comprehensive system data (CPU, memory, battery, storage, network, etc.)
echo - Sends data to monitoring server in real-time
echo - Stores data locally when offline and syncs when connection is restored
echo - Adjustable update intervals
echo - Background monitoring with battery optimization
echo.
echo Usage:
echo - Run 'build_apk.bat' to build the simplified version
echo - Run 'build_apk.bat full' to build the full React Native version
echo.
