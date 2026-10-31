package com.attendance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AttendanceCalculatorTest {

    @Test
    @DisplayName("percentage is present / total * 100, rounded to 2 decimals")
    void percentageIsRounded() {
        assertEquals(66.67, AttendanceCalculator.percentage(2, 3));
        assertEquals(33.33, AttendanceCalculator.percentage(1, 3));
        assertEquals(75.0, AttendanceCalculator.percentage(3, 4));
    }

    @Test
    @DisplayName("all present is 100 and none present is 0")
    void boundaries() {
        assertEquals(100.0, AttendanceCalculator.percentage(8, 8));
        assertEquals(0.0, AttendanceCalculator.percentage(0, 8));
    }

    @Test
    @DisplayName("no counted sessions gives null (not available), never a divide-by-zero or a fake 0/100")
    void noSessionsIsNull() {
        assertNull(AttendanceCalculator.percentage(0, 0));
    }

    @Test
    @DisplayName("low-attendance flag is strictly below the threshold and never set for 'not available'")
    void lowFlag() {
        assertTrue(AttendanceCalculator.isLow(74.99, 75.0));
        assertFalse(AttendanceCalculator.isLow(75.0, 75.0));
        assertFalse(AttendanceCalculator.isLow(90.0, 75.0));
        assertFalse(AttendanceCalculator.isLow(null, 75.0));
    }
}
