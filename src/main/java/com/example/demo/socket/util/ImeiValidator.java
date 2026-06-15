package com.example.demo.socket.util;

/**
 * Validates bracelet IMEI values before registering or auto-creating devices.
 */
public final class ImeiValidator {
    private static final String TEST_IMEI_PREFIX = "359999";

    private ImeiValidator() {
    }

    public static boolean isValid(String imei) {
        if (imei == null || !imei.matches("\\d{15}")) {
            return false;
        }
        if (imei.matches("^0+$") || imei.startsWith("00")) {
            return false;
        }
        if (imei.startsWith(TEST_IMEI_PREFIX)) {
            return true;
        }
        return passesLuhn(imei);
    }

    private static boolean passesLuhn(String imei) {
        int sum = 0;
        int parity = imei.length() % 2;
        for (int i = 0; i < imei.length(); i++) {
            int digit = imei.charAt(i) - '0';
            if (i % 2 == parity) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
        }
        return sum % 10 == 0;
    }
}
