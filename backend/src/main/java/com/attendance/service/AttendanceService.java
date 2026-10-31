package com.attendance.service;

import com.attendance.config.AppProperties;
import com.attendance.domain.AttendanceStatus;
import com.attendance.domain.Role;
import com.attendance.domain.SessionStatus;
import com.attendance.dto.AttendanceDtos.AttendanceEntry;
import com.attendance.dto.AttendanceDtos.CourseSummaryResponse;
import com.attendance.dto.AttendanceDtos.MarkAttendanceRequest;
import com.attendance.dto.AttendanceDtos.RecordResponse;
import com.attendance.dto.AttendanceDtos.SessionDetailResponse;
import com.attendance.dto.AttendanceDtos.SessionResponse;
import com.attendance.dto.AttendanceDtos.StudentCourseSummary;
import com.attendance.dto.StudentDtos.StudentResponse;
import com.attendance.entity.AttendanceRecord;
import com.attendance.entity.ClassSession;
import com.attendance.entity.Course;
import com.attendance.entity.Faculty;
import com.attendance.entity.Student;
import com.attendance.exception.BadRequestException;
import com.attendance.exception.ResourceNotFoundException;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core attendance rules:
 * <ul>
 *   <li>only FACULTY assigned to the course may record or change attendance</li>
 *   <li>ADMIN and assigned FACULTY may view; students use StudentPortalService (own data only)</li>
 *   <li>no future dates; changes only within the configured edit window</li>
 *   <li>(course, date, slot) identifies a session and (session, student) a record - both unique in the database,
 *       so re-submitting updates instead of duplicating</li>
 * </ul>
 */
