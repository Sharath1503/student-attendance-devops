package com.attendance.domain;

/**
 * HELD sessions count towards attendance percentages.
 * CANCELLED and HOLIDAY sessions are stored for the record but are never counted.
 */
public enum SessionStatus {
    HELD, CANCELLED, HOLIDAY
}
