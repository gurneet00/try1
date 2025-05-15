# Mobile System Monitor App

## Overview

The Mobile System Monitor app collects system information from your Android device and sends it to a central server for monitoring. This allows you to track the performance and status of your mobile devices alongside your desktop systems.

## Installation Options

### Option 1: Download from Server

1. Visit your System Monitor server at `http://YOUR_SERVER_IP:5000/download`
2. Download the APK file
3. Enable "Install from Unknown Sources" in your device settings
4. Install the APK on your device

### Option 2: Build from Source

#### Prerequisites
- Android Studio
- JDK 8 or higher
- Android SDK

#### Steps
1. Open the `mobile/simplified` directory in Android Studio
2. Build the project
3. Install the generated APK on your device

## Troubleshooting "Parse Error"

If you encounter a "Parse Error" when installing the APK, try these solutions:

1. **Check Android Version Compatibility**
   - Make sure your device is running Android 5.0 (Lollipop) or higher

2. **Enable Unknown Sources**
   - Go to Settings > Security > Unknown Sources (or Settings > Apps > Special access > Install unknown apps)

3. **Clear Cache**
   - Go to Settings > Apps > Package Installer > Storage > Clear Cache

4. **Use a Different Installation Method**
   - Try installing the APK using a file manager app or ADB (Android Debug Bridge)

5. **Build a Fresh APK**
   - Follow the instructions in Option 2 to build a fresh APK using Android Studio

## Configuration

After installing the app, you'll need to configure it:

1. Open the app
2. Enter your server URL (e.g., `http://YOUR_SERVER_IP:5000`)
3. Enter the authentication token (default is `default_token_change_me`)
4. Test the connection
5. Enable monitoring

## Features

- **System Information Collection**
  - Device information (manufacturer, model, OS version)
  - Battery status
  - Storage usage
  - Memory usage
  - Network connectivity

- **Background Monitoring**
  - Continues to collect and send data even when the app is closed
  - Automatically starts on device boot (if enabled)

- **Secure Communication**
  - Uses token-based authentication
  - HTTPS support (if configured on the server)

## Permissions

The app requires the following permissions:

- Internet access (to send data to the server)
- Network state access (to check connectivity)
- Boot completed (to start monitoring on device boot)
- Foreground service (to run in the background)

## Building a Production APK

For a production-ready APK, use Android Studio:

1. Open the project in Android Studio
2. Select Build > Generate Signed Bundle / APK
3. Follow the wizard to create a signed APK
4. Use the release build variant for better performance

## Support

If you encounter any issues, please contact the system administrator or file an issue in the project repository.
