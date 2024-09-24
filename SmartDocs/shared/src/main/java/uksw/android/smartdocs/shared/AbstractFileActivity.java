package uksw.android.smartdocs.shared;

import static uksw.android.smartdocs.shared.Dialogs.alert;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public abstract class AbstractFileActivity extends AppCompatActivity {
    protected final ContentObserver observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            if (!selfChange) {
                onFileChanged();
            }
        }
    };

    protected TextView titleView;
    protected TextView contentsView;
    protected Uri uri;

    @Override
    protected void onStart() {
        super.onStart();
        uri = getIntent().getData();
        if (uri == null) {
            alert(this, R.string.error, R.string.missing_uri, this::finish);
            return;
        }
        titleView.setText(getString(R.string.file_name, uri.getLastPathSegment()));
        readFile();
        getContentResolver().registerContentObserver(uri, false, observer);
    }

    @Override
    protected void onStop() {
        getContentResolver().unregisterContentObserver(observer);
        super.onStop();
    }

    protected void onFileChanged() {
        Dialogs.confirm(this, R.string.file_modified, R.string.file_modified_outside, this::readFile);
    }

    protected void readFile() {
        new Thread(this::readFileInBackground).start();
    }

    protected void readFileInBackground() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getContentResolver().openInputStream(uri)))) {
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                runOnUiThread(() -> contentsView.append(line + "\n"));
            }
        } catch (Exception e) {
            runOnUiThread(() -> alert(this, getString(R.string.error),
                    getString(R.string.failed_read_file, e.getMessage()), this::finish));
        }
    }
}
