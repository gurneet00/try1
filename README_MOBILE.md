# Mobile System Monitor

This project extends our System Monitoring Tool with a mobile client application that allows you to monitor mobile devices alongside desktop systems.

## Components

1. **Mobile Client Application** (`mobile/` directory)
   - React Native application for Android and iOS
   - Collects device information (CPU, memory, battery, storage, network)
   - Securely transmits data to the monitoring server
   - Runs in the background with minimal battery impact

2. **Enhanced Web Server** (`server.py`)
   - Updated to handle both desktop and mobile clients
   - Provides a responsive web interface for viewing system data
   - Distinguishes between mobile and desktop devices in the dashboard

## Mobile Client Features

- **Comprehensive Device Monitoring**: Collects data on device specs, battery, storage, memory, and network
- **Background Monitoring**: Continues to collect and send data even when the app is not in the foreground
- **Offline Support**: Stores data locally when offline and syncs when connection is restored
- **Low Battery Impact**: Optimized to minimize battery usage while monitoring
- **Secure Data Transmission**: Uses authentication tokens and can be configured to use HTTPS

## Setup Instructions

### Server Setup

1. Install Python 3.6+ if not already installed
2. Run the server with an authentication token:

```bash
python server.py --port 5000 --auth YOUR_SECRET_TOKEN
```

This will start the web server on port 5000. You can access the dashboard by opening a web browser and navigating to `http://localhost:5000`.

### Mobile Client Setup

#### Prerequisites

- Node.js and npm
- React Native CLI
- Android Studio (for Android development)
- Xcode (for iOS development, Mac only)

#### Installation

1. Install dependencies:

```bash
cd mobile
npm install
```

2. Configure the app:
   - Edit `mobile/config.js` to set your server URL and authentication token

3. Run on Android:

```bash
cd mobile
npx react-native run-android
```

4. Run on iOS (Mac only):

```bash
cd mobile
npx react-native run-ios
```

#### Building for Production

##### Android

```bash
cd mobile/android
./gradlew assembleRelease
```

The APK will be generated at `mobile/android/app/build/outputs/apk/release/app-release.apk`

##### iOS (Mac only)

Build using Xcode by opening the `mobile/ios/MobileSystemMonitor.xcworkspace` file and using the standard build process.

## Using the Mobile App

1. **Initial Setup**:
   - Launch the app
   - Enter your server URL and authentication token
   - Tap "Test Connection" to verify connectivity

2. **Monitoring**:
   - Toggle "Enable Monitoring" to start/stop data collection
   - Toggle "Background Monitoring" to enable/disable background collection
   - Tap "Update Now" to manually collect and send data

3. **Viewing Data**:
   - The app shows basic system information on the main screen
   - For detailed information, use the web dashboard

## Security Considerations

- Use a strong, unique authentication token
- For production use, configure the server with HTTPS
- Be aware of the sensitive nature of system information
- Only monitor systems you own or have permission to monitor
- Store data securely and limit access to the dashboard

## Troubleshooting

### Mobile App Issues

- **App crashes or freezes**: Check that you have the latest version of React Native and all dependencies
- **Cannot connect to server**: Verify server URL and authentication token, check network connectivity
- **Background monitoring stops**: Check battery optimization settings on your device
- **Missing permissions**: Ensure all required permissions are granted in device settings

### Server Issues

- **Server won't start**: Check that all required Python packages are installed
- **Cannot access dashboard**: Verify the server is running and accessible from your network
- **Mobile data not showing**: Check that the mobile client is properly configured and sending data

## License

This software is provided for educational and legitimate system administration purposes only. Use responsibly and only on systems you own or have permission to monitor.
