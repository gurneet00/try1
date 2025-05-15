package com.mobilesystemmonitor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    private EditText serverUrlEditText;
    private EditText authTokenEditText;
    private Switch monitoringSwitch;
    private Switch backgroundMonitoringSwitch;
    private Button testConnectionButton;
    private Button updateNowButton;
    private TextView statusTextView;
    private TextView lastUpdateTextView;

    private SharedPreferences sharedPreferences;
    private Timer monitoringTimer;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        serverUrlEditText = findViewById(R.id.serverUrlEditText);
        authTokenEditText = findViewById(R.id.authTokenEditText);
        monitoringSwitch = findViewById(R.id.monitoringSwitch);
        backgroundMonitoringSwitch = findViewById(R.id.backgroundMonitoringSwitch);
        testConnectionButton = findViewById(R.id.testConnectionButton);
        updateNowButton = findViewById(R.id.updateNowButton);
        statusTextView = findViewById(R.id.statusTextView);
        lastUpdateTextView = findViewById(R.id.lastUpdateTextView);

        // Load saved settings
        sharedPreferences = getSharedPreferences("SystemMonitorPrefs", Context.MODE_PRIVATE);
        serverUrlEditText.setText(sharedPreferences.getString("serverUrl", "http://your-server-ip:5000"));
        authTokenEditText.setText(sharedPreferences.getString("authToken", "default_token_change_me"));
        monitoringSwitch.setChecked(sharedPreferences.getBoolean("monitoring", false));
        backgroundMonitoringSwitch.setChecked(sharedPreferences.getBoolean("backgroundMonitoring", false));

        // Set up button click listeners
        testConnectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testConnection();
            }
        });

        updateNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collectAndSendData();
            }
        });

        // Set up switch listeners
        monitoringSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSettings();
            if (isChecked) {
                startMonitoring();
            } else {
                stopMonitoring();
            }
        });

        backgroundMonitoringSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSettings();
            if (isChecked) {
                startBackgroundService();
            } else {
                stopBackgroundService();
            }
        });

        // Start monitoring if it was enabled
        if (monitoringSwitch.isChecked()) {
            startMonitoring();
        }

        // Start background service if it was enabled
        if (backgroundMonitoringSwitch.isChecked()) {
            startBackgroundService();
        }
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("serverUrl", serverUrlEditText.getText().toString());
        editor.putString("authToken", authTokenEditText.getText().toString());
        editor.putBoolean("monitoring", monitoringSwitch.isChecked());
        editor.putBoolean("backgroundMonitoring", backgroundMonitoringSwitch.isChecked());
        editor.apply();
    }

    private void testConnection() {
        saveSettings();
        statusTextView.setText("Testing connection...");
        
        new Thread(() -> {
            try {
                String serverUrl = serverUrlEditText.getText().toString();
                String authToken = authTokenEditText.getText().toString();
                
                URL url = new URL(serverUrl + "/api/ping");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "Bearer " + authToken);
                
                int responseCode = connection.getResponseCode();
                
                final String message;
                if (responseCode == 200) {
                    message = "Connection successful!";
                } else {
                    message = "Connection failed: " + responseCode;
                }
                
                handler.post(() -> {
                    statusTextView.setText(message);
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                });
                
            } catch (Exception e) {
                final String errorMessage = "Connection error: " + e.getMessage();
                handler.post(() -> {
                    statusTextView.setText(errorMessage);
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void startMonitoring() {
        if (monitoringTimer != null) {
            monitoringTimer.cancel();
        }
        
        monitoringTimer = new Timer();
        monitoringTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                collectAndSendData();
            }
        }, 0, 60000); // Collect data every 60 seconds
    }

    private void stopMonitoring() {
        if (monitoringTimer != null) {
            monitoringTimer.cancel();
            monitoringTimer = null;
        }
    }

    private void startBackgroundService() {
        Intent serviceIntent = new Intent(this, MonitorService.class);
        serviceIntent.putExtra("serverUrl", serverUrlEditText.getText().toString());
        serviceIntent.putExtra("authToken", authTokenEditText.getText().toString());
        startService(serviceIntent);
    }

    private void stopBackgroundService() {
        Intent serviceIntent = new Intent(this, MonitorService.class);
        stopService(serviceIntent);
    }

    private void collectAndSendData() {
        new Thread(() -> {
            try {
                // Collect system data
                JSONObject systemData = SystemDataCollector.collectData(this);
                
                // Send data to server
                String serverUrl = serverUrlEditText.getText().toString();
                String authToken = authTokenEditText.getText().toString();
                
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
                
                final String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new java.util.Date());
                
                handler.post(() -> {
                    if (responseCode == 200) {
                        statusTextView.setText("Data sent successfully");
                        lastUpdateTextView.setText("Last update: " + timestamp);
                    } else {
                        statusTextView.setText("Failed to send data: " + responseCode);
                    }
                });
                
            } catch (Exception e) {
                final String errorMessage = "Error: " + e.getMessage();
                handler.post(() -> {
                    statusTextView.setText(errorMessage);
                });
            }
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSettings();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMonitoring();
    }
}
