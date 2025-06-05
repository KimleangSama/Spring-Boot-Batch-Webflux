package com.keakimleang.springbatchwebflux.utils;

import org.apache.commons.lang3.*;

public final class StringWrapperUtils {

    private StringWrapperUtils() {
    }

    public static String strip(final String s) {
        return StringUtils.strip(s);
    }

    public static String strip(final String s,
                               final String stripChars) {
        return StringUtils.strip(s, stripChars);
    }

    public static boolean equals(final String s1,
                                 final String s2) {
        return StringUtils.equals(s1, s2);
    }

    public static boolean equalsIgnoreCase(final String s1,
                                           final String s2) {
        return StringUtils.equalsIgnoreCase(s1, s2);
    }

    public static boolean isBlank(final String s1) {
        return StringUtils.isBlank(s1);
    }

    public static boolean isNotBlank(final String s1) {
        return StringUtils.isNotBlank(s1);
    }

    public static boolean isAnyBlank(final String...s) {
        return StringUtils.isAnyBlank(s);
    }

    public static boolean endsWithIgnoreCase(final String s,
                                             final String suffix) {
        return StringUtils.endsWithIgnoreCase(s, suffix);
    }

    public static String upperCase(final String s) {
        return StringUtils.upperCase(s);
    }

    public static int countMatch(final String s,
                                 final String countStr) {
        return StringUtils.countMatches(s, countStr);
    }
}
