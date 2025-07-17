package com.example.myapplication.datasync;


import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.myapplication.R;

public class JobServiceActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String CHANNEL_ID = "DataSyncChannel";

    private TextView statusText;
    private ScrollView scrollView;
    private StringBuilder statusLog = new StringBuilder();

    private boolean notificationPermissionGranted;

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

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
        initViews();

        checkInitialPermissions();
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

    private void checkInitialPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires runtime notification permission
            checkNotificationPermission();
        } else {
            // Pre-Android 13: No notification permission required
            boolean notificationPermissionGranted = true;
            updateStatus("✅ Android < 13: Notification permission not required");
            enableServiceButtons();
        }
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



    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean hasPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

            notificationPermissionGranted = hasPermission;

            if (hasPermission) {
                updateStatus("✅ Notification permission granted");
                enableServiceButtons();
            } else {
                updateStatus("❌ Notification permission not granted");
                updateStatus("⚠️ Foreground services require notification permission!");
                disableServiceButtons();

                // Auto-request permission on first launch
                requestNotificationPermission();
            }
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                // Check if we should show permission rationale
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.POST_NOTIFICATIONS)) {

                    // Show explanation dialog
                    showPermissionRationaleDialog();
                } else {
                    // Request permission directly
                    updateStatus("Requesting notification permission...");
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            NOTIFICATION_PERMISSION_REQUEST_CODE);
                }
            } else {
                // Permission already granted
                notificationPermissionGranted = true;
                updateStatus("✅ Notification permission already granted");
                enableServiceButtons();
            }
        }
    }

    private void showPermissionRationaleDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Notification Permission Required")
                .setMessage("This app needs notification permission to run foreground services for data synchronization.\n\n" +
                        "Without this permission:\n" +
                        "• Foreground services cannot start\n" +
                        "• Data sync will fail\n" +
                        "• App functionality will be limited")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            NOTIFICATION_PERMISSION_REQUEST_CODE);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    updateStatus("❌ Permission denied by user");
                    disableServiceButtons();
                    showPermissionDeniedOptions();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                notificationPermissionGranted = true;
                updateStatus("✅ Notification permission granted successfully!");
                enableServiceButtons();

                Toast.makeText(this, "Permission granted! Foreground services can now start",
                        Toast.LENGTH_LONG).show();

            } else {
                // Permission denied
                notificationPermissionGranted = false;
                updateStatus("❌ Notification permission denied");
                disableServiceButtons();

                // Check if user selected "Don't ask again"
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.POST_NOTIFICATIONS)) {
                    updateStatus("⚠️ Permission permanently denied - manual action required");
                    showPermissionPermanentlyDeniedDialog();
                } else {
                    showPermissionDeniedOptions();
                }
            }
        }
    }

    private void showPermissionDeniedOptions() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("Notification permission is required for foreground services.\n\n" +
                        "What would you like to do?")
                .setPositiveButton("Retry", (dialog, which) -> requestNotificationPermission())
                .setNegativeButton("Go to Settings", (dialog, which) -> openAppSettings())
                .setNeutralButton("Continue Without", (dialog, which) -> {
                    updateStatus("⚠️ Continuing without permission - limited functionality");
                })
                .show();
    }

    private void showPermissionPermanentlyDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("Notification permission has been permanently denied.\n\n" +
                        "To enable foreground services:\n" +
                        "1. Go to App Settings\n" +
                        "2. Tap Permissions\n" +
                        "3. Enable Notifications")
                .setPositiveButton("Open Settings", (dialog, which) -> openAppSettings())
                .setNegativeButton("Cancel", (dialog, which) -> {
                    updateStatus("❌ Manual permission grant required in Settings");
                })
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);

        updateStatus("Opening app settings - please enable notifications");
    }

    // ===================================================================
    // PERMISSION CHECKING AND VALIDATION
    // ===================================================================

    private void checkAllPermissions() {
        updateStatus("\n=== PERMISSION STATUS CHECK ===");

        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean hasNotificationPerm = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            updateStatus("POST_NOTIFICATIONS: " + (hasNotificationPerm ? "✅ Granted" : "❌ Denied"));
            notificationPermissionGranted = hasNotificationPerm;

            if (!hasNotificationPerm) {
                updateStatus("⚠️ Foreground services require notification permission!");
            }
        } else {
            updateStatus("POST_NOTIFICATIONS: ✅ Not required (Android < 13)");
            notificationPermissionGranted = true;
        }

        // Check other foreground service permissions
        String[] otherPermissions = {
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC,
                Manifest.permission.INTERNET,
                Manifest.permission.WAKE_LOCK
        };

        for (String permission : otherPermissions) {
            boolean hasPermission = ContextCompat.checkSelfPermission(this, permission)
                    == PackageManager.PERMISSION_GRANTED;
            String permName = permission.substring(permission.lastIndexOf('.') + 1);
            updateStatus(permName + ": " + (hasPermission ? "✅ Granted" : "❌ Denied"));
        }

        // Update button states based on permissions
        if (notificationPermissionGranted) {
            enableServiceButtons();
        } else {
            disableServiceButtons();
        }

        updateStatus("Permission check complete\n");
    }

    private boolean validatePermissionsBeforeService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationPermissionGranted) {
            updateStatus("❌ Cannot start foreground service: Notification permission required");

            new AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("Notification permission is required to start foreground services.")
                    .setPositiveButton("Grant Permission", (dialog, which) -> requestNotificationPermission())
                    .setNegativeButton("Cancel", null)
                    .show();

            return false;
        }
        return true;
    }

    // ===================================================================
    // UI STATE MANAGEMENT
    // ===================================================================

    private void enableServiceButtons() {
//        startServiceBtn.setEnabled(true);
//        startServiceBtn.setText("Start Foreground Service");
//        startServiceBtn.setAlpha(1.0f);
    }

    private void disableServiceButtons() {
//        startServiceBtn.setEnabled(false);
//        startServiceBtn.setText("Service Disabled (No Permission)");
//        startServiceBtn.setAlpha(0.5f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusReceiver);
    }
}