package com.example.myapplication.datasync;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.myapplication.R;

public class JobServiceActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String CHANNEL_ID = "DataSyncChannel";

    private TextView statusText;
    private ScrollView scrollView;
    private StringBuilder statusLog = new StringBuilder();

    private BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            String level = intent.getStringExtra("level");
            updateStatus("[" + level + "] " + message);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_service);

        createNotificationChannel();
        initViews();
        setupButtons();
        registerStatusReceiver();

        updateStatus("=== Android 15 DataSync Timeout Demo ===");
        updateStatus("Target SDK: " + getApplicationInfo().targetSdkVersion);
        updateStatus("Device API: " + Build.VERSION.SDK_INT);
        updateStatus("Ready for testing...\n");
    }

    private void initViews() {
        statusText = findViewById(R.id.statusText);
        scrollView = findViewById(R.id.scrollView);
    }

    private void setupButtons() {
        // Test 1: JobIntentService background work (not subject to timeout)
        Button backgroundJobBtn = findViewById(R.id.backgroundJobBtn);
        backgroundJobBtn.setOnClickListener(v -> startBackgroundJob());

        // Test 2: JobIntentService starts foreground service (subject to timeout)
        Button foregroundJobBtn = findViewById(R.id.foregroundJobBtn);
        foregroundJobBtn.setOnClickListener(v -> startForegroundJob());

        // Test 3: Multiple JobIntentServices triggering foreground services
        Button multipleJobsBtn = findViewById(R.id.multipleJobsBtn);
        multipleJobsBtn.setOnClickListener(v -> startMultipleJobs());

        // Test 4: Long-running test (for timeout testing)
        Button longRunBtn = findViewById(R.id.longRunBtn);
        longRunBtn.setOnClickListener(v -> startLongRunningTest());

        // Test 5: Stop all services
        Button stopAllBtn = findViewById(R.id.stopAllBtn);
        stopAllBtn.setOnClickListener(v -> stopAllServices());

        // Show testing commands
        Button commandsBtn = findViewById(R.id.commandsBtn);
        commandsBtn.setOnClickListener(v -> showTestingCommands());

        // Clear log
        Button clearBtn = findViewById(R.id.clearBtn);
        clearBtn.setOnClickListener(v -> clearLog());
    }

    private void startBackgroundJob() {
        updateStatus("\n=== TEST 1: Background JobIntentService ===");
        updateStatus("Starting background sync (NOT subject to 6h timeout)");

        Intent intent = new Intent();
        intent.putExtra("action", "background_sync");
        intent.putExtra("duration_minutes", 3);
        intent.putExtra("use_foreground", false);

        DataSyncJobIntentService.enqueueWork(this, intent);
        updateStatus("Background job enqueued - check logs for progress");
    }

    private void startForegroundJob() {
        updateStatus("\n=== TEST 2: JobIntentService → Foreground Service ===");
        updateStatus("JobIntentService will start dataSync foreground service");
        updateStatus("This WILL be subject to Android 15 timeout limits!");

        Intent intent = new Intent();
        intent.putExtra("action", "start_foreground_sync");
        intent.putExtra("duration_hours", 2);
        intent.putExtra("use_foreground", true);

        DataSyncJobIntentService.enqueueWork(this, intent);
        updateStatus("Job enqueued to start foreground dataSync service");
    }

    private void startMultipleJobs() {
        updateStatus("\n=== TEST 3: Multiple Jobs → Multiple Foreground Services ===");
        updateStatus("Testing cumulative 6-hour limit across services");

        // Start 3 different jobs that each start foreground services
        for (int i = 1; i <= 3; i++) {
            Intent intent = new Intent();
            intent.putExtra("action", "start_foreground_sync");
            intent.putExtra("duration_hours", 2);
            intent.putExtra("sync_id", "sync_" + i);
            intent.putExtra("use_foreground", true);

            DataSyncJobIntentService.enqueueWork(this, intent);
            updateStatus("Job " + i + " enqueued (2 hours each = 6 total)");
        }
        updateStatus("Total: 6 hours - should hit timeout limit!");
    }

    private void startLongRunningTest() {
        updateStatus("\n=== TEST 4: Long-Running Timeout Test ===");
        updateStatus("Starting 7-hour job to trigger timeout...");

        Intent intent = new Intent();
        intent.putExtra("action", "start_foreground_sync");
        intent.putExtra("duration_hours", 7); // Exceeds 6-hour limit
        intent.putExtra("sync_id", "long_test");
        intent.putExtra("use_foreground", true);

        DataSyncJobIntentService.enqueueWork(this, intent);
        updateStatus("7-hour job started - should timeout after 6 hours");
        updateStatus("Monitor logs for onTimeout() callback");
    }

    private void stopAllServices() {
        updateStatus("\n=== Stopping All Services ===");

        // Stop foreground service
        Intent stopIntent = new Intent(this, DataSyncForegroundService.class);
        stopService(stopIntent);

        updateStatus("Stop commands sent to all services");
    }

    private void showTestingCommands() {
        updateStatus("\n=== ANDROID 15 TIMEOUT TESTING COMMANDS ===");
        updateStatus("Execute these ADB commands for testing:\n");
        updateStatus("1. Enable timeout testing:");
        updateStatus("adb shell am compat enable FGS_INTRODUCE_TIME_LIMITS com.example.jobintentdemo\n");
        updateStatus("2. Set 5-minute timeout (for quick testing):");
        updateStatus("adb shell device_config put activity_manager data_sync_fgs_timeout_duration 300000\n");
        updateStatus("3. Monitor timeout logs:");
        updateStatus("adb logcat | grep -E \"(DataSync|onTimeout|RemoteServiceException)\"\n");
        updateStatus("4. Reset timeout to default:");
        updateStatus("adb shell device_config delete activity_manager data_sync_fgs_timeout_duration\n");
        updateStatus("5. Disable testing:");
        updateStatus("adb shell am compat disable FGS_INTRODUCE_TIME_LIMITS com.example.jobintentdemo\n");
    }

    private void clearLog() {
        statusLog.setLength(0);
        statusText.setText("");
    }

    private void updateStatus(String message) {
        runOnUiThread(() -> {
            statusLog.append(message).append("\n");
            statusText.setText(statusLog.toString());
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
        Log.i(TAG, message);
    }

    private void registerStatusReceiver() {
        IntentFilter filter = new IntentFilter("com.example.jobintentdemo.STATUS_UPDATE");
        LocalBroadcastManager.getInstance(this).registerReceiver(statusReceiver, filter);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Data Sync Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Channel for data sync notifications");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusReceiver);
    }
}