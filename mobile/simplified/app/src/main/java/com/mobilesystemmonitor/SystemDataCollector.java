package com.mobilesystemmonitor;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;

import org.json.JSONObject;

import java.io.File;
import java.util.UUID;

public class SystemDataCollector {

    public static JSONObject collectData(Context context) {
        try {
            JSONObject data = new JSONObject();
            
            // Add timestamp
            data.put("timestamp", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                    .format(new java.util.Date()));
            
            // Add device ID
            String deviceId = getDeviceId(context);
            data.put("deviceId", deviceId);
            
            // Add device info
            JSONObject deviceInfo = collectDeviceInfo(context);
            data.put("deviceInfo", deviceInfo);
            
            // Add battery info
            JSONObject batteryInfo = collectBatteryInfo(context);
            data.put("batteryInfo", batteryInfo);
            
            // Add storage info
            JSONObject storageInfo = collectStorageInfo();
            data.put("storageInfo", storageInfo);
            
            // Add memory info
            JSONObject memoryInfo = collectMemoryInfo(context);
            data.put("memoryInfo", memoryInfo);
            
            // Add network info
            JSONObject networkInfo = collectNetworkInfo(context);
            data.put("networkInfo", networkInfo);
            
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }
    
    private static String getDeviceId(Context context) {
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId == null || androidId.isEmpty() || androidId.equals("9774d56d682e549c")) {
            // Use a UUID if the Android ID is not available or is the emulator ID
            return UUID.randomUUID().toString();
        }
        return UUID.nameUUIDFromBytes(androidId.getBytes()).toString();
    }
    
    private static JSONObject collectDeviceInfo(Context context) {
        try {
            JSONObject deviceInfo = new JSONObject();
            
            deviceInfo.put("manufacturer", Build.MANUFACTURER);
            deviceInfo.put("model", Build.MODEL);
            deviceInfo.put("deviceName", Build.DEVICE);
            deviceInfo.put("systemName", "Android");
            deviceInfo.put("systemVersion", Build.VERSION.RELEASE);
            deviceInfo.put("apiLevel", Build.VERSION.SDK_INT);
            deviceInfo.put("deviceType", isTablet(context) ? "tablet" : "phone");
            
            return deviceInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }
    
    private static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout 
                & android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK) 
                >= android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
    
    private static JSONObject collectBatteryInfo(Context context) {
        try {
            JSONObject batteryInfo = new JSONObject();
            
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);
            
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPct = level * 100 / (float)scale;
            
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                 status == BatteryManager.BATTERY_STATUS_FULL;
            
            batteryInfo.put("level", batteryPct / 100.0f);
            batteryInfo.put("charging", isCharging);
            
            return batteryInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }
    
    private static JSONObject collectStorageInfo() {
        try {
            JSONObject storageInfo = new JSONObject();
            
            File path = Environment.getDataDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSizeLong();
            long totalBlocks = stat.getBlockCountLong();
            long availableBlocks = stat.getAvailableBlocksLong();
            
            long totalSize = totalBlocks * blockSize;
            long availableSize = availableBlocks * blockSize;
            long usedSize = totalSize - availableSize;
            
            storageInfo.put("total", totalSize);
            storageInfo.put("free", availableSize);
            storageInfo.put("used", usedSize);
            storageInfo.put("usedPercentage", (usedSize * 100.0f) / totalSize);
            
            return storageInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }
    
    private static JSONObject collectMemoryInfo(Context context) {
        try {
            JSONObject memoryInfo = new JSONObject();
            
            ActivityManager actManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            actManager.getMemoryInfo(memInfo);
            
            long totalMemory = memInfo.totalMem;
            long availableMemory = memInfo.availMem;
            long usedMemory = totalMemory - availableMemory;
            
            memoryInfo.put("total", totalMemory);
            memoryInfo.put("free", availableMemory);
            memoryInfo.put("used", usedMemory);
            memoryInfo.put("usedPercentage", (usedMemory * 100.0f) / totalMemory);
            
            return memoryInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }
    
    private static JSONObject collectNetworkInfo(Context context) {
        try {
            JSONObject networkInfo = new JSONObject();
            
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            
            String type = "unknown";
            if (activeNetwork != null) {
                if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                    type = "wifi";
                } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                    type = "cellular";
                } else if (activeNetwork.getType() == ConnectivityManager.TYPE_ETHERNET) {
                    type = "ethernet";
                }
            }
            
            networkInfo.put("isConnected", isConnected);
            networkInfo.put("type", type);
            
            return networkInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }
}
