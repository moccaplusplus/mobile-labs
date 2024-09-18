package uksw.android.smartdocs.client;

import static uksw.android.smartdocs.shared.Dialogs.alert;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import uksw.android.smartdocs.shared.Dialogs;

public class EditorActivity extends AppCompatActivity {
    private EditText editor;
    private Button saveButton;
    private Uri uri;
    private TextView titleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        titleView = findViewById(R.id.title_text);
        editor = findViewById(R.id.edit_text);
        saveButton = findViewById(R.id.button_save);
        saveButton.setOnClickListener(v -> save());
        findViewById(R.id.button_close).setOnClickListener(v -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        uri = getIntent().getData();
        if (uri == null) {
            alert(this, R.string.error, R.string.missing_uri, this::finish);
            return;
        }

        titleView.setText(getString(R.string.file_name, uri.getLastPathSegment()));
        saveButton.setEnabled(true);
        readFile();
    }

    private void save() {
        String content = editor.getText().toString();
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(getContentResolver().openOutputStream(uri)))) {
            writer.write(content);
            alert(this, R.string.file_saved);
        } catch (IOException e) {
            alert(this, R.string.error, R.string.failed_write_file);
        }
    }

    private void readFile() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getContentResolver().openInputStream(uri)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                editor.append(line);
            }
        } catch (IOException e) {
            alert(this, R.string.error, R.string.failed_read_file);
        }
    }
}