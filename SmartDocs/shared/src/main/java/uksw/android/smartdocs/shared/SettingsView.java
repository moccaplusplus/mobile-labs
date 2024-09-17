package uksw.android.smartdocs.shared;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.GridLayout;

public class SettingsView extends GridLayout {
    private final TextWatcher portUpdateListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            int port = -1;
            try {
                port = Integer.parseInt(s.toString());
                if (port == 0) {
                    port = -1;
                    s.clear();
                }
            } catch (Exception ignored) {
            }
            settings.setUdpServerPort(port);
        }
    };
    private EditText udpPortInput;
    private Settings settings;

    public SettingsView(Context context) {
        super(context);
        init();
    }

    public SettingsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SettingsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.settings_view, this);
        settings = Settings.get(getContext());
        udpPortInput = findViewById(R.id.udp_port_input);
        updatePortView();
        udpPortInput.addTextChangedListener(portUpdateListener);
    }

    private void updatePortView() {
        int port = settings.getUdpServerPort();
        if (port == 0) {
            udpPortInput.getText().clear();
        } else {
            udpPortInput.setText(String.valueOf(port));
        }
    }

    public void setEnabled(boolean enabled) {
        udpPortInput.setEnabled(enabled);
    }

    public int getUdpServerPort() {
        return settings.getUdpServerPort();
    }
}
