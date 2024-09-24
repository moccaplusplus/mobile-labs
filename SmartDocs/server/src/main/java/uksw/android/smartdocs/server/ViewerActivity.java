package uksw.android.smartdocs.server;

import android.os.Bundle;

import uksw.android.smartdocs.shared.AbstractFileActivity;

public class ViewerActivity extends AbstractFileActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);
        titleView = findViewById(R.id.title_text);
        contentsView = findViewById(R.id.contents_text);
        findViewById(R.id.button_close).setOnClickListener(v -> finish());
    }
}