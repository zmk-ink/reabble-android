package com.zmk.ink.reabble.utils;

public class TimingUtil {
    private static long lastCallTime = 0;

    public static long logTimeDifference() {
        long currentTime = System.currentTimeMillis();
        long timeDifference = currentTime - lastCallTime;
        lastCallTime = currentTime;
        return timeDifference;
    }
}