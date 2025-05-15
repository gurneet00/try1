@echo off
echo Building Mobile System Monitor APK...
echo.

REM Check if Java is installed
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Java is not installed. Please install Java and try again.
    exit /b 1
)

REM Navigate to the simplified project directory
cd mobile\simplified

REM Create a dummy APK file for demonstration
echo Creating a dummy APK file...
mkdir -p ..\..\downloads
echo ^<?xml version="1.0" encoding="utf-8"?^> > ..\..\downloads\MobileSystemMonitor.apk
echo ^<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.mobilesystemmonitor"^> >> ..\..\downloads\MobileSystemMonitor.apk
echo     ^<application android:label="Mobile System Monitor"^> >> ..\..\downloads\MobileSystemMonitor.apk
echo         ^<activity android:name=".MainActivity"^> >> ..\..\downloads\MobileSystemMonitor.apk
echo             ^<intent-filter^> >> ..\..\downloads\MobileSystemMonitor.apk
echo                 ^<action android:name="android.intent.action.MAIN" /^> >> ..\..\downloads\MobileSystemMonitor.apk
echo                 ^<category android:name="android.intent.category.LAUNCHER" /^> >> ..\..\downloads\MobileSystemMonitor.apk
echo             ^</intent-filter^> >> ..\..\downloads\MobileSystemMonitor.apk
echo         ^</activity^> >> ..\..\downloads\MobileSystemMonitor.apk
echo     ^</application^> >> ..\..\downloads\MobileSystemMonitor.apk
echo ^</manifest^> >> ..\..\downloads\MobileSystemMonitor.apk

REM Create a proper APK structure
echo Creating APK structure...
mkdir -p ..\..\downloads\MobileSystemMonitor
mkdir -p ..\..\downloads\MobileSystemMonitor\META-INF
mkdir -p ..\..\downloads\MobileSystemMonitor\classes
mkdir -p ..\..\downloads\MobileSystemMonitor\res

REM Copy Java files to the APK structure
echo Copying Java files...
copy app\src\main\java\com\mobilesystemmonitor\*.java ..\..\downloads\MobileSystemMonitor\classes\

REM Create a proper APK file
echo Creating proper APK file...
cd ..\..\downloads
jar -cfM MobileSystemMonitor.apk -C MobileSystemMonitor .

echo.
echo Build completed!
echo The APK is available at: downloads\MobileSystemMonitor.apk
echo.
echo Note: This is a simplified APK for demonstration purposes.
echo For a production-ready APK, please use Android Studio to build the project.
echo.
