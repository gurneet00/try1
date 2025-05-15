# Mobile System Monitor

A React Native application that monitors system information on mobile devices and sends it to a central server.

## Features

- Collects device information (CPU, memory, battery, storage, network)
- Securely transmits data to the monitoring server
- Runs in the background with minimal battery impact
- Provides a local view of system metrics
- Supports both Android and iOS (with some platform-specific limitations)

## Setup Instructions

### Prerequisites

- Node.js and npm
- React Native CLI
- Android Studio (for Android development)
- Xcode (for iOS development, Mac only)

### Installation

1. Install dependencies:

```bash
cd mobile
npm install
```

2. Start the Metro bundler:

```bash
npx react-native start
```

3. Run on Android:

```bash
npx react-native run-android
```

4. Run on iOS (Mac only):

```bash
npx react-native run-ios
```

## Configuration

Edit the `config.js` file to set your server URL and authentication token:

```javascript
export default {
  serverUrl: 'http://your-server-address:5000',
  authToken: 'your_secret_token',
  updateInterval: 60000, // milliseconds
};
```

## Building for Production

### Android

```bash
cd android
./gradlew assembleRelease
```

The APK will be generated at `android/app/build/outputs/apk/release/app-release.apk`

### iOS (Mac only)

Build using Xcode by opening the `ios/MobileSystemMonitor.xcworkspace` file and using the standard build process.

## Permissions

The app requires the following permissions:

- Internet access
- Read phone state
- Access network state
- Battery stats
- Storage access
- Background execution

## Technical Details

The app uses the following libraries:

- React Native Device Info: For collecting device metrics
- React Native Background Task: For background monitoring
- React Native Secure Storage: For storing authentication credentials
- Axios: For API communication

## Security Considerations

- All data is transmitted using HTTPS
- Authentication token is stored securely using the device's secure storage
- No sensitive personal data is collected

## Limitations

- Some system metrics may not be available on all devices due to OS restrictions
- iOS has more restrictions on background processing and system information access
- Battery optimization settings may affect background monitoring

## Troubleshooting

If you encounter issues with data collection:

1. Check that all required permissions are granted
2. Verify that battery optimization is disabled for the app
3. Ensure the server URL and authentication token are correct
4. Check your network connection
