# Mobile System Monitor

This is an Android application that monitors system information on your mobile device and sends it to a central server.

## Building the APK

### Option 1: Using Android Studio (Recommended)

1. Open Android Studio
2. Select "Open an existing Android Studio project"
3. Navigate to this directory and select it
4. Wait for the project to sync
5. Select Build > Build Bundle(s) / APK(s) > Build APK(s)
6. The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`

### Option 2: Using Command Line

#### Prerequisites

- Java Development Kit (JDK) 8 or higher
- Android SDK with build tools
- ANDROID_HOME environment variable set to your Android SDK location

#### Build Steps

1. Open a command prompt or terminal
2. Navigate to this directory
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
2. Transfer the APK to your device
3. Open the APK on your device to install

## Configuring the App

1. Open the app
2. Enter your server URL (e.g., `http://your-server-ip:5000`)
3. Enter the authentication token (`default_token_change_me` or your custom token)
4. Test the connection
5. Enable monitoring

## Features

- Collects device information (CPU, memory, battery, storage, network)
- Securely transmits data to the monitoring server
- Runs in the background with minimal battery impact
- Stores data locally when offline and syncs when connection is restored

## Permissions

The app requires the following permissions:
- Internet access
- Network state
- Read phone state
- Storage access
- Battery stats
- Background execution

## Troubleshooting

- **Connection Issues**: Make sure your server is running and accessible from your device
- **Background Monitoring Stops**: Disable battery optimization for this app in your device settings
- **Missing Permissions**: Grant all required permissions in your device settings
