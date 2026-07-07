package com.authbar87.authenticator.util;

import com.authbar87.authenticator.model.Account;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Generates TOTP (RFC 6238) and HOTP (RFC 4226) one-time codes.
 */
public final class TotpGenerator {

    private TotpGenerator() {}

    public static String generate(Account account, long unixTimeSeconds) {
        long counter = account.type == Account.Type.HOTP
                ? account.counter
                : unixTimeSeconds / Math.max(1, account.period);
        return generateForCounter(account, counter);
    }

    private static String generateForCounter(Account account, long counter) {
        try {
            byte[] key = Base32.decode(account.secretBase32);
            byte[] data = new byte[8];
            long value = counter;
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (value & 0xFF);
                value >>= 8;
            }

            String macAlgo;
            switch (account.algorithm) {
                case SHA256: macAlgo = "HmacSHA256"; break;
                case SHA512: macAlgo = "HmacSHA512"; break;
                default: macAlgo = "HmacSHA1";
            }

            Mac mac = Mac.getInstance(macAlgo);
            mac.init(new SecretKeySpec(key, macAlgo));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary =
                    ((hash[offset] & 0x7F) << 24) |
                    ((hash[offset + 1] & 0xFF) << 16) |
                    ((hash[offset + 2] & 0xFF) << 8) |
                    (hash[offset + 3] & 0xFF);

            int digits = account.digits <= 0 ? 6 : account.digits;
            int otp = binary % (int) Math.pow(10, digits);

            StringBuilder sb = new StringBuilder(Integer.toString(otp));
            while (sb.length() < digits) sb.insert(0, '0');
            return sb.toString();
        } catch (Exception e) {
            return "------";
        }
    }

    /** Seconds remaining until the current TOTP window expires. */
    public static int secondsRemaining(Account account, long unixTimeSeconds) {
        int period = Math.max(1, account.period);
        return (int) (period - (unixTimeSeconds % period));
    }
}
