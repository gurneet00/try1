package com.mobilesystemmonitor;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MonitorService extends Service {
    private static final String TAG = "MonitorService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "SystemMonitorChannel";
    private static final String OFFLINE_DATA_DIR = "offline_data";
    private static final int MAX_OFFLINE_FILES = 100;
    private static final long DEFAULT_UPDATE_INTERVAL = 5 * 60 * 1000; // 5 minutes

    private Timer timer;
    private String serverUrl;
    private String authToken;
    private long updateInterval;
    private PowerManager.WakeLock wakeLock;
    private boolean isRunning = false;
    private int failedAttempts = 0;
    private static final int MAX_FAILED_ATTEMPTS = 5;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        // Create directory for offline data storage
        File offlineDir = new File(getFilesDir(), OFFLINE_DATA_DIR);
        if (!offlineDir.exists()) {
            offlineDir.mkdirs();
        }

        // Acquire wake lock to keep CPU running for our service
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MonitorService::WakeLock");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            serverUrl = intent.getStringExtra("serverUrl");
            authToken = intent.getStringExtra("authToken");
            updateInterval = intent.getLongExtra("updateInterval", DEFAULT_UPDATE_INTERVAL);
        } else {
            // If intent is null (service restarted by system), load from SharedPreferences
            SharedPreferences prefs = getSharedPreferences("SystemMonitorPrefs", MODE_PRIVATE);
            serverUrl = prefs.getString("serverUrl", "");
            authToken = prefs.getString("authToken", "");
            updateInterval = prefs.getLong("updateInterval", DEFAULT_UPDATE_INTERVAL);
        }

        // Save current settings to SharedPreferences
        SharedPreferences.Editor editor = getSharedPreferences("SystemMonitorPrefs", MODE_PRIVATE).edit();
        editor.putString("serverUrl", serverUrl);
        editor.putString("authToken", authToken);
        editor.putLong("updateInterval", updateInterval);
        editor.apply();

        startForeground(NOTIFICATION_ID, createNotification("Monitoring system..."));

        // Acquire wake lock
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }

        startMonitoring();

        return START_STICKY;
    }

    private void startMonitoring() {
        if (isRunning) {
            return;
        }

        if (timer != null) {
            timer.cancel();
        }

        isRunning = true;
        failedAttempts = 0;

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                collectAndSendData();

                // Also try to send any offline data
                sendOfflineData();
            }
        }, 0, updateInterval);

        Log.i(TAG, "Monitoring started with interval: " + updateInterval + "ms");
        updateNotification("Monitoring active - Interval: " + (updateInterval / 60000) + " min");
    }

    private void collectAndSendData() {
        try {
            // Collect system data
            JSONObject systemData = SystemDataCollector.collectData(this);

            // Check if we have internet connection
            if (!isNetworkAvailable()) {
                Log.w(TAG, "No network connection available, saving data offline");
                saveDataOffline(systemData);
                updateNotification("No network - Data saved offline");
                return;
            }

            // Send data to server
            try {
                URL url = new URI(serverUrl + "/api/system-data").toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + authToken);
                connection.setConnectTimeout(15000); // 15 seconds timeout
                connection.setReadTimeout(15000);
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = systemData.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();

                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }

                if (responseCode == 200) {
                    Log.d(TAG, "Data sent successfully");
                    failedAttempts = 0;
                    updateNotification("Monitoring active - Last update: " +
                            new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));
                } else {
                    Log.e(TAG, "Failed to send data: " + responseCode);
                    failedAttempts++;
                    saveDataOffline(systemData);
                    updateNotification("Error sending data - Code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending data", e);
                failedAttempts++;
                saveDataOffline(systemData);
                updateNotification("Error sending data: " + e.getMessage());
            }

            // If we've had too many failed attempts, adjust the update interval
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                adjustUpdateInterval();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error collecting data", e);
            updateNotification("Error collecting data: " + e.getMessage());
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void saveDataOffline(JSONObject data) {
        try {
            // Create a unique filename based on timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String filename = "data_" + timestamp + ".json";

            File offlineDir = new File(getFilesDir(), OFFLINE_DATA_DIR);
            File dataFile = new File(offlineDir, filename);

            // Write data to file
            try (FileWriter writer = new FileWriter(dataFile)) {
                writer.write(data.toString());
            }

            Log.i(TAG, "Data saved offline: " + filename);

            // Clean up old files if we have too many
            cleanupOfflineFiles();

        } catch (Exception e) {
            Log.e(TAG, "Error saving data offline", e);
        }
    }

    private void cleanupOfflineFiles() {
        try {
            File offlineDir = new File(getFilesDir(), OFFLINE_DATA_DIR);
            File[] files = offlineDir.listFiles();

            if (files != null && files.length > MAX_OFFLINE_FILES) {
                // Sort files by last modified time
                List<File> fileList = new ArrayList<>();
                for (File file : files) {
                    fileList.add(file);
                }

                fileList.sort((f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));

                // Delete oldest files
                int filesToDelete = fileList.size() - MAX_OFFLINE_FILES;
                for (int i = 0; i < filesToDelete; i++) {
                    File file = fileList.get(i);
                    if (file.delete()) {
                        Log.d(TAG, "Deleted old offline file: " + file.getName());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up offline files", e);
        }
    }

    private void sendOfflineData() {
        if (!isNetworkAvailable()) {
            return;
        }

        try {
            File offlineDir = new File(getFilesDir(), OFFLINE_DATA_DIR);
            File[] files = offlineDir.listFiles();

            if (files == null || files.length == 0) {
                return;
            }

            int successCount = 0;
            int failCount = 0;

            for (File file : files) {
                try {
                    // Read the file
                    StringBuilder content = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            content.append(line);
                        }
                    }

                    // Parse the JSON
                    JSONObject data = new JSONObject(content.toString());

                    // Send to server
                    URL url = new URI(serverUrl + "/api/system-data").toURL();
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Authorization", "Bearer " + authToken);
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(15000);
                    connection.setDoOutput(true);

                    try (OutputStream os = connection.getOutputStream()) {
                        byte[] input = data.toString().getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }

                    int responseCode = connection.getResponseCode();

                    if (responseCode == 200) {
                        // Success, delete the file
                        if (file.delete()) {
                            Log.d(TAG, "Offline data sent and file deleted: " + file.getName());
                            successCount++;
                        }
                    } else {
                        Log.w(TAG, "Failed to send offline data: " + file.getName() + ", code: " + responseCode);
                        failCount++;
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error processing offline file: " + file.getName(), e);
                    failCount++;
                }
            }

            if (successCount > 0) {
                Log.i(TAG, "Sent " + successCount + " offline files, " + failCount + " failed");
                updateNotification("Sent " + successCount + " offline files");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error sending offline data", e);
        }
    }

    private void adjustUpdateInterval() {
        // If we've had too many failures, increase the interval to reduce battery usage
        long newInterval = Math.min(updateInterval * 2, 30 * 60 * 1000); // Max 30 minutes

        if (newInterval != updateInterval) {
            updateInterval = newInterval;

            // Save the new interval
            SharedPreferences.Editor editor = getSharedPreferences("SystemMonitorPrefs", MODE_PRIVATE).edit();
            editor.putLong("updateInterval", updateInterval);
            editor.apply();

            // Restart the timer with the new interval
            if (timer != null) {
                timer.cancel();
            }

            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    collectAndSendData();
                    sendOfflineData();
                }
            }, updateInterval, updateInterval);

            Log.i(TAG, "Adjusted update interval to: " + updateInterval + "ms due to connection failures");
            updateNotification("Reduced update frequency due to connection issues");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "System Monitor Service",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Background monitoring service");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
                .setContentTitle("Mobile System Monitor")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, createNotification(text));
    }

    @Override
    public void onDestroy() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        // Release wake lock if held
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        isRunning = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
