package com.authbar87.authenticator;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.authbar87.authenticator.model.Account;
import com.authbar87.authenticator.util.Base32;
import com.authbar87.authenticator.util.OtpAuthUri;
import com.authbar87.authenticator.util.SecureStorage;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.List;

public class AddAccountActivity extends AppCompatActivity {

    public static final String EXTRA_EDIT_ID = "extra_edit_id";
    private static final int REQ_CAMERA_PERMISSION = 2001;

    private SecureStorage storage;
    private TextInputEditText editIssuer, editAccountName, editSecret, editDigits, editPeriod;
    private AutoCompleteTextView dropdownAlgorithm;
    private View advancedContainer;
    private boolean advancedVisible = false;
    private String editingId; // non-null when editing an existing account

    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> qrLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    handleScannedText(result.getContents());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_account);

        storage = new SecureStorage(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        editIssuer = findViewById(R.id.edit_issuer);
        editAccountName = findViewById(R.id.edit_account_name);
        editSecret = findViewById(R.id.edit_secret);
        editDigits = findViewById(R.id.edit_digits);
        editPeriod = findViewById(R.id.edit_period);
        dropdownAlgorithm = findViewById(R.id.dropdown_algorithm);
        advancedContainer = findViewById(R.id.advanced_options_container);

        ArrayAdapter<String> algoAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                new String[]{"SHA1", "SHA256", "SHA512"});
        dropdownAlgorithm.setAdapter(algoAdapter);
        dropdownAlgorithm.setText("SHA1", false);

        findViewById(R.id.button_toggle_advanced).setOnClickListener(v -> {
            advancedVisible = !advancedVisible;
            advancedContainer.setVisibility(advancedVisible ? View.VISIBLE : View.GONE);
        });

        findViewById(R.id.button_scan_qr).setOnClickListener(v -> requestCameraAndScan());
        findViewById(R.id.button_save).setOnClickListener(v -> saveAccount());

        editingId = getIntent().getStringExtra(EXTRA_EDIT_ID);
        if (editingId != null) {
            loadForEditing(editingId);
        }
    }

    private void loadForEditing(String id) {
        for (Account a : storage.loadAccounts()) {
            if (a.id.equals(id)) {
                editIssuer.setText(a.issuer);
                editAccountName.setText(a.accountName);
                editSecret.setText(a.secretBase32);
                editDigits.setText(String.valueOf(a.digits));
                editPeriod.setText(String.valueOf(a.period));
                dropdownAlgorithm.setText(a.algorithm.name(), false);
                break;
            }
        }
    }

    private void requestCameraAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_PERMISSION);
        } else {
            launchScanner();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchScanner();
            } else {
                Toast.makeText(this, R.string.error_camera_permission, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchScanner() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        options.setPrompt("");
        qrLauncher.launch(options);
    }

    private void handleScannedText(String text) {
        Account parsed = OtpAuthUri.parse(text);
        if (parsed == null) {
            Toast.makeText(this, R.string.error_invalid_secret, Toast.LENGTH_LONG).show();
            return;
        }
        editIssuer.setText(parsed.issuer);
        editAccountName.setText(parsed.accountName);
        editSecret.setText(parsed.secretBase32);
        editDigits.setText(String.valueOf(parsed.digits));
        editPeriod.setText(String.valueOf(parsed.period));
        dropdownAlgorithm.setText(parsed.algorithm.name(), false);
        // Auto-save directly after a successful scan for a smooth flow.
        saveAccount();
    }

    private void saveAccount() {
        String issuer = safeText(editIssuer);
        String accountName = safeText(editAccountName);
        String secret = safeText(editSecret).replace(" ", "");

        if (secret.isEmpty() || (issuer.isEmpty() && accountName.isEmpty())) {
            Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Base32.isValid(secret)) {
            Toast.makeText(this, R.string.error_invalid_secret, Toast.LENGTH_LONG).show();
            return;
        }

        Account account = new Account();
        if (editingId != null) account.id = editingId;
        account.issuer = issuer;
        account.accountName = accountName;
        account.secretBase32 = secret;

        try {
            account.digits = Integer.parseInt(safeText(editDigits));
        } catch (NumberFormatException e) {
            account.digits = 6;
        }
        try {
            account.period = Integer.parseInt(safeText(editPeriod));
        } catch (NumberFormatException e) {
            account.period = 30;
        }

        String algo = dropdownAlgorithm.getText().toString();
        switch (algo) {
            case "SHA256": account.algorithm = Account.Algorithm.SHA256; break;
            case "SHA512": account.algorithm = Account.Algorithm.SHA512; break;
            default: account.algorithm = Account.Algorithm.SHA1;
        }

        List<Account> current = new ArrayList<>(storage.loadAccounts());
        if (editingId != null) {
            for (int i = 0; i < current.size(); i++) {
                if (current.get(i).id.equals(editingId)) {
                    current.set(i, account);
                    break;
                }
            }
        } else {
            current.add(account);
        }
        storage.saveAccounts(current);

        setResult(RESULT_OK);
        finish();
    }

    private String safeText(TextInputEditText edit) {
        return edit.getText() == null ? "" : edit.getText().toString().trim();
    }
}
