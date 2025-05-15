package com.mobilesystemmonitor;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class SystemDataCollector {

    private static final String TAG = "SystemDataCollector";

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

            // Add CPU info
            JSONObject cpuInfo = collectCpuInfo();
            data.put("cpuInfo", cpuInfo);

            // Add sensor info
            JSONObject sensorInfo = collectSensorInfo(context);
            data.put("sensorInfo", sensorInfo);

            // Add running processes info
            JSONObject processInfo = collectProcessInfo(context);
            data.put("processInfo", processInfo);

            // Add location services info
            JSONObject locationInfo = collectLocationServicesInfo(context);
            data.put("locationInfo", locationInfo);

            // Add cellular network info
            JSONObject cellularInfo = collectCellularInfo(context);
            data.put("cellularInfo", cellularInfo);

            return data;
        } catch (Exception e) {
            Log.e(TAG, "Error collecting system data", e);
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

            if (activeNetwork != null) {
                networkInfo.put("subtype", activeNetwork.getSubtypeName());
                networkInfo.put("extraInfo", activeNetwork.getExtraInfo());
                networkInfo.put("roaming", activeNetwork.isRoaming());
                networkInfo.put("networkName", activeNetwork.getTypeName());
            }

            return networkInfo;
        } catch (Exception e) {
            Log.e(TAG, "Error collecting network info", e);
            return new JSONObject();
        }
    }

    private static JSONObject collectCpuInfo() {
        try {
            JSONObject cpuInfo = new JSONObject();

            // Read CPU info from /proc/cpuinfo
            StringBuilder sb = new StringBuilder();
            try {
                BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
            } catch (IOException e) {
                Log.e(TAG, "Error reading CPU info", e);
            }

            String cpuInfoStr = sb.toString();

            // Extract processor count
            int processorCount = 0;
            for (String line : cpuInfoStr.split("\n")) {
                if (line.startsWith("processor")) {
                    processorCount++;
                }
            }

            // Extract CPU model
            String model = "";
            for (String line : cpuInfoStr.split("\n")) {
                if (line.startsWith("model name") || line.startsWith("Processor")) {
                    String[] parts = line.split(":");
                    if (parts.length > 1) {
                        model = parts[1].trim();
                        break;
                    }
                }
            }

            cpuInfo.put("processorCount", processorCount);
            cpuInfo.put("model", model);

            // Get CPU usage
            try {
                float[] cpuUsage = getCpuUsage();
                cpuInfo.put("usage", cpuUsage[0]);
                cpuInfo.put("userUsage", cpuUsage[1]);
                cpuInfo.put("systemUsage", cpuUsage[2]);
                cpuInfo.put("idleUsage", cpuUsage[3]);
            } catch (Exception e) {
                Log.e(TAG, "Error getting CPU usage", e);
            }

            return cpuInfo;
        } catch (Exception e) {
            Log.e(TAG, "Error collecting CPU info", e);
            return new JSONObject();
        }
    }

    private static float[] getCpuUsage() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"));
            String line = reader.readLine();
            reader.close();

            if (line != null) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 5) {
                    long user = Long.parseLong(parts[1]);
                    long nice = Long.parseLong(parts[2]);
                    long system = Long.parseLong(parts[3]);
                    long idle = Long.parseLong(parts[4]);

                    long total = user + nice + system + idle;

                    float userPercent = (float) user / total * 100;
                    float systemPercent = (float) system / total * 100;
                    float idlePercent = (float) idle / total * 100;
                    float usagePercent = 100 - idlePercent;

                    return new float[] { usagePercent, userPercent, systemPercent, idlePercent };
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading CPU usage", e);
        }

        return new float[] { 0, 0, 0, 0 };
    }

    private static JSONObject collectSensorInfo(Context context) {
        try {
            JSONObject sensorInfo = new JSONObject();
            JSONArray sensors = new JSONArray();

            SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            List<Sensor> deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);

            for (Sensor sensor : deviceSensors) {
                JSONObject sensorObj = new JSONObject();
                sensorObj.put("name", sensor.getName());
                sensorObj.put("type", sensor.getType());
                sensorObj.put("vendor", sensor.getVendor());
                sensorObj.put("version", sensor.getVersion());
                sensorObj.put("power", sensor.getPower());
                sensorObj.put("resolution", sensor.getResolution());
                sensorObj.put("range", sensor.getMaximumRange());

                sensors.put(sensorObj);
            }

            sensorInfo.put("count", deviceSensors.size());
            sensorInfo.put("sensors", sensors);

            return sensorInfo;
        } catch (Exception e) {
            Log.e(TAG, "Error collecting sensor info", e);
            return new JSONObject();
        }
    }

    private static JSONObject collectProcessInfo(Context context) {
        try {
            JSONObject processInfo = new JSONObject();
            JSONArray processes = new JSONArray();

            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager.getRunningAppProcesses();

            if (runningProcesses != null) {
                // Limit to top 10 processes to avoid too much data
                int count = Math.min(runningProcesses.size(), 10);

                for (int i = 0; i < count; i++) {
                    ActivityManager.RunningAppProcessInfo process = runningProcesses.get(i);

                    JSONObject processObj = new JSONObject();
                    processObj.put("pid", process.pid);
                    processObj.put("processName", process.processName);
                    processObj.put("importance", process.importance);

                    processes.put(processObj);
                }
            }

            processInfo.put("count", runningProcesses != null ? runningProcesses.size() : 0);
            processInfo.put("topProcesses", processes);

            return processInfo;
        } catch (Exception e) {
            Log.e(TAG, "Error collecting process info", e);
            return new JSONObject();
        }
    }

    private static JSONObject collectLocationServicesInfo(Context context) {
        try {
            JSONObject locationInfo = new JSONObject();

            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

            boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            locationInfo.put("gpsEnabled", gpsEnabled);
            locationInfo.put("networkLocationEnabled", networkEnabled);
            locationInfo.put("locationServicesEnabled", gpsEnabled || networkEnabled);

            return locationInfo;
        } catch (Exception e) {
            Log.e(TAG, "Error collecting location services info", e);
            return new JSONObject();
        }
    }

    private static JSONObject collectCellularInfo(Context context) {
        try {
            JSONObject cellularInfo = new JSONObject();

            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            if (telephonyManager != null) {
                cellularInfo.put("networkOperator", telephonyManager.getNetworkOperatorName());
                cellularInfo.put("simOperator", telephonyManager.getSimOperatorName());
                cellularInfo.put("networkType", getNetworkTypeName(telephonyManager.getNetworkType()));
                cellularInfo.put("phoneType", getPhoneTypeName(telephonyManager.getPhoneType()));
                cellularInfo.put("isRoaming", telephonyManager.isNetworkRoaming());
            }

            return cellularInfo;
        } catch (Exception e) {
            Log.e(TAG, "Error collecting cellular info", e);
            return new JSONObject();
        }
    }

    private static String getNetworkTypeName(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS: return "GPRS";
            case TelephonyManager.NETWORK_TYPE_EDGE: return "EDGE";
            case TelephonyManager.NETWORK_TYPE_UMTS: return "UMTS";
            case TelephonyManager.NETWORK_TYPE_HSDPA: return "HSDPA";
            case TelephonyManager.NETWORK_TYPE_HSUPA: return "HSUPA";
            case TelephonyManager.NETWORK_TYPE_HSPA: return "HSPA";
            case TelephonyManager.NETWORK_TYPE_CDMA: return "CDMA";
            case TelephonyManager.NETWORK_TYPE_EVDO_0: return "EVDO_0";
            case TelephonyManager.NETWORK_TYPE_EVDO_A: return "EVDO_A";
            case TelephonyManager.NETWORK_TYPE_EVDO_B: return "EVDO_B";
            case TelephonyManager.NETWORK_TYPE_1xRTT: return "1xRTT";
            case TelephonyManager.NETWORK_TYPE_LTE: return "LTE";
            case TelephonyManager.NETWORK_TYPE_IDEN: return "IDEN";
            case TelephonyManager.NETWORK_TYPE_HSPAP: return "HSPAP";
            case TelephonyManager.NETWORK_TYPE_GSM: return "GSM";
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA: return "TD_SCDMA";
            case TelephonyManager.NETWORK_TYPE_IWLAN: return "IWLAN";
            case TelephonyManager.NETWORK_TYPE_NR: return "5G";
            default: return "UNKNOWN";
        }
    }

    private static String getPhoneTypeName(int phoneType) {
        switch (phoneType) {
            case TelephonyManager.PHONE_TYPE_GSM: return "GSM";
            case TelephonyManager.PHONE_TYPE_CDMA: return "CDMA";
            case TelephonyManager.PHONE_TYPE_SIP: return "SIP";
            case TelephonyManager.PHONE_TYPE_NONE: return "NONE";
            default: return "UNKNOWN";
        }
    }
}
