package uksw.android.smartdocs.client;

import static uksw.android.smartdocs.shared.Dialogs.alert;

import android.os.Bundle;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

import uksw.android.smartdocs.shared.AbstractFileActivity;

public class EditorActivity extends AbstractFileActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        titleView = findViewById(R.id.title_text);
        contentsView = findViewById(R.id.edit_text);
        findViewById(R.id.button_close).setOnClickListener(v -> finish());
        findViewById(R.id.button_save).setOnClickListener(v -> save());
    }

    @Override
    protected void readFile() {
        contentsView.setEnabled(false);
        super.readFile();
    }

    @Override
    protected void readFileInBackground() {
        super.readFileInBackground();
        runOnUiThread(() -> contentsView.setEnabled(true));
    }

    private void save() {
        String content = contentsView.getText().toString();
        new Thread(() -> save(content)).start();
    }

    private void save(String content) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(getContentResolver().openOutputStream(uri, "w")))) {
            writer.write(content);
            runOnUiThread(() -> alert(this, R.string.file_saved, this::finish));
            getContentResolver().notifyChange(uri, observer);
        } catch (Exception e) {
            runOnUiThread(() -> alert(this, getString(R.string.error), getString(R.string.failed_write_file, e.getMessage())));
        }
    }
}