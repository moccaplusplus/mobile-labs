package uksw.android.smartdocs.client;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class EditorActivity extends AppCompatActivity {
    private Uri file;
    private EditText editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        editor = findViewById(R.id.edit_text);
        findViewById(R.id.button_save).setOnClickListener(v -> saveAs());
    }

    private void saveAs() {
        Uri uri = DocumentsContract.buildDocumentUri("uksw.android.smartdocs.client", "root");
        createFile(uri);

//        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uriToLoad);
//        }
//
//        startActivityForResult(intent, your-request-code);

        editor.getText();
    }

    private void save() {
        if (file == null) {

            return;
        }
        String content = editor.getText().toString();
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(getContentResolver().openOutputStream(file)))) {
            writer.write(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createFile(Uri directory) {
//        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
//        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(SmartDocsClientProvider.SMART_DOC_MIME_TYPE);
//        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, directory);

        startActivityForResult(intent, 123);
    }
}