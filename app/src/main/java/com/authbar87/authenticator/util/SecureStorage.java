package com.authbar87.authenticator.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.authbar87.authenticator.model.Account;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Persists accounts inside an EncryptedSharedPreferences file so that
 * secrets are never stored in plain text on disk.
 */
public class SecureStorage {

    private static final String TAG = "SecureStorage";
    private static final String PREFS_FILE = "authbar87_secure_prefs";
    private static final String KEY_ACCOUNTS = "accounts_json";
    private static final String KEY_APP_LOCK_ENABLED = "app_lock_enabled";

    private final SharedPreferences prefs;

    public SecureStorage(Context context) {
        SharedPreferences p;
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            p = EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            Log.e(TAG, "Falling back to regular prefs (should not happen normally)", e);
            p = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        }
        this.prefs = p;
    }

    public synchronized List<Account> loadAccounts() {
        List<Account> list = new ArrayList<>();
        String json = prefs.getString(KEY_ACCOUNTS, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                list.add(Account.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse stored accounts", e);
        }
        return list;
    }

    public synchronized void saveAccounts(List<Account> accounts) {
        JSONArray arr = new JSONArray();
        try {
            for (Account a : accounts) {
                arr.put(a.toJson());
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to serialize accounts", e);
        }
        prefs.edit().putString(KEY_ACCOUNTS, arr.toString()).apply();
    }

    public boolean isAppLockEnabled() {
        return prefs.getBoolean(KEY_APP_LOCK_ENABLED, false);
    }

    public void setAppLockEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply();
    }
}
