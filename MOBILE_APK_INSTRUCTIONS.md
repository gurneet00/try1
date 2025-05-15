# Mobile System Monitor APK Instructions

## What's Included

I've prepared all the necessary files to build the Mobile System Monitor APK. The files are organized in the `mobile_apk_project` directory with the following structure:

```
mobile_apk_project/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── mobilesystemmonitor/
│   │       │           ├── MainActivity.java
│   │       │           ├── SystemDataCollector.java
│   │       │           ├── MonitorService.java
│   │       │           └── BootReceiver.java
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   │   └── activity_main.xml
│   │       │   └── values/
│   │       │       ├── strings.xml
│   │       │       └── styles.xml
│   │       └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
├── gradle.properties
└── README.md
```

## Building the APK

### Option 1: Using Android Studio (Recommended)

1. Install Android Studio if you don't have it already
2. Open Android Studio
3. Select "Open an existing Android Studio project"
4. Navigate to the `mobile_apk_project` directory and select it
5. Wait for the project to sync
6. Select Build > Build Bundle(s) / APK(s) > Build APK(s)
7. The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`

### Option 2: Using Command Line

#### Prerequisites

- Java Development Kit (JDK) 8 or higher
- Android SDK with build tools
- ANDROID_HOME environment variable set to your Android SDK location

#### Build Steps

1. Open a command prompt or terminal
2. Navigate to the `mobile_apk_project` directory
3. Run the following commands:

```
# On Windows
gradlew.bat assembleDebug

# On Linux/Mac
./gradlew assembleDebug
```

4. The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`

## Installing the APK

1. Enable "Install from Unknown Sources" in your device settings
   - Go to Settings > Security > Unknown Sources (or similar, depending on your Android version)
2. Transfer the APK to your device
   - Connect your device to your computer via USB
   - Copy the APK file to your device
   - Or use a file sharing app or email to transfer the APK
3. Open the APK on your device to install
   - Use a file manager app to navigate to where you saved the APK
   - Tap on the APK file
   - Follow the on-screen instructions to install

## Configuring the App

1. Open the app after installation
2. Enter your server URL
   - This should be the IP address or hostname of the computer running the server
   - For example: `http://192.168.1.100:5000`
3. Enter the authentication token
   - Use the same token you configured on the server
   - Default is `default_token_change_me`
4. Tap "Test Connection" to verify connectivity
5. Enable monitoring by toggling the switches

## Troubleshooting

- **Build Errors**: Make sure you have the latest version of Android Studio and the Android SDK
- **Installation Errors**: Check that you've enabled installation from unknown sources
- **Connection Issues**: Verify that your server is running and accessible from your device
- **Permission Errors**: Grant all required permissions when prompted

The final APK file will be named `app-debug.apk` or `app-release.apk` depending on the build type. You can rename it to `MobileSystemMonitor.apk` for clarity.
