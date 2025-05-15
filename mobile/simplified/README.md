# Simplified Mobile System Monitor

This is a simplified version of the Mobile System Monitor app that can be compiled directly with Android Studio without requiring the full React Native setup.

## Files

- `MainActivity.java`: Main activity for the app
- `SystemDataCollector.java`: Utility class to collect system information
- `MonitorService.java`: Background service for continuous monitoring
- `BootReceiver.java`: Receiver to start the service on device boot
- `activity_main.xml`: Layout for the main activity
- `AndroidManifest.xml`: App manifest with permissions
- `strings.xml`: String resources
- `styles.xml`: Style resources
- `build.gradle`: Gradle build configuration

## How to Build

1. Create a new Android Studio project
2. Replace the default files with these files
3. Build the APK using Android Studio

## Features

- Collects device information (CPU, memory, battery, storage, network)
- Securely transmits data to the monitoring server
- Runs in the background with minimal battery impact
- Stores data locally when offline and syncs when connection is restored

## Configuration

In the app, you'll need to configure:
- Server URL: The URL of your monitoring server (e.g., http://your-server-ip:5000)
- Authentication Token: The token used to authenticate with the server

## Permissions

The app requires the following permissions:
- Internet access
- Network state
- Read phone state
- Storage access
- Battery stats
- Background execution

## Note

This is a simplified version of the app. For a more feature-rich version, use the React Native implementation.
