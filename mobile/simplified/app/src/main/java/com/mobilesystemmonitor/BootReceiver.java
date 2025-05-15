package com.mobilesystemmonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences sharedPreferences = 
                    context.getSharedPreferences("SystemMonitorPrefs", Context.MODE_PRIVATE);
            
            boolean backgroundMonitoring = sharedPreferences.getBoolean("backgroundMonitoring", false);
            
            if (backgroundMonitoring) {
                String serverUrl = sharedPreferences.getString("serverUrl", "");
                String authToken = sharedPreferences.getString("authToken", "");
                
                Intent serviceIntent = new Intent(context, MonitorService.class);
                serviceIntent.putExtra("serverUrl", serverUrl);
                serviceIntent.putExtra("authToken", authToken);
                
                context.startService(serviceIntent);
            }
        }
    }
}
