package com.attendance.service;

import com.attendance.config.AppProperties;
import com.attendance.domain.AttendanceStatus;
import com.attendance.domain.SessionStatus;
import com.attendance.dto.CommonDtos.AdminDashboardResponse;
import com.attendance.dto.CommonDtos.AuditResponse;
import com.attendance.dto.CommonDtos.PageResponse;
import com.attendance.repository.AttendanceRecordRepository;
import com.attendance.repository.AuditRecordRepository;
import com.attendance.repository.ClassSessionRepository;
import com.attendance.repository.CourseRepository;
import com.attendance.repository.FacultyRepository;
import com.attendance.repository.StudentRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Live statistics for the administrator dashboard (computed from the database on every call). */
@Service
public class DashboardService {

    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final CourseRepository courseRepository;
    private final ClassSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final AuditRecordRepository auditRepository;
    private final AppProperties props;
    private final Clock clock;

    public DashboardService(StudentRepository studentRepository, FacultyRepository facultyRepository,
                            CourseRepository courseRepository, ClassSessionRepository sessionRepository,
                            AttendanceRecordRepository recordRepository, AuditRecordRepository auditRepository,
                            AppProperties props, Clock clock) {
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.courseRepository = courseRepository;
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.auditRepository = auditRepository;
        this.props = props;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse adminDashboard() {
        double threshold = props.attendance().lowThresholdPercent();
        Map<Long, long[]> perStudent = new HashMap<>();
        long present = 0;
        long total = 0;
        for (Object[] row : recordRepository.countAllByStudent(SessionStatus.HELD)) {
            long[] c = perStudent.computeIfAbsent((Long) row[0], k -> new long[2]);
            long n = (Long) row[2];
            if (row[1] == AttendanceStatus.PRESENT) {
                c[0] += n;
                present += n;
            } else {
                c[1] += n;
            }
            total += n;
        }
        long low = perStudent.values().stream()
                .filter(c -> AttendanceCalculator.isLow(AttendanceCalculator.percentage(c[0], c[0] + c[1]), threshold))
                .count();
        LocalDate weekAgo = LocalDate.now(clock).minusDays(7);
        return new AdminDashboardResponse(
                studentRepository.count(), facultyRepository.count(), courseRepository.count(),
                sessionRepository.countByStatus(SessionStatus.HELD),
                sessionRepository.countByStatusAndSessionDateGreaterThanEqual(SessionStatus.HELD, weekAgo),
                AttendanceCalculator.percentage(present, total), low, threshold);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditResponse> audit(int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.from(auditRepository.findAll(pageable)
                .map(a -> new AuditResponse(a.getId(), a.getUsername(), a.getAction(), a.getDetails(), a.getCreatedAt())));
    }
}
