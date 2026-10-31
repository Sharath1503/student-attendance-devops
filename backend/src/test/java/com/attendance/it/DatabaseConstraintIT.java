package com.attendance.it;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.attendance.domain.AttendanceStatus;
import com.attendance.domain.SessionStatus;
import com.attendance.entity.AttendanceRecord;
import com.attendance.entity.ClassSession;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** The database itself (not only the service code) refuses duplicate attendance data. */
class DatabaseConstraintIT extends IntegrationTestBase {

    private ClassSession newSession(LocalDate date, int slot) {
        ClassSession s = new ClassSession();
        s.setCourse(courseA);
        s.setSessionDate(date);
        s.setSlot(slot);
        s.setStatus(SessionStatus.HELD);
        return s;
    }

    private AttendanceRecord newRecord(ClassSession session) {
        AttendanceRecord r = new AttendanceRecord();
        r.setSession(session);
        r.setStudent(stu1);
        r.setStatus(AttendanceStatus.PRESENT);
        return r;
    }

    @Test
    @DisplayName("unique (session, student): the same student cannot get two records for one class")
    void duplicateAttendanceRecordRejected() {
        ClassSession session = sessionRepository.saveAndFlush(newSession(LocalDate.now(), 1));
        recordRepository.saveAndFlush(newRecord(session));
        assertThrows(DataIntegrityViolationException.class, () -> recordRepository.saveAndFlush(newRecord(session)));
    }

    @Test
    @DisplayName("unique (course, date, slot): a class cannot be created twice")
    void duplicateSessionRejected() {
        sessionRepository.saveAndFlush(newSession(LocalDate.now(), 1));
        assertThrows(DataIntegrityViolationException.class,
                () -> sessionRepository.saveAndFlush(newSession(LocalDate.now(), 1)));
    }

    @Test
    @DisplayName("unique (student, course): double enrolment is rejected")
    void duplicateEnrollmentRejected() {
        assertThrows(DataIntegrityViolationException.class, () ->
                enrollmentRepository.saveAndFlush(new com.attendance.entity.Enrollment(stu1, courseA)));
    }
}
