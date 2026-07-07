package com.authbar87.authenticator.util;

/**
 * Minimal RFC 4648 Base32 decoder (no external dependency needed).
 */
public final class Base32 {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private Base32() {}

    public static byte[] decode(String input) {
        if (input == null) return new byte[0];
        String clean = input.trim().toUpperCase().replace("=", "").replaceAll("\\s+", "");
        if (clean.isEmpty()) return new byte[0];

        int byteCount = clean.length() * 5 / 8;
        byte[] result = new byte[byteCount];

        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;

        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            int val = ALPHABET.indexOf(c);
            if (val < 0) continue; // skip invalid chars
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return result;
    }

    public static boolean isValid(String input) {
        if (input == null || input.trim().isEmpty()) return false;
        String clean = input.trim().toUpperCase().replace("=", "").replaceAll("\\s+", "");
        for (char c : clean.toCharArray()) {
            if (ALPHABET.indexOf(c) < 0) return false;
        }
        return !clean.isEmpty();
    }
}
