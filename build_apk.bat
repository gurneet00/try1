@echo off
echo Building Mobile System Monitor APK...
echo.

REM Check if Node.js is installed
where node >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Node.js is not installed. Please install Node.js and try again.
    exit /b 1
)

REM Check if Java is installed
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Java is not installed. Please install Java and try again.
    exit /b 1
)

REM Create necessary directories
mkdir mobile\android\app\src\main\assets 2>nul

REM Install dependencies
echo Installing dependencies...
cd mobile
call npm install

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

REM Copy the APK to the root directory
echo Copying APK to root directory...
copy app\build\outputs\apk\release\app-release.apk ..\..\MobileSystemMonitor.apk
cd ..
cd ..

echo.
echo Build completed successfully!
echo The APK is available at: MobileSystemMonitor.apk
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
