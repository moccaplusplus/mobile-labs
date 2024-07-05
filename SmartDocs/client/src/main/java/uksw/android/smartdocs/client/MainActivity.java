package uksw.android.smartdocs.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.button_discover).setOnClickListener(v -> openDiscoveryActivity());
    }

    private void openDiscoveryActivity() {
        Intent intent = new Intent(this, DiscoveryActivity.class);
        startActivity(intent);
    }
}