package com.attendance.service;

import com.attendance.config.AppProperties;
import com.attendance.domain.AttendanceStatus;
import com.attendance.domain.SessionStatus;
import com.attendance.dto.AttendanceDtos.CourseAttendanceSummary;
import com.attendance.dto.AttendanceDtos.HistoryItem;
import com.attendance.dto.AttendanceDtos.StudentSummaryResponse;
import com.attendance.dto.CourseDtos.CourseResponse;
import com.attendance.dto.StudentDtos.StudentResponse;
import com.attendance.entity.AttendanceRecord;
import com.attendance.entity.Course;
import com.attendance.entity.Student;
import com.attendance.repository.AttendanceRecordRepository;
import com.attendance.repository.EnrollmentRepository;
import com.attendance.repository.StudentRepository;
import com.attendance.security.CurrentUserService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Everything a student can see. The student is ALWAYS taken from the authenticated principal -
 * no endpoint accepts a student id, so a student can never read another student's data.
 */
@Service
public class StudentPortalService {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRecordRepository recordRepository;
    private final CurrentUserService currentUser;
    private final AppProperties props;

    public StudentPortalService(StudentRepository studentRepository, EnrollmentRepository enrollmentRepository,
                                AttendanceRecordRepository recordRepository, CurrentUserService currentUser,
                                AppProperties props) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.recordRepository = recordRepository;
        this.currentUser = currentUser;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public StudentResponse profile() {
        return StudentResponse.from(me());
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> courses() {
        return enrollmentRepository.findCoursesByStudentId(me().getId()).stream().map(CourseResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<HistoryItem> history(Long courseId) {
        Long studentId = me().getId();
        List<AttendanceRecord> records = courseId == null
                ? recordRepository.findHistoryForStudent(studentId, SessionStatus.HELD)
                : recordRepository.findHistoryForStudentAndCourse(studentId, courseId, SessionStatus.HELD);
        return records.stream()
                .map(r -> new HistoryItem(r.getSession().getSessionDate(), r.getSession().getSlot(),
                        r.getSession().getCourse().getId(), r.getSession().getCourse().getCode(),
                        r.getSession().getCourse().getName(), r.getStatus()))
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentSummaryResponse summary() {
        Student student = me();
        double threshold = props.attendance().lowThresholdPercent();

        Map<Long, long[]> perCourse = new HashMap<>(); // courseId -> [present, absent]
        for (Object[] row : recordRepository.countByCourseForStudent(student.getId(), SessionStatus.HELD)) {
            long[] c = perCourse.computeIfAbsent((Long) row[0], k -> new long[2]);
            if (row[1] == AttendanceStatus.PRESENT) {
                c[0] += (Long) row[2];
            } else {
                c[1] += (Long) row[2];
            }
        }

        List<CourseAttendanceSummary> rows = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        long totalPresent = 0;
        long totalAll = 0;
        for (Course course : enrollmentRepository.findCoursesByStudentId(student.getId())) {
            long[] c = perCourse.getOrDefault(course.getId(), new long[2]);
            long total = c[0] + c[1];
            Double pct = AttendanceCalculator.percentage(c[0], total);
            boolean low = AttendanceCalculator.isLow(pct, threshold);
            rows.add(new CourseAttendanceSummary(course.getId(), course.getCode(), course.getName(),
                    c[0], c[1], total, pct, low));
            totalPresent += c[0];
            totalAll += total;
            if (low) {
                warnings.add("Low attendance in " + course.getCode() + " - " + course.getName() + ": " + pct
                        + "% (minimum " + threshold + "%)");
            }
        }
        Double overall = AttendanceCalculator.percentage(totalPresent, totalAll);
        if (AttendanceCalculator.isLow(overall, threshold)) {
            warnings.add(0, "Your overall attendance is " + overall + "%, below the required " + threshold + "%");
        }
        return new StudentSummaryResponse(overall, totalPresent, totalAll, threshold, rows, warnings);
    }

    private Student me() {
        Long userId = currentUser.require().getId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("You do not have a student profile"));
    }
}
