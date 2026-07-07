package com.authbar87.authenticator;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.authbar87.authenticator.adapter.AccountAdapter;
import com.authbar87.authenticator.model.Account;
import com.authbar87.authenticator.util.OtpAuthUri;
import com.authbar87.authenticator.util.SecureStorage;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements AccountAdapter.Listener {

    private static final int REQ_ADD_ACCOUNT = 1001;

    private SecureStorage storage;
    private final List<Account> accounts = new ArrayList<>();
    private final List<Account> filtered = new ArrayList<>();
    private AccountAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyState;

    private final Handler tickHandler = new Handler(Looper.getMainLooper());
    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (adapter != null) adapter.tick();
            tickHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storage = new SecureStorage(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recycler_accounts);
        emptyState = findViewById(R.id.empty_state);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AccountAdapter(filtered, this);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(v ->
                startActivityForResult(new Intent(this, AddAccountActivity.class), REQ_ADD_ACCOUNT));

        com.google.android.material.textfield.TextInputEditText searchEdit = findViewById(R.id.edit_search);
        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { applyFilter(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe = findViewById(R.id.swipe_refresh);
        swipe.setOnRefreshListener(() -> {
            reloadAccounts();
            swipe.setRefreshing(false);
        });

        reloadAccounts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadAccounts();
        tickHandler.post(tickRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        tickHandler.removeCallbacks(tickRunnable);
    }

    private void reloadAccounts() {
        accounts.clear();
        accounts.addAll(storage.loadAccounts());
        applyFilter("");
    }

    private void applyFilter(String query) {
        filtered.clear();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        for (Account a : accounts) {
            if (q.isEmpty() || a.displayName().toLowerCase(Locale.getDefault()).contains(q)) {
                filtered.add(a);
            }
        }
        adapter.notifyDataSetChanged();
        emptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ADD_ACCOUNT && resultCode == RESULT_OK) {
            reloadAccounts();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ---- AccountAdapter.Listener ----

    @Override
    public void onCodeTapped(Account account, String code) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("otp_code", code));
        Toast.makeText(this, R.string.code_copied, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onHotpRefresh(Account account) {
        storage.saveAccounts(accounts);
    }

    @Override
    public void onLongPress(Account account, View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenuInflater().inflate(R.menu.menu_account_item, menu.getMenu());
        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_copy) {
                onCodeTapped(account, com.authbar87.authenticator.util.TotpGenerator.generate(
                        account, System.currentTimeMillis() / 1000L));
                return true;
            } else if (id == R.id.action_show_qr) {
                showQrDialog(account);
                return true;
            } else if (id == R.id.action_edit) {
                Intent intent = new Intent(this, AddAccountActivity.class);
                intent.putExtra(AddAccountActivity.EXTRA_EDIT_ID, account.id);
                startActivityForResult(intent, REQ_ADD_ACCOUNT);
                return true;
            } else if (id == R.id.action_delete) {
                confirmDelete(account);
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void confirmDelete(Account account) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    accounts.removeIf(a -> a.id.equals(account.id));
                    storage.saveAccounts(accounts);
                    applyFilter("");
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showQrDialog(Account account) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_show_qr, null);
        TextView title = view.findViewById(R.id.text_dialog_title);
        ImageView qrImage = view.findViewById(R.id.image_qr);
        title.setText(account.displayName());

        try {
            String uri = OtpAuthUri.build(account);
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(uri, BarcodeFormat.QR_CODE, 512, 512);
            Bitmap bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565);
            for (int x = 0; x < 512; x++) {
                for (int y = 0; y < 512; y++) {
                    bmp.setPixel(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            qrImage.setImageBitmap(bmp);
        } catch (WriterException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
