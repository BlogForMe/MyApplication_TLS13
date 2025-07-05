package com.example.myapplication.datasync;


import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class DataSyncJobIntentService extends JobIntentService {
    private static final String TAG = "DataSyncJobIntent";
    private static final int JOB_ID = 2000;

    public static void enqueueWork(Context context, Intent work) {
        try {
            Log.d(TAG, "Enqueueing work to JobIntentService");
            enqueueWork(context, DataSyncJobIntentService.class, JOB_ID, work);
            Log.d(TAG, "Work enqueued successfully");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to enqueue work - check manifest", e);
            throw e;
        }
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        String action = intent.getStringExtra("action");
        boolean useForeground = intent.getBooleanExtra("use_foreground", false);

        sendStatusUpdate("JobIntentService started: " + action, "INFO");
        Log.i(TAG, "Handling work: " + action + ", useForeground: " + useForeground);

        try {
            switch (action) {
                case "background_sync":
                    performBackgroundSync(intent);
                    break;
                case "start_foreground_sync":
                    startForegroundDataSync(intent);
                    break;
                default:
                    Log.w(TAG, "Unknown action: " + action);
                    sendStatusUpdate("Unknown action: " + action, "WARN");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in JobIntentService work", e);
            sendStatusUpdate("JobIntentService error: " + e.getMessage(), "ERROR");
        }

        sendStatusUpdate("JobIntentService completed: " + action, "INFO");
        Log.i(TAG, "Work completed: " + action);
    }

    private void performBackgroundSync(Intent intent) {
        int durationMinutes = intent.getIntExtra("duration_minutes", 1);
        sendStatusUpdate("Background sync starting (" + durationMinutes + " min)", "INFO");

        // This runs in background - NOT subject to dataSync timeout
        long durationMs = durationMinutes * 60 * 1000L;
        long startTime = System.currentTimeMillis();

        int steps = 10;
        long stepDuration = durationMs / steps;

        for (int step = 1; step <= steps; step++) {
            if (Thread.currentThread().isInterrupted()) {
                sendStatusUpdate("Background sync interrupted at step " + step, "WARN");
                return;
            }

            SystemClock.sleep(stepDuration);
            sendStatusUpdate("Background sync: " + step + "/" + steps + " (" +
                    (step * 100 / steps) + "%)", "DEBUG");
            Log.d(TAG, "Background sync progress: " + step + "/" + steps);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        sendStatusUpdate("Background sync completed in " + (elapsed / 1000) + "s", "INFO");
    }

    private void startForegroundDataSync(Intent intent) {
        int durationHours = intent.getIntExtra("duration_hours", 1);
        String syncId = intent.getStringExtra("sync_id");

        sendStatusUpdate("JobIntentService starting foreground dataSync service", "INFO");
        sendStatusUpdate("Duration: " + durationHours + "h, ID: " + syncId, "INFO");

        try {
            // Create intent to start the foreground service
            Intent foregroundIntent = new Intent(this, DataSyncForegroundService.class);
            foregroundIntent.putExtra("duration_hours", durationHours);
            foregroundIntent.putExtra("sync_id", syncId != null ? syncId : "default");
            foregroundIntent.putExtra("started_by", "JobIntentService");

            // CRITICAL: Specify service type for Android 14+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                foregroundIntent.putExtra("foregroundServiceType",
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            }

            // Start the foreground service (subject to Android 15 timeout)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, foregroundIntent);
            } else {
                startService(foregroundIntent);
            }

            sendStatusUpdate("Foreground dataSync service started by JobIntentService", "INFO");
            sendStatusUpdate("This service IS subject to 6-hour timeout!", "WARN");

        } catch (Exception e) {
            Log.e(TAG, "Failed to start foreground service from JobIntentService", e);
            sendStatusUpdate("Failed to start foreground service: " + e.getMessage(), "ERROR");
        }
    }

    private void sendStatusUpdate(String message, String level) {
        Intent statusIntent = new Intent("com.example.jobintentdemo.STATUS_UPDATE");
        statusIntent.putExtra("message", message);
        statusIntent.putExtra("level", level);
        LocalBroadcastManager.getInstance(this).sendBroadcast(statusIntent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "JobIntentService created");
        sendStatusUpdate("JobIntentService created", "DEBUG");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "JobIntentService destroyed");
        sendStatusUpdate("JobIntentService destroyed", "DEBUG");
    }
}