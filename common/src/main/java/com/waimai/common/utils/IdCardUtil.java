package com.waimai.common.utils;

/**
 * Chinese 18-digit ID card validation utility.
 * Rules: area code (6) + birthday (8) + sequence (3) + checksum (1).
 */
public class IdCardUtil {

    private static final int[] WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final char[] CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    private IdCardUtil() {}

    public static boolean isValid(String idCard) {
        if (idCard == null || idCard.length() != 18) return false;
        // First 17 must be digits
        for (int i = 0; i < 17; i++) {
            if (!Character.isDigit(idCard.charAt(i))) return false;
        }
        // Last char: digit or X
        char last = idCard.charAt(17);
        if (!Character.isDigit(last) && last != 'X' && last != 'x') return false;

        // Checksum
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (idCard.charAt(i) - '0') * WEIGHTS[i];
        }
        char expected = CHECK_CODES[sum % 11];
        return expected == Character.toUpperCase(last);
    }

    public static String extractBirthday(String idCard) {
        if (!isValid(idCard)) return null;
        String y = idCard.substring(6, 10);
        String m = idCard.substring(10, 12);
        String d = idCard.substring(12, 14);
        return y + "-" + m + "-" + d;
    }

    public static String extractGender(String idCard) {
        if (!isValid(idCard)) return null;
        int seq = Integer.parseInt(idCard.substring(14, 17));
        return seq % 2 == 1 ? "男" : "女";
    }
}
