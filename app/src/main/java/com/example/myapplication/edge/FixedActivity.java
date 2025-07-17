package com.example.myapplication.edge;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.myapplication.R;

public class FixedActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ FIXED: Enable edge-to-edge before setContentView
        enableEdgeToEdge();

        setContentView(R.layout.activity_edge_to_edge);

        // ✅ FIXED: Handle window insets properly
        setupWindowInsets();

        setupViews();
    }

    private void enableEdgeToEdge() {
        // Tell the system that we want to handle window insets ourselves
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Make system bars transparent
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        // Handle display cutouts for devices with notches
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        // Control system bar appearance
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
    }

    private void setupWindowInsets() {
        View rootView = findViewById(R.id.root_container);

        ViewCompat.setOnApplyWindowInsetsListener(rootView, new androidx.core.view.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View view, WindowInsetsCompat insets) {
                androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                androidx.core.graphics.Insets displayCutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout());

                // Apply padding to prevent content from being hidden
                view.setPadding(
                        Math.max(systemBars.left, displayCutout.left),
                        Math.max(systemBars.top, displayCutout.top),
                        Math.max(systemBars.right, displayCutout.right),
                        Math.max(systemBars.bottom, displayCutout.bottom)
                );

                return insets;
            }
        });
    }

    private void setupViews() {
        TextView titleText = findViewById(R.id.title_text);
        Button button1 = findViewById(R.id.button_1);
        Button button2 = findViewById(R.id.button_2);
        Button bottomButton = findViewById(R.id.bottom_button);

        titleText.setText("Fixed Activity - Edge-to-Edge Ready!");

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(FixedActivity.this, "Button 1 clicked", Toast.LENGTH_SHORT).show();
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(FixedActivity.this, "Button 2 clicked", Toast.LENGTH_SHORT).show();
            }
        });

        bottomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(FixedActivity.this, "Bottom button clicked", Toast.LENGTH_SHORT).show();
            }
        });
    }
}