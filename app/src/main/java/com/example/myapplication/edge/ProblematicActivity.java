package com.example.myapplication.edge;
// ============================================================================
// PROBLEMATIC VERSION - Issues with Android 15 Edge-to-Edge Enforcement
// ============================================================================

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;

public class ProblematicActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ❌ PROBLEM: Direct setContentView without edge-to-edge handling
        setContentView(R.layout.activity_edge_to_edge);

        // ❌ PROBLEM: No window insets handling
        // Content will be hidden behind status bar and navigation bar

        setupViews();
    }

    private void setupViews() {
        TextView titleText = findViewById(R.id.title_text);
        Button button1 = findViewById(R.id.button_1);
        Button button2 = findViewById(R.id.button_2);
        Button bottomButton = findViewById(R.id.bottom_button);

        titleText.setText("Problematic Activity");

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProblematicActivity.this, "Button 1 clicked", Toast.LENGTH_SHORT).show();
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProblematicActivity.this, "Button 2 clicked", Toast.LENGTH_SHORT).show();
            }
        });

        bottomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProblematicActivity.this, "Bottom button clicked", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
