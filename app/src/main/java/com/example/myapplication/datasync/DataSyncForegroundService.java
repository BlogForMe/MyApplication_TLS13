package com.example.myapplication.datasync;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class DataSyncForegroundService extends Service {
    private static final String TAG = "DataSyncForeground";
    private static final String CHANNEL_ID = "DataSyncChannel";
    private static final int NOTIFICATION_ID = 1001;

    private Handler mainHandler;
    private volatile boolean isRunning = false;
    private volatile boolean timeoutReceived = false;
    private Thread syncThread;
    private String syncId = "unknown";

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "DataSync Foreground Service created");
        sendStatusUpdate("Foreground Service created", "DEBUG");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "DataSync Foreground Service started");

        if (intent != null) {
            syncId = intent.getStringExtra("sync_id");
            String startedBy = intent.getStringExtra("started_by");
            int durationHours = intent.getIntExtra("duration_hours", 1);

            sendStatusUpdate("Foreground service started by: " + startedBy, "INFO");
            sendStatusUpdate("Sync ID: " + syncId + ", Duration: " + durationHours + "h", "INFO");
        }

        // Start as foreground service immediately
        startAsForegroundService(intent);

        if (intent != null) {
            int durationHours = intent.getIntExtra("duration_hours", 1);
            startDataSync(durationHours);
        }

        return START_NOT_STICKY;
    }

    private void startAsForegroundService(Intent intent) {
        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("DataSync Service (" + syncId + ")")
                .setContentText("Syncing data - subject to 6h timeout")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .build();

        // Use proper API for Android 14+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            int serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
            startForeground(NOTIFICATION_ID, notification, serviceType);
            sendStatusUpdate("Started as dataSync foreground service (API 34+)", "INFO");
        } else {
            startForeground(NOTIFICATION_ID, notification);
            sendStatusUpdate("Started as foreground service (API < 34)", "INFO");
        }
    }

    private void startDataSync(int durationHours) {
        isRunning = true;
        timeoutReceived = false;

        syncThread = new Thread(() -> {
            try {
                performDataSync(durationHours);
            } catch (InterruptedException e) {
                sendStatusUpdate("Sync thread interrupted", "WARN");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                Log.e(TAG, "Error in sync thread", e);
                sendStatusUpdate("Sync error: " + e.getMessage(), "ERROR");
            } finally {
                if (!timeoutReceived) {
                    sendStatusUpdate("Sync completed normally - stopping service", "INFO");
                    stopSelfSafely();
                }
            }
        });

        syncThread.start();
        sendStatusUpdate("Data sync thread started for " + durationHours + " hours", "INFO");
    }

    private void performDataSync(int durationHours) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long durationMs = durationHours * 60 * 60 * 1000L;
        long endTime = startTime + durationMs;

        sendStatusUpdate("Starting sync: " + durationHours + "h duration", "INFO");

        int updateIntervalMs = 60000; // Update every minute
        int stepNumber = 1;

        while (System.currentTimeMillis() < endTime && isRunning && !Thread.currentThread().isInterrupted()) {
            SystemClock.sleep(updateIntervalMs);

            long elapsed = System.currentTimeMillis() - startTime;
            long remaining = endTime - System.currentTimeMillis();

            String progress = "Step " + stepNumber + " - Elapsed: " + (elapsed / 1000 / 60) +
                    "min, Remaining: " + (remaining / 1000 / 60) + "min";

            Log.d(TAG, progress);
            sendStatusUpdate(progress, "DEBUG");
            updateNotification(progress);

            stepNumber++;
        }

        if (isRunning) {
            sendStatusUpdate("Data sync completed after " + durationHours + " hours", "INFO");
        }
    }

    // CRITICAL: Android 15 timeout callback
    @Override
    public void onTimeout(int type, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            Log.w(TAG, "🚨 ANDROID 15 TIMEOUT RECEIVED! Type: " + type + ", StartId: " + startId);
            sendStatusUpdate("🚨 TIMEOUT RECEIVED! Service exceeded 6-hour limit", "ERROR");
            sendStatusUpdate("Type: " + type + ", StartId: " + startId, "ERROR");

            timeoutReceived = true;
            isRunning = false;

            // Stop the sync work
            if (syncThread != null) {
                syncThread.interrupt();
            }

            // Update notification
            updateNotification("⚠️ Service timed out - stopping...");

            // Clean up and stop
            mainHandler.postDelayed(() -> {
                sendStatusUpdate("Stopping service after timeout cleanup", "INFO");
                stopSelf();
            }, 2000);
        }
    }

    private void updateNotification(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        String title = timeoutReceived ? "⚠️ DataSync Timed Out" : "DataSync Service (" + syncId + ")";

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(!timeoutReceived)
                .build();

        manager.notify(NOTIFICATION_ID, notification);
    }

    private void stopSelfSafely() {
        mainHandler.post(() -> {
            sendStatusUpdate("Stopping foreground service safely", "INFO");
            isRunning = false;
            stopForeground(true);
            stopSelf();
        });
    }

    private void sendStatusUpdate(String message, String level) {
        Intent statusIntent = new Intent("com.example.jobintentdemo.STATUS_UPDATE");
        statusIntent.putExtra("message", message);
        statusIntent.putExtra("level", level);
        LocalBroadcastManager.getInstance(this).sendBroadcast(statusIntent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Data Sync Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;

        if (syncThread != null && syncThread.isAlive()) {
            syncThread.interrupt();
            try {
                syncThread.join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        sendStatusUpdate("Foreground service destroyed", "DEBUG");
        Log.d(TAG, "DataSync Foreground Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}