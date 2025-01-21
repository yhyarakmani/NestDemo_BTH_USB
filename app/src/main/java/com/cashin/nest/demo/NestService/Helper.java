package com.cashin.nest.demo.NestService;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Helper {
    public static double round(double d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Double.toString(d));
        bd = bd.setScale(decimalPlace, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
    public static String stringToUnicode(String input) {
        StringBuilder unicodeString = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c > 127) { // Non-ASCII characters
                unicodeString.append(String.format("\\u%04x", (int) c));
            } else {
                unicodeString.append(c); // Keep ASCII characters as is
            }
        }
        return unicodeString.toString();
    }
}
