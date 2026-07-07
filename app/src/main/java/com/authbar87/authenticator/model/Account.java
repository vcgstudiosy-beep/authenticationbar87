package com.authbar87.authenticator.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Represents one TOTP/HOTP account entry.
 */
public class Account {

    public enum Algorithm { SHA1, SHA256, SHA512 }
    public enum Type { TOTP, HOTP }

    public String id;
    public String issuer;
    public String accountName;
    public String secretBase32;
    public Algorithm algorithm = Algorithm.SHA1;
    public Type type = Type.TOTP;
    public int digits = 6;
    public int period = 30;      // seconds, TOTP only
    public long counter = 0;     // HOTP only
    public String colorHex = "#2196F3";

    public Account() {
        this.id = UUID.randomUUID().toString();
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("issuer", issuer);
        o.put("accountName", accountName);
        o.put("secret", secretBase32);
        o.put("algorithm", algorithm.name());
        o.put("type", type.name());
        o.put("digits", digits);
        o.put("period", period);
        o.put("counter", counter);
        o.put("color", colorHex);
        return o;
    }

    public static Account fromJson(JSONObject o) throws JSONException {
        Account a = new Account();
        a.id = o.optString("id", UUID.randomUUID().toString());
        a.issuer = o.optString("issuer", "");
        a.accountName = o.optString("accountName", "");
        a.secretBase32 = o.optString("secret", "");
        a.algorithm = Algorithm.valueOf(o.optString("algorithm", "SHA1"));
        a.type = Type.valueOf(o.optString("type", "TOTP"));
        a.digits = o.optInt("digits", 6);
        a.period = o.optInt("period", 30);
        a.counter = o.optLong("counter", 0);
        a.colorHex = o.optString("color", "#2196F3");
        return a;
    }

    public String displayName() {
        if (issuer != null && !issuer.isEmpty()) {
            if (accountName != null && !accountName.isEmpty()) {
                return issuer + " (" + accountName + ")";
            }
            return issuer;
        }
        return accountName != null ? accountName : "Account";
    }
}
