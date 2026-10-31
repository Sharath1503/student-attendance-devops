package com.attendance.service;

/**
 * Single place that defines how attendance percentages are calculated.
 *
 * <pre>
 * percentage = PRESENT / (PRESENT + ABSENT) * 100   (rounded to 2 decimals)
 * </pre>
 *
 * Only records of HELD sessions are ever passed in. CANCELLED and HOLIDAY sessions, and sessions for which
 * a student has no record, are therefore not counted. With no counted sessions the percentage is {@code null}
 * ("not available") instead of 0 or 100.
 */
public final class AttendanceCalculator {

    private AttendanceCalculator() {}

    public static Double percentage(long present, long total) {
        if (total <= 0) {
            return null;
        }
        return Math.round(present * 10000.0 / total) / 100.0;
    }

    /** A student is "low" when a percentage exists and is strictly below the threshold. */
    public static boolean isLow(Double percentage, double thresholdPercent) {
        return percentage != null && percentage < thresholdPercent;
    }
}
