package com.mobilesystemmonitor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final long DEFAULT_UPDATE_INTERVAL = 60000; // 1 minute

    private EditText serverUrlEditText;
    private EditText authTokenEditText;
    private Switch monitoringSwitch;
    private Switch backgroundMonitoringSwitch;
    private Button testConnectionButton;
    private Button updateNowButton;
    private TextView statusTextView;
    private TextView lastUpdateTextView;
    private TextView updateIntervalTextView;
    private SeekBar updateIntervalSeekBar;
    private ProgressBar progressBar;

    private SharedPreferences sharedPreferences;
    private Timer monitoringTimer;
    private Handler handler = new Handler(Looper.getMainLooper());
    private long updateInterval = DEFAULT_UPDATE_INTERVAL;

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
        updateIntervalTextView = findViewById(R.id.updateIntervalTextView);
        updateIntervalSeekBar = findViewById(R.id.updateIntervalSeekBar);
        progressBar = findViewById(R.id.progressBar);

        // Hide progress bar initially
        progressBar.setVisibility(View.INVISIBLE);

        // Load saved settings
        sharedPreferences = getSharedPreferences("SystemMonitorPrefs", Context.MODE_PRIVATE);
        serverUrlEditText.setText(sharedPreferences.getString("serverUrl", "http://your-server-ip:5000"));
        authTokenEditText.setText(sharedPreferences.getString("authToken", "default_token_change_me"));
        monitoringSwitch.setChecked(sharedPreferences.getBoolean("monitoring", false));
        backgroundMonitoringSwitch.setChecked(sharedPreferences.getBoolean("backgroundMonitoring", false));

        // Load update interval
        updateInterval = sharedPreferences.getLong("updateInterval", DEFAULT_UPDATE_INTERVAL);

        // Configure update interval seek bar
        updateIntervalSeekBar.setMax(9); // 0-9 representing different intervals
        int progress = convertIntervalToProgress(updateInterval);
        updateIntervalSeekBar.setProgress(progress);
        updateIntervalTextView.setText(formatUpdateInterval(updateInterval));

        updateIntervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateInterval = convertProgressToInterval(progress);
                updateIntervalTextView.setText(formatUpdateInterval(updateInterval));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not needed
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
                if (monitoringTimer != null) {
                    // Restart monitoring with new interval
                    stopMonitoring();
                    startMonitoring();
                }
            }
        });

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
        editor.putLong("updateInterval", updateInterval);
        editor.apply();
    }

    private String formatUpdateInterval(long interval) {
        if (interval < 60000) {
            return (interval / 1000) + " seconds";
        } else if (interval < 3600000) {
            return (interval / 60000) + " minutes";
        } else {
            return (interval / 3600000) + " hours";
        }
    }

    private int convertIntervalToProgress(long interval) {
        if (interval <= 15000) return 0;        // 15 seconds
        else if (interval <= 30000) return 1;   // 30 seconds
        else if (interval <= 60000) return 2;   // 1 minute
        else if (interval <= 300000) return 3;  // 5 minutes
        else if (interval <= 600000) return 4;  // 10 minutes
        else if (interval <= 900000) return 5;  // 15 minutes
        else if (interval <= 1800000) return 6; // 30 minutes
        else if (interval <= 3600000) return 7; // 1 hour
        else if (interval <= 7200000) return 8; // 2 hours
        else return 9;                          // 6 hours
    }

    private long convertProgressToInterval(int progress) {
        switch (progress) {
            case 0: return 15000;    // 15 seconds
            case 1: return 30000;    // 30 seconds
            case 2: return 60000;    // 1 minute
            case 3: return 300000;   // 5 minutes
            case 4: return 600000;   // 10 minutes
            case 5: return 900000;   // 15 minutes
            case 6: return 1800000;  // 30 minutes
            case 7: return 3600000;  // 1 hour
            case 8: return 7200000;  // 2 hours
            case 9: return 21600000; // 6 hours
            default: return 60000;   // Default: 1 minute
        }
    }

    private void testConnection() {
        saveSettings();

        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        statusTextView.setText("Testing connection...");
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                String serverUrl = serverUrlEditText.getText().toString();
                String authToken = authTokenEditText.getText().toString();

                // Check network connectivity first
                if (!isNetworkAvailable()) {
                    handler.post(() -> {
                        statusTextView.setText("No network connection");
                        progressBar.setVisibility(View.INVISIBLE);
                        Toast.makeText(MainActivity.this, "No network connection", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                URL url = new URI(serverUrl + "/api/ping").toURL();
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
                    progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                final String errorMessage = "Connection error: " + e.getMessage();
                handler.post(() -> {
                    statusTextView.setText(errorMessage);
                    progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private boolean validateInputs() {
        String serverUrl = serverUrlEditText.getText().toString().trim();
        String authToken = authTokenEditText.getText().toString().trim();

        if (TextUtils.isEmpty(serverUrl)) {
            Toast.makeText(this, "Server URL cannot be empty", Toast.LENGTH_SHORT).show();
            serverUrlEditText.requestFocus();
            return false;
        }

        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            Toast.makeText(this, "Server URL must start with http:// or https://", Toast.LENGTH_SHORT).show();
            serverUrlEditText.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(authToken)) {
            Toast.makeText(this, "Authentication token cannot be empty", Toast.LENGTH_SHORT).show();
            authTokenEditText.requestFocus();
            return false;
        }

        return true;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
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
        }, 0, updateInterval);

        statusTextView.setText("Monitoring started");
        Toast.makeText(this, "Monitoring started with interval: " + formatUpdateInterval(updateInterval), Toast.LENGTH_SHORT).show();
    }

    private void stopMonitoring() {
        if (monitoringTimer != null) {
            monitoringTimer.cancel();
            monitoringTimer = null;
        }
    }

    private void startBackgroundService() {
        if (!validateInputs()) {
            backgroundMonitoringSwitch.setChecked(false);
            return;
        }

        Intent serviceIntent = new Intent(this, MonitorService.class);
        serviceIntent.putExtra("serverUrl", serverUrlEditText.getText().toString());
        serviceIntent.putExtra("authToken", authTokenEditText.getText().toString());
        serviceIntent.putExtra("updateInterval", updateInterval);
        startService(serviceIntent);

        Toast.makeText(this, "Background monitoring started", Toast.LENGTH_SHORT).show();
    }

    private void stopBackgroundService() {
        Intent serviceIntent = new Intent(this, MonitorService.class);
        stopService(serviceIntent);
    }

    private void collectAndSendData() {
        // Show progress on UI thread
        handler.post(() -> {
            progressBar.setVisibility(View.VISIBLE);
            statusTextView.setText("Collecting data...");
        });

        new Thread(() -> {
            try {
                // Check network connectivity first
                if (!isNetworkAvailable()) {
                    handler.post(() -> {
                        statusTextView.setText("No network connection");
                        progressBar.setVisibility(View.INVISIBLE);
                    });
                    return;
                }

                // Collect system data
                JSONObject systemData = SystemDataCollector.collectData(this);

                handler.post(() -> {
                    statusTextView.setText("Sending data to server...");
                });

                // Send data to server
                String serverUrl = serverUrlEditText.getText().toString();
                String authToken = authTokenEditText.getText().toString();

                URL url = new URI(serverUrl + "/api/system-data").toURL();
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
                    progressBar.setVisibility(View.INVISIBLE);
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
                    progressBar.setVisibility(View.INVISIBLE);
                    statusTextView.setText(errorMessage);
                });
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "About");
        menu.add(0, 2, 0, "Help");
        menu.add(0, 3, 0, "Clear Data");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1: // About
                showAboutDialog();
                return true;
            case 2: // Help
                showHelpDialog();
                return true;
            case 3: // Clear Data
                showClearDataConfirmation();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showAboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("About Mobile System Monitor");
        builder.setMessage("Version 1.0.0\n\nThis application collects system information from your device and sends it to a monitoring server.\n\n" +
                "It can run in the background and store data offline when no connection is available.");
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void showHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Help");
        builder.setMessage(
                "Server URL: The address of your monitoring server\n\n" +
                "Auth Token: The authentication token for your server\n\n" +
                "Update Interval: How often to collect and send data\n\n" +
                "Enable Monitoring: Collect data while the app is open\n\n" +
                "Enable Background Monitoring: Continue collecting data when the app is closed\n\n" +
                "Test Connection: Check if the server is reachable\n\n" +
                "Update Now: Collect and send data immediately");
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void showClearDataConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Clear Data");
        builder.setMessage("This will reset all settings and stop monitoring. Continue?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                clearAllData();
            }
        });
        builder.setNegativeButton("No", null);
        builder.show();
    }

    private void clearAllData() {
        // Stop monitoring and services
        stopMonitoring();
        stopBackgroundService();

        // Clear preferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        // Reset UI
        serverUrlEditText.setText("http://your-server-ip:5000");
        authTokenEditText.setText("default_token_change_me");
        monitoringSwitch.setChecked(false);
        backgroundMonitoringSwitch.setChecked(false);
        updateInterval = DEFAULT_UPDATE_INTERVAL;
        updateIntervalSeekBar.setProgress(convertIntervalToProgress(updateInterval));
        updateIntervalTextView.setText(formatUpdateInterval(updateInterval));
        statusTextView.setText("Status: Not connected");
        lastUpdateTextView.setText("Last update: Never");

        Toast.makeText(this, "All data cleared", Toast.LENGTH_SHORT).show();
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
