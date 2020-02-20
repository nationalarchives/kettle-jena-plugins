package com.evolvedbinary.pdi;

public class Util {

    public static String nullIfEmpty(final String s) {
        if (s != null && !s.isEmpty()) {
            return s;
        } else {
            return null;
        }
    }

    public static String emptyIfNull(final String s) {
        if (s == null) {
            return "";
        } else {
            return s;
        }
    }
}
