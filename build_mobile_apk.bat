@echo off
echo Building Mobile System Monitor APK...
echo.

REM Check if Java is installed
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Java is not installed. Please install Java and try again.
    exit /b 1
)

REM Navigate to the project directory
cd mobile_apk_project

REM Check if Android SDK is available
if not defined ANDROID_HOME (
    echo ANDROID_HOME environment variable is not set.
    echo Please install Android SDK and set ANDROID_HOME.
    exit /b 1
)

REM Download Gradle wrapper if it doesn't exist
if not exist gradlew.bat (
    echo Downloading Gradle wrapper...
    call gradle wrapper
)

REM Build the APK
echo Building APK...
call gradlew assembleDebug

REM Check if build was successful
if %ERRORLEVEL% NEQ 0 (
    echo Failed to build APK.
    cd ..
    exit /b 1
)

REM Copy the APK to the root directory
echo Copying APK to root directory...
copy app\build\outputs\apk\debug\app-debug.apk ..\MobileSystemMonitor.apk
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
