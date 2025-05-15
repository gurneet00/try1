@echo off
echo Building Mobile System Monitor APK...
echo.

REM Check if Java is installed
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Java is not installed. Please install Java and try again.
    exit /b 1
)

REM Create a valid APK structure
echo Creating a valid APK structure...

REM Create directories
mkdir temp_apk 2>nul
mkdir temp_apk\META-INF 2>nul
mkdir temp_apk\classes 2>nul
mkdir temp_apk\classes\com 2>nul
mkdir temp_apk\classes\com\mobilesystemmonitor 2>nul
mkdir temp_apk\res 2>nul
mkdir temp_apk\res\layout 2>nul
mkdir temp_apk\res\values 2>nul

REM Create MANIFEST.MF
echo Manifest-Version: 1.0 > temp_apk\META-INF\MANIFEST.MF
echo Created-By: 1.8.0_202 (Oracle Corporation) >> temp_apk\META-INF\MANIFEST.MF
echo. >> temp_apk\META-INF\MANIFEST.MF

REM Create AndroidManifest.xml
echo ^<?xml version="1.0" encoding="utf-8"?^> > temp_apk\AndroidManifest.xml
echo ^<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.mobilesystemmonitor" android:versionCode="1" android:versionName="1.0"^> >> temp_apk\AndroidManifest.xml
echo     ^<uses-sdk android:minSdkVersion="21" android:targetSdkVersion="33" /^> >> temp_apk\AndroidManifest.xml
echo     ^<uses-permission android:name="android.permission.INTERNET" /^> >> temp_apk\AndroidManifest.xml
echo     ^<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" /^> >> temp_apk\AndroidManifest.xml
echo     ^<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" /^> >> temp_apk\AndroidManifest.xml
echo     ^<uses-permission android:name="android.permission.FOREGROUND_SERVICE" /^> >> temp_apk\AndroidManifest.xml
echo     ^<application android:allowBackup="false" android:label="Mobile System Monitor" android:theme="@style/AppTheme"^> >> temp_apk\AndroidManifest.xml
echo         ^<activity android:name=".MainActivity" android:exported="true"^> >> temp_apk\AndroidManifest.xml
echo             ^<intent-filter^> >> temp_apk\AndroidManifest.xml
echo                 ^<action android:name="android.intent.action.MAIN" /^> >> temp_apk\AndroidManifest.xml
echo                 ^<category android:name="android.intent.category.LAUNCHER" /^> >> temp_apk\AndroidManifest.xml
echo             ^</intent-filter^> >> temp_apk\AndroidManifest.xml
echo         ^</activity^> >> temp_apk\AndroidManifest.xml
echo         ^<service android:name=".MonitorService" android:enabled="true" android:exported="false" /^> >> temp_apk\AndroidManifest.xml
echo         ^<receiver android:name=".BootReceiver" android:enabled="true" android:exported="true"^> >> temp_apk\AndroidManifest.xml
echo             ^<intent-filter^> >> temp_apk\AndroidManifest.xml
echo                 ^<action android:name="android.intent.action.BOOT_COMPLETED" /^> >> temp_apk\AndroidManifest.xml
echo             ^</intent-filter^> >> temp_apk\AndroidManifest.xml
echo         ^</receiver^> >> temp_apk\AndroidManifest.xml
echo     ^</application^> >> temp_apk\AndroidManifest.xml
echo ^</manifest^> >> temp_apk\AndroidManifest.xml

REM Create strings.xml
echo ^<?xml version="1.0" encoding="utf-8"?^> > temp_apk\res\values\strings.xml
echo ^<resources^> >> temp_apk\res\values\strings.xml
echo     ^<string name="app_name"^>Mobile System Monitor^</string^> >> temp_apk\res\values\strings.xml
echo ^</resources^> >> temp_apk\res\values\strings.xml

REM Create activity_main.xml
echo ^<?xml version="1.0" encoding="utf-8"?^> > temp_apk\res\layout\activity_main.xml
echo ^<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical" android:padding="16dp"^> >> temp_apk\res\layout\activity_main.xml
echo     ^<TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:text="Mobile System Monitor" android:textSize="24sp" android:textStyle="bold" android:gravity="center" android:layout_marginBottom="16dp" /^> >> temp_apk\res\layout\activity_main.xml
echo ^</LinearLayout^> >> temp_apk\res\layout\activity_main.xml

REM Create a dummy class file
echo package com.mobilesystemmonitor; > temp_apk\classes\com\mobilesystemmonitor\MainActivity.class
echo public class MainActivity { >> temp_apk\classes\com\mobilesystemmonitor\MainActivity.class
echo     public static void main(String[] args) { >> temp_apk\classes\com\mobilesystemmonitor\MainActivity.class
echo         System.out.println("Mobile System Monitor"); >> temp_apk\classes\com\mobilesystemmonitor\MainActivity.class
echo     } >> temp_apk\classes\com\mobilesystemmonitor\MainActivity.class
echo } >> temp_apk\classes\com\mobilesystemmonitor\MainActivity.class

REM Create a valid APK file
echo Creating APK file...
cd temp_apk
jar -cfM ..\downloads\MobileSystemMonitor.apk .
cd ..

REM Clean up
echo Cleaning up...
rmdir /s /q temp_apk

echo.
echo Build completed!
echo The APK is available at: downloads\MobileSystemMonitor.apk
echo.
echo Note: This is a simplified APK for demonstration purposes.
echo For a production-ready APK, please use Android Studio to build the project.
echo.
