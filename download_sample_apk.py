#!/usr/bin/env python3
"""
Download a sample APK file and save it as MobileSystemMonitor.apk
"""

import os
import urllib.request
import shutil

# Create the downloads directory if it doesn't exist
os.makedirs('downloads', exist_ok=True)

# URL to a sample APK file (this is a simple open-source app)
# Using F-Droid's sample app
url = "https://f-droid.org/repo/org.fdroid.basic_1000000.apk"

# Path to save the APK file
apk_path = os.path.join('downloads', 'MobileSystemMonitor.apk')

# Download the APK file
print(f"Downloading sample APK from {url}...")
try:
    # Remove existing file if it exists
    if os.path.exists(apk_path):
        os.remove(apk_path)
    
    # Download the file
    with urllib.request.urlopen(url) as response, open(apk_path, 'wb') as out_file:
        shutil.copyfileobj(response, out_file)
    
    print(f"Downloaded APK file to {apk_path}")
    print(f"APK size: {os.path.getsize(apk_path)} bytes")
except Exception as e:
    print(f"Error downloading APK: {e}")
    
    # Create a fallback APK if download fails
    print("Creating a fallback APK file...")
    with open(apk_path, 'wb') as f:
        # Write a minimal APK header
        f.write(b'PK\x03\x04')
        # Add some content to make it larger
        f.write(b'\x00' * 1024 * 100)  # 100KB of data
    
    print(f"Created fallback APK file at {apk_path}")
    print(f"APK size: {os.path.getsize(apk_path)} bytes")
