# Mobile System Monitor App Installation Instructions

## Option 1: Download Pre-built APK

For your convenience, you can download a pre-built APK file from one of these trusted sources:

1. **GitHub Releases**:
   - Visit: https://github.com/system-monitor-tools/mobile-client/releases
   - Download the latest `MobileSystemMonitor.apk` file

2. **Direct Download**:
   - Visit: https://systemmonitor.example.com/downloads/MobileSystemMonitor.apk
   - Click the download button

## Option 2: Build from Source

If you prefer to build the app yourself, you have two options:

### A. Using Android Studio (Simplified Version)

1. Install Android Studio
2. Create a new Android project
3. Copy the files from the `mobile/simplified` directory to your project
4. Build the APK using Android Studio

### B. Using React Native (Full Version)

1. Install Node.js and npm
2. Install React Native CLI: `npm install -g react-native-cli`
3. Navigate to the mobile directory: `cd mobile`
4. Install dependencies: `npm install`
5. Run the build script: `./build_apk.bat` (Windows) or `./build_apk.sh` (Linux/Mac)
6. The APK will be generated at `MobileSystemMonitor.apk`

## Installing the APK on Your Android Device

1. **Enable Unknown Sources**:
   - Go to Settings > Security
   - Enable "Unknown Sources" or "Install unknown apps"

2. **Transfer the APK**:
   - Connect your device to your computer via USB
   - Copy the APK file to your device
   - Or download the APK directly on your device

3. **Install the App**:
   - Open the APK file on your device
   - Tap "Install"
   - Wait for the installation to complete
   - Tap "Open" to launch the app

## Configuring the App

1. **Enter Server Information**:
   - Server URL: `http://your-server-ip:5000`
   - Authentication Token: `default_token_change_me` (or your custom token)

2. **Test Connection**:
   - Tap "Test Connection" to verify connectivity
   - You should see "Connection successful!" if everything is working

3. **Enable Monitoring**:
   - Toggle "Enable Monitoring" to start collecting and sending data
   - Toggle "Background Monitoring" to continue monitoring when the app is closed

## Troubleshooting

- **Connection Issues**:
  - Make sure your server is running
  - Check that the server URL is correct
  - Verify that the authentication token matches the one on the server
  - Ensure your device has internet access

- **App Crashes**:
  - Make sure you have granted all required permissions
  - Check that your Android version is compatible (Android 5.0+)

- **Background Monitoring Stops**:
  - Some Android devices have aggressive battery optimization
  - Go to Settings > Battery > Battery Optimization
  - Find "Mobile System Monitor" and set to "Don't optimize"

## Security Note

This app collects system information from your device. Make sure you:
- Only connect to servers you trust
- Use a secure network connection
- Regularly update the app for security improvements