@Service
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    private final CourseRepository courseRepository;
    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;
    private final CourseAssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final CurrentUserService currentUser;
    private final AuditService audit;
    private final AppProperties props;
    private final Clock clock;

    public AttendanceService(CourseRepository courseRepository, FacultyRepository facultyRepository,
                             StudentRepository studentRepository,
                             CourseAssignmentRepository assignmentRepository, EnrollmentRepository enrollmentRepository,
                             ClassSessionRepository sessionRepository, AttendanceRecordRepository recordRepository,
                             CurrentUserService currentUser, AuditService audit, AppProperties props, Clock clock) {
        this.courseRepository = courseRepository;
        this.facultyRepository = facultyRepository;
        this.studentRepository = studentRepository;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.currentUser = currentUser;
        this.audit = audit;
        this.props = props;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ write

    @Transactional
    public SessionDetailResponse markAttendance(Long courseId, MarkAttendanceRequest req) {
        AppUserDetails user = currentUser.require();
        Course course = findCourse(courseId);
        Faculty faculty = facultyRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AccessDeniedException("You are not allowed to record attendance"));
        if (!assignmentRepository.existsByFacultyIdAndCourseId(faculty.getId(), courseId)) {
            throw new AccessDeniedException("You are not assigned to this course");
        }
        if (!course.isActive()) {
            throw new BadRequestException("Attendance cannot be recorded for an inactive course");
        }

        LocalDate today = LocalDate.now(clock);
        if (req.date().isAfter(today)) {
            throw new BadRequestException("Attendance cannot be recorded for a future date");
        }
        int window = props.attendance().editWindowDays();
        if (req.date().isBefore(today.minusDays(window))) {
            throw new BadRequestException("Attendance can only be recorded or changed within " + window
                    + " days of the class date");
        }

        int slot = req.slot() == null ? 1 : req.slot();
        List<AttendanceEntry> entries = req.records() == null ? List.of() : req.records();
        Set<Long> enrolledIds = enrollmentRepository.findStudentIdsByCourseId(courseId);

        if (req.status() == SessionStatus.HELD) {
            validateEntries(entries, enrolledIds);
        } else if (!entries.isEmpty()) {
            throw new BadRequestException("Records must not be supplied for a " + req.status() + " session");
        }

        ClassSession session = sessionRepository.findByCourseIdAndSessionDateAndSlot(courseId, req.date(), slot)
                .orElseGet(() -> {
                    ClassSession s = new ClassSession();
                    s.setCourse(course);
                    s.setSessionDate(req.date());
                    s.setSlot(slot);
                    s.setCreatedBy(user.getId());
                    return s;
                });
        session.setStatus(req.status());
        session = sessionRepository.save(session);

        if (req.status() == SessionStatus.HELD) {
            upsertRecords(session, entries, user.getId());
        } else {
            recordRepository.deleteAllBySessionId(session.getId());
        }

        audit.record("ATTENDANCE_SAVED", "course=" + course.getCode() + ", date=" + req.date()
                + ", slot=" + slot + ", status=" + req.status());
        log.info("Attendance saved by '{}' for course {} on {} slot {} ({})",
                user.getUsername(), course.getCode(), req.date(), slot, req.status());
        return toDetail(session);
    }

    /** Package-visible for unit tests: rejects duplicates in the payload and students not enrolled. */
    void validateEntries(List<AttendanceEntry> entries, Set<Long> enrolledIds) {
        if (entries.isEmpty()) {
            throw new BadRequestException("At least one attendance record is required for a held class");
        }
        Set<Long> seen = new HashSet<>();
        for (AttendanceEntry e : entries) {
            if (!seen.add(e.studentId())) {
                throw new BadRequestException("Student " + e.studentId() + " appears more than once in the request");
            }
            if (!enrolledIds.contains(e.studentId())) {
                throw new BadRequestException("Student " + e.studentId() + " is not enrolled in this course");
            }
        }
    }

    private void upsertRecords(ClassSession session, List<AttendanceEntry> entries, Long markedBy) {
        Map<Long, AttendanceRecord> existing = new HashMap<>();
        for (AttendanceRecord r : recordRepository.findBySessionIdWithStudent(session.getId())) {
            existing.put(r.getStudent().getId(), r);
        }
        List<AttendanceRecord> toSave = new ArrayList<>();
        for (AttendanceEntry e : entries) {
            AttendanceRecord r = existing.get(e.studentId());
            if (r == null) {
                r = new AttendanceRecord();
                r.setSession(session);
                // getReferenceById avoids loading the student; enrolment was validated above.
                r.setStudent(studentRepository.getReferenceById(e.studentId()));
            }
            r.setStatus(e.status());
            r.setMarkedBy(markedBy);
            toSave.add(r);
        }
        recordRepository.saveAll(toSave);
    }

    // ------------------------------------------------------------------ read

    @Transactional(readOnly = true)
    public List<StudentResponse> roster(Long courseId) {
        assertCanView(courseId);
        return enrollmentRepository.findStudentsByCourseId(courseId).stream().map(StudentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> sessions(Long courseId) {
        assertCanView(courseId);
        Course course = findCourse(courseId);
        List<ClassSession> sessions = sessionRepository.findByCourseIdOrderBySessionDateDescSlotDesc(courseId);
        if (sessions.isEmpty()) {
            return List.of();
        }
        Map<Long, long[]> counts = countsBySession(sessions.stream().map(ClassSession::getId).toList());
        return sessions.stream().map(s -> toSessionResponse(s, course, counts.get(s.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public SessionDetailResponse sessionDetail(Long courseId, Long sessionId) {
        assertCanView(courseId);
        ClassSession session = sessionRepository.findById(sessionId)
                .filter(s -> s.getCourse().getId().equals(courseId))
                .orElseThrow(() -> new ResourceNotFoundException("Session " + sessionId + " not found for this course"));
        return toDetail(session);
    }

    @Transactional(readOnly = true)
    public CourseSummaryResponse courseSummary(Long courseId) {
        assertCanView(courseId);
        Course course = findCourse(courseId);
        double threshold = props.attendance().lowThresholdPercent();

        Map<Long, long[]> perStudent = new HashMap<>(); // [present, absent]
        for (Object[] row : recordRepository.countByStudentForCourse(courseId, SessionStatus.HELD)) {
            long[] c = perStudent.computeIfAbsent((Long) row[0], k -> new long[2]);
            if (row[1] == AttendanceStatus.PRESENT) {
                c[0] += (Long) row[2];
            } else {
                c[1] += (Long) row[2];
            }
        }
        long held = sessionRepository.findByCourseIdOrderBySessionDateDescSlotDesc(courseId).stream()
                .filter(s -> s.getStatus() == SessionStatus.HELD).count();

        List<StudentCourseSummary> rows = new ArrayList<>();
        for (Student s : enrollmentRepository.findStudentsByCourseId(courseId)) {
            long[] c = perStudent.getOrDefault(s.getId(), new long[2]);
            long total = c[0] + c[1];
            Double pct = AttendanceCalculator.percentage(c[0], total);
            rows.add(new StudentCourseSummary(s.getId(), s.getStudentCode(), s.getFullName(), c[0], c[1], total, pct,
                    AttendanceCalculator.isLow(pct, threshold)));
        }
        return new CourseSummaryResponse(course.getId(), course.getCode(), course.getName(), held, threshold, rows);
    }

    // ------------------------------------------------------------------ helpers

    /** ADMIN may view any course; FACULTY only courses they are assigned to; everyone else is refused. */
    void assertCanView(Long courseId) {
        AppUserDetails user = currentUser.require();
        findCourse(courseId);
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (user.getRole() == Role.FACULTY) {
            Faculty f = facultyRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new AccessDeniedException("You do not have a faculty profile"));
            if (assignmentRepository.existsByFacultyIdAndCourseId(f.getId(), courseId)) {
                return;
            }
            throw new AccessDeniedException("You are not assigned to this course");
        }
        throw new AccessDeniedException("You do not have permission to view this course's attendance");
    }

    private Course findCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course " + id + " not found"));
    }

    private SessionDetailResponse toDetail(ClassSession session) {
        List<AttendanceRecord> records = recordRepository.findBySessionIdWithStudent(session.getId());
        long present = records.stream().filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count();
        SessionResponse sr = toSessionResponse(session, session.getCourse(), new long[] {present, records.size() - present});
        List<RecordResponse> rows = records.stream()
                .map(r -> new RecordResponse(r.getStudent().getId(), r.getStudent().getStudentCode(),
                        r.getStudent().getFullName(), r.getStatus()))
                .toList();
        return new SessionDetailResponse(sr, rows);
    }

    private SessionResponse toSessionResponse(ClassSession s, Course course, long[] counts) {
        long[] c = counts == null ? new long[2] : counts;
        return new SessionResponse(s.getId(), course.getId(), course.getCode(), s.getSessionDate(), s.getSlot(),
                s.getStatus(), c[0], c[1]);
    }

    private Map<Long, long[]> countsBySession(Collection<Long> sessionIds) {
        Map<Long, long[]> result = new HashMap<>();
        for (Object[] row : recordRepository.countBySessionIds(sessionIds)) {
            long[] c = result.computeIfAbsent((Long) row[0], k -> new long[2]);
            if (row[1] == AttendanceStatus.PRESENT) {
                c[0] += (Long) row[2];
            } else {
                c[1] += (Long) row[2];
            }
        }
        return result;
    }
}
