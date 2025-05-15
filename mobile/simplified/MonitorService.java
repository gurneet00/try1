package com.mobilesystemmonitor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class MonitorService extends Service {
    private static final String TAG = "MonitorService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "SystemMonitorChannel";
    
    private Timer timer;
    private String serverUrl;
    private String authToken;
    
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            serverUrl = intent.getStringExtra("serverUrl");
            authToken = intent.getStringExtra("authToken");
        }
        
        startForeground(NOTIFICATION_ID, createNotification("Monitoring system..."));
        
        startMonitoring();
        
        return START_STICKY;
    }
    
    private void startMonitoring() {
        if (timer != null) {
            timer.cancel();
        }
        
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                collectAndSendData();
            }
        }, 0, 5 * 60 * 1000); // Every 5 minutes
    }
    
    private void collectAndSendData() {
        try {
            // Collect system data
            JSONObject systemData = SystemDataCollector.collectData(this);
            
            // Send data to server
            URL url = new URL(serverUrl + "/api/system-data");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
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
                updateNotification("Monitoring active - Last update: " + 
                        new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));
            } else {
                Log.e(TAG, "Failed to send data: " + responseCode);
                updateNotification("Error sending data - Code: " + responseCode);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error collecting or sending data", e);
            updateNotification("Error: " + e.getMessage());
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
        super.onDestroy();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
