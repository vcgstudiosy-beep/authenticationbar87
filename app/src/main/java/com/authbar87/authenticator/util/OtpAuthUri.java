package com.authbar87.authenticator.util;

import android.net.Uri;

import com.authbar87.authenticator.model.Account;

/**
 * Parses / builds standard otpauth:// URIs
 * Format: otpauth://totp/Issuer:account?secret=BASE32&issuer=Issuer&algorithm=SHA1&digits=6&period=30
 */
public final class OtpAuthUri {

    private OtpAuthUri() {}

    public static Account parse(String uriString) {
        try {
            Uri uri = Uri.parse(uriString);
            if (!"otpauth".equalsIgnoreCase(uri.getScheme())) return null;

            String host = uri.getHost(); // "totp" or "hotp"
            Account account = new Account();
            account.type = "hotp".equalsIgnoreCase(host) ? Account.Type.HOTP : Account.Type.TOTP;

            String path = uri.getPath();
            String label = path != null && path.startsWith("/") ? path.substring(1) : path;
            if (label != null && label.contains(":")) {
                String[] parts = label.split(":", 2);
                account.issuer = Uri.decode(parts[0]);
                account.accountName = Uri.decode(parts[1]);
            } else {
                account.accountName = label != null ? Uri.decode(label) : "";
            }

            String secret = uri.getQueryParameter("secret");
            if (secret == null || !Base32.isValid(secret)) return null;
            account.secretBase32 = secret;

            String issuerParam = uri.getQueryParameter("issuer");
            if (issuerParam != null && !issuerParam.isEmpty()) {
                account.issuer = issuerParam;
            }

            String algo = uri.getQueryParameter("algorithm");
            if (algo != null) {
                switch (algo.toUpperCase()) {
                    case "SHA256": account.algorithm = Account.Algorithm.SHA256; break;
                    case "SHA512": account.algorithm = Account.Algorithm.SHA512; break;
                    default: account.algorithm = Account.Algorithm.SHA1;
                }
            }

            String digits = uri.getQueryParameter("digits");
            if (digits != null) {
                try { account.digits = Integer.parseInt(digits); } catch (NumberFormatException ignored) {}
            }

            String period = uri.getQueryParameter("period");
            if (period != null) {
                try { account.period = Integer.parseInt(period); } catch (NumberFormatException ignored) {}
            }

            String counter = uri.getQueryParameter("counter");
            if (counter != null) {
                try { account.counter = Long.parseLong(counter); } catch (NumberFormatException ignored) {}
            }

            return account;
        } catch (Exception e) {
            return null;
        }
    }

    public static String build(Account a) {
        String type = a.type == Account.Type.HOTP ? "hotp" : "totp";
        String label = (a.issuer != null && !a.issuer.isEmpty())
                ? Uri.encode(a.issuer) + ":" + Uri.encode(a.accountName)
                : Uri.encode(a.accountName);

        StringBuilder sb = new StringBuilder();
        sb.append("otpauth://").append(type).append("/").append(label);
        sb.append("?secret=").append(Uri.encode(a.secretBase32));
        if (a.issuer != null) sb.append("&issuer=").append(Uri.encode(a.issuer));
        sb.append("&algorithm=").append(a.algorithm.name());
        sb.append("&digits=").append(a.digits);
        if (a.type == Account.Type.TOTP) {
            sb.append("&period=").append(a.period);
        } else {
            sb.append("&counter=").append(a.counter);
        }
        return sb.toString();
    }
}
