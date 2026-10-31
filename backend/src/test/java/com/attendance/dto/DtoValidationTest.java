package com.attendance.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.attendance.domain.AttendanceStatus;
import com.attendance.domain.SessionStatus;
import com.attendance.dto.AttendanceDtos.AttendanceEntry;
import com.attendance.dto.AttendanceDtos.MarkAttendanceRequest;
import com.attendance.dto.AuthDtos.LoginRequest;
import com.attendance.dto.StudentDtos.StudentCreateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Bean-validation rules on request DTOs (the same rules the controllers enforce with @Valid). */
class DtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static Set<String> fields(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    @Test
    void validStudentHasNoViolations() {
        var r = new StudentCreateRequest("MCA1", "Ravi", "ravi@example.edu", "CA", "MCA", "2025-26", "ravi.k", "longenough1");
        assertTrue(validator.validate(r).isEmpty());
    }

    @Test
    void invalidStudentReportsEachBadField() {
        var r = new StudentCreateRequest(" ", "", "not-an-email", "CA", "MCA", "2025-26", "bad name!", "short");
        Set<String> bad = fields(validator.validate(r));
        assertTrue(bad.containsAll(Set.of("studentCode", "fullName", "email", "username", "password")), bad.toString());
    }

    @Test
    void loginRequiresBothFields() {
        assertEquals(Set.of("username", "password"), fields(validator.validate(new LoginRequest("", ""))));
    }

    @Test
    void attendanceRequestNeedsDateStatusAndValidSlot() {
        var r = new MarkAttendanceRequest(null, 11, null, null);
        assertEquals(Set.of("date", "slot", "status"), fields(validator.validate(r)));
    }

    @Test
    void attendanceEntriesAreValidated() {
        var r = new MarkAttendanceRequest(LocalDate.now(), 1, SessionStatus.HELD,
                List.of(new AttendanceEntry(null, AttendanceStatus.PRESENT)));
        assertTrue(fields(validator.validate(r)).contains("records[0].studentId"));
    }
}
