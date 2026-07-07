package com.authbar87.authenticator;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.authbar87.authenticator.model.Account;
import com.authbar87.authenticator.util.OtpAuthUri;
import com.authbar87.authenticator.util.SecureStorage;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private SecureStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        storage = new SecureStorage(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        MaterialSwitch appLockSwitch = findViewById(R.id.switch_app_lock);
        appLockSwitch.setChecked(storage.isAppLockEnabled());
        appLockSwitch.setOnCheckedChangeListener((btn, checked) ->
                storage.setAppLockEnabled(checked));

        findViewById(R.id.button_export).setOnClickListener(v -> exportAccounts());
        findViewById(R.id.button_import).setOnClickListener(v ->
                Toast.makeText(this, R.string.import_accounts, Toast.LENGTH_SHORT).show());
    }

    /**
     * Exports all otpauth:// URIs (one per line) to the clipboard.
     * Note: this exposes plaintext secrets — advise users to only use this
     * on a trusted device and to clear the clipboard afterwards.
     */
    private void exportAccounts() {
        List<Account> accounts = storage.loadAccounts();
        StringBuilder sb = new StringBuilder();
        for (Account a : accounts) {
            sb.append(OtpAuthUri.build(a)).append("\n");
        }
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("otpauth_export", sb.toString().trim()));
        Toast.makeText(this, R.string.code_copied, Toast.LENGTH_SHORT).show();
    }
}
