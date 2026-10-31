package com.attendance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.attendance.config.AppProperties;
import com.attendance.domain.AttendanceStatus;
import com.attendance.domain.Role;
import com.attendance.domain.SessionStatus;
import com.attendance.dto.AttendanceDtos.AttendanceEntry;
import com.attendance.dto.AttendanceDtos.MarkAttendanceRequest;
import com.attendance.entity.AttendanceRecord;
import com.attendance.entity.ClassSession;
import com.attendance.entity.Course;
import com.attendance.entity.Faculty;
import com.attendance.entity.Student;
import com.attendance.exception.BadRequestException;
import com.attendance.repository.AttendanceRecordRepository;
import com.attendance.repository.ClassSessionRepository;
import com.attendance.repository.CourseAssignmentRepository;
import com.attendance.repository.CourseRepository;
import com.attendance.repository.EnrollmentRepository;
import com.attendance.repository.FacultyRepository;
import com.attendance.repository.StudentRepository;
import com.attendance.security.AppUserDetails;
import com.attendance.security.CurrentUserService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

/** Pure unit tests of the attendance business rules (repositories are mocked, no database). */
@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    private static final Long COURSE_ID = 1L;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-10T10:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);

    @Mock CourseRepository courseRepository;
    @Mock FacultyRepository facultyRepository;
    @Mock StudentRepository studentRepository;
    @Mock CourseAssignmentRepository assignmentRepository;
    @Mock EnrollmentRepository enrollmentRepository;
    @Mock ClassSessionRepository sessionRepository;
    @Mock AttendanceRecordRepository recordRepository;
    @Mock CurrentUserService currentUser;
    @Mock AuditService audit;

    private AttendanceService service;
    private Faculty faculty;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(
                new AppProperties.Jwt("x".repeat(40), 60),
                new AppProperties.Cors(List.of("http://localhost")),
                new AppProperties.Attendance(7, 75.0),
                new AppProperties.Bootstrap("admin", ""),
                new AppProperties.Seed(false, ""));
        service = new AttendanceService(courseRepository, facultyRepository, studentRepository, assignmentRepository,
                enrollmentRepository, sessionRepository, recordRepository, currentUser, audit, props, CLOCK);

        faculty = new Faculty();
        ReflectionTestUtils.setField(faculty, "id", 5L);
        Course course = new Course();
        ReflectionTestUtils.setField(course, "id", COURSE_ID);

        lenient().when(currentUser.require()).thenReturn(new AppUserDetails(10L, "fac1", "hash", Role.FACULTY, true));
        lenient().when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        lenient().when(facultyRepository.findByUserId(10L)).thenReturn(Optional.of(faculty));
        lenient().when(assignmentRepository.existsByFacultyIdAndCourseId(5L, COURSE_ID)).thenReturn(true);
        lenient().when(enrollmentRepository.findStudentIdsByCourseId(COURSE_ID)).thenReturn(Set.of(100L, 200L));
    }

    private static MarkAttendanceRequest held(LocalDate date, AttendanceEntry... entries) {
        return new MarkAttendanceRequest(date, 1, SessionStatus.HELD, List.of(entries));
    }

    @Test
    @DisplayName("faculty not assigned to the course cannot record attendance")
    void notAssignedFacultyIsDenied() {
        when(assignmentRepository.existsByFacultyIdAndCourseId(5L, COURSE_ID)).thenReturn(false);
        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> service.markAttendance(COURSE_ID,
                held(TODAY, new AttendanceEntry(100L, AttendanceStatus.PRESENT))));
        assertTrue(ex.getMessage().contains("not assigned"));
        verify(recordRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("a user without a faculty profile (e.g. admin) cannot record attendance")
    void nonFacultyIsDenied() {
        when(currentUser.require()).thenReturn(new AppUserDetails(1L, "admin", "hash", Role.ADMIN, true));
        when(facultyRepository.findByUserId(1L)).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> service.markAttendance(COURSE_ID,
                held(TODAY, new AttendanceEntry(100L, AttendanceStatus.PRESENT))));
    }

    @Test
    @DisplayName("future dates are rejected")
    void futureDateRejected() {
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.markAttendance(COURSE_ID,
                held(TODAY.plusDays(1), new AttendanceEntry(100L, AttendanceStatus.PRESENT))));
        assertTrue(ex.getMessage().contains("future"));
    }

    @Test
    @DisplayName("dates older than the edit window are rejected; the boundary day is allowed")
    void editWindowEnforced() {
        assertThrows(BadRequestException.class, () -> service.markAttendance(COURSE_ID,
                held(TODAY.minusDays(8), new AttendanceEntry(100L, AttendanceStatus.PRESENT))));

        when(sessionRepository.findByCourseIdAndSessionDateAndSlot(COURSE_ID, TODAY.minusDays(7), 1))
                .thenReturn(Optional.empty());
        when(sessionRepository.save(any(ClassSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(studentRepository.getReferenceById(100L)).thenReturn(new Student());
        when(recordRepository.findBySessionIdWithStudent(any())).thenReturn(List.of());
        // exactly 7 days ago is still inside the window
        service.markAttendance(COURSE_ID, held(TODAY.minusDays(7), new AttendanceEntry(100L, AttendanceStatus.PRESENT)));
        verify(recordRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("the same student twice in one request is rejected (duplicate prevention)")
    void duplicateStudentInPayloadRejected() {
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.markAttendance(COURSE_ID,
                held(TODAY, new AttendanceEntry(100L, AttendanceStatus.PRESENT),
                        new AttendanceEntry(100L, AttendanceStatus.ABSENT))));
        assertTrue(ex.getMessage().contains("more than once"));
        verify(sessionRepository, never()).save(any(ClassSession.class));
    }

    @Test
    @DisplayName("a student who is not enrolled in the course is rejected")
    void notEnrolledStudentRejected() {
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.markAttendance(COURSE_ID,
                held(TODAY, new AttendanceEntry(999L, AttendanceStatus.PRESENT))));
        assertTrue(ex.getMessage().contains("not enrolled"));
    }

    @Test
    @DisplayName("a held class needs at least one record")
    void heldClassNeedsRecords() {
        assertThrows(BadRequestException.class, () -> service.markAttendance(COURSE_ID, held(TODAY)));
    }

    @Test
    @DisplayName("records are not allowed for cancelled / holiday sessions")
    void cancelledSessionWithRecordsRejected() {
        MarkAttendanceRequest req = new MarkAttendanceRequest(TODAY, 1, SessionStatus.CANCELLED,
                List.of(new AttendanceEntry(100L, AttendanceStatus.PRESENT)));
        assertThrows(BadRequestException.class, () -> service.markAttendance(COURSE_ID, req));
    }

    @Test
    @DisplayName("a cancelled session is saved without records and clears earlier records")
    void cancelledSessionClearsRecords() {
        when(sessionRepository.findByCourseIdAndSessionDateAndSlot(COURSE_ID, TODAY, 1)).thenReturn(Optional.empty());
        when(sessionRepository.save(any(ClassSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(recordRepository.findBySessionIdWithStudent(any())).thenReturn(List.of());

        service.markAttendance(COURSE_ID, new MarkAttendanceRequest(TODAY, 1, SessionStatus.CANCELLED, List.of()));

        verify(recordRepository).deleteAllBySessionId(any());
        verify(recordRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("happy path: one session and one record per student, statuses stored as sent")
    void happyPathCreatesSessionAndRecords() {
        when(sessionRepository.findByCourseIdAndSessionDateAndSlot(COURSE_ID, TODAY, 1)).thenReturn(Optional.empty());
        when(sessionRepository.save(any(ClassSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(recordRepository.findBySessionIdWithStudent(any())).thenReturn(List.of());
        when(studentRepository.getReferenceById(eq(100L))).thenReturn(new Student());
        when(studentRepository.getReferenceById(eq(200L))).thenReturn(new Student());

        service.markAttendance(COURSE_ID, held(TODAY,
                new AttendanceEntry(100L, AttendanceStatus.PRESENT),
                new AttendanceEntry(200L, AttendanceStatus.ABSENT)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AttendanceRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(recordRepository).saveAll(captor.capture());
        List<AttendanceRecord> saved = captor.getValue();
        assertEquals(2, saved.size());
        assertEquals(AttendanceStatus.PRESENT, saved.get(0).getStatus());
        assertEquals(AttendanceStatus.ABSENT, saved.get(1).getStatus());
        assertEquals(10L, saved.get(0).getMarkedBy());
        verify(audit).record(eq("ATTENDANCE_SAVED"), any());
    }

    @Test
    @DisplayName("faculty not assigned to the course cannot view its summary")
    void notAssignedFacultyCannotViewSummary() {
        when(assignmentRepository.existsByFacultyIdAndCourseId(5L, COURSE_ID)).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> service.courseSummary(COURSE_ID));
    }
}
