package com.attendance.bootstrap;

import com.attendance.config.AppProperties;
import com.attendance.domain.AttendanceStatus;
import com.attendance.domain.Role;
import com.attendance.domain.SessionStatus;
import com.attendance.entity.AttendanceRecord;
import com.attendance.entity.ClassSession;
import com.attendance.entity.Course;
import com.attendance.entity.CourseAssignment;
import com.attendance.entity.Enrollment;
import com.attendance.entity.Faculty;
import com.attendance.entity.Student;
import com.attendance.entity.User;
import com.attendance.repository.AttendanceRecordRepository;
import com.attendance.repository.ClassSessionRepository;
import com.attendance.repository.CourseAssignmentRepository;
import com.attendance.repository.CourseRepository;
import com.attendance.repository.EnrollmentRepository;
import com.attendance.repository.FacultyRepository;
import com.attendance.repository.StudentRepository;
import com.attendance.repository.UserRepository;
import com.attendance.service.UserAccountService;
import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first administrator and (optionally) demo data at start-up.
 * Passwords always come from environment variables; nothing is hard-coded and no password is ever logged.
 * Both actions are idempotent: they do nothing when the data already exists.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AppProperties props;
    private final UserRepository userRepository;
    private final UserAccountService accounts;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseAssignmentRepository assignmentRepository;
    private final ClassSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final Clock clock;

    public DataInitializer(AppProperties props, UserRepository userRepository, UserAccountService accounts,
                           StudentRepository studentRepository, FacultyRepository facultyRepository,
                           CourseRepository courseRepository, EnrollmentRepository enrollmentRepository,
                           CourseAssignmentRepository assignmentRepository, ClassSessionRepository sessionRepository,
                           AttendanceRecordRepository recordRepository, Clock clock) {
        this.props = props;
        this.userRepository = userRepository;
        this.accounts = accounts;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createAdminIfRequested();
        createDemoDataIfRequested();
    }

    private void createAdminIfRequested() {
        String username = props.bootstrap().adminUsername();
        String password = props.bootstrap().adminPassword();
        if (password == null || password.isBlank()) {
            log.info("BOOTSTRAP_ADMIN_PASSWORD not set - skipping initial administrator creation");
            return;
        }
        if (password.length() < 8) {
            log.warn("BOOTSTRAP_ADMIN_PASSWORD is shorter than 8 characters - administrator NOT created");
            return;
        }
        if (userRepository.existsByUsername(username)) {
            log.info("Administrator '{}' already exists", username);
            return;
        }
        accounts.create(username, password, Role.ADMIN);
        log.info("Initial administrator '{}' created", username);
    }

    private void createDemoDataIfRequested() {
        if (!props.seed().demoEnabled()) {
            return;
        }
        String password = props.seed().demoPassword();
        if (password == null || password.length() < 8) {
            log.warn("SEED_DEMO_DATA=true but SEED_DEMO_PASSWORD is missing or shorter than 8 characters - demo data NOT created");
            return;
        }
        if (userRepository.existsByUsername("faculty1")) {
            log.info("Demo data already present");
            return;
        }

        Faculty faculty = newFaculty("faculty1", "FAC001", "Dr. Anita Rao", "anita.rao@example.edu", "Computer Applications", password);
        Student s1 = newStudent("student1", "MCA24001", "Ravi Kumar", "ravi.kumar@example.edu", password);
        Student s2 = newStudent("student2", "MCA24002", "Sneha Patel", "sneha.patel@example.edu", password);
        Student s3 = newStudent("student3", "MCA24003", "Arjun Mehta", "arjun.mehta@example.edu", password);

        Course devops = newCourse("MCA501", "DevOps and Cloud Computing");
        Course java = newCourse("MCA502", "Advanced Java Programming");
        assignmentRepository.save(new CourseAssignment(faculty, devops));
        assignmentRepository.save(new CourseAssignment(faculty, java));
        for (Student s : new Student[] {s1, s2, s3}) {
            enrollmentRepository.save(new Enrollment(s, devops));
            enrollmentRepository.save(new Enrollment(s, java));
        }

        // Sample history for the last few days so dashboards and percentages have something to show.
        LocalDate today = LocalDate.now(clock);
        for (int daysAgo = 1; daysAgo <= 5; daysAgo++) {
            LocalDate d = today.minusDays(daysAgo);
            for (Course c : new Course[] {devops, java}) {
                ClassSession session = new ClassSession();
                session.setCourse(c);
                session.setSessionDate(d);
                session.setSlot(1);
                session.setStatus(SessionStatus.HELD);
                session.setCreatedBy(faculty.getUser().getId());
                sessionRepository.save(session);
                mark(session, s1, AttendanceStatus.PRESENT, faculty);
                mark(session, s2, daysAgo % 2 == 0 ? AttendanceStatus.ABSENT : AttendanceStatus.PRESENT, faculty);
                mark(session, s3, daysAgo <= 2 ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT, faculty);
            }
        }
        log.info("Demo data created (faculty1, student1..student3; password from SEED_DEMO_PASSWORD)");
    }

    private Faculty newFaculty(String username, String code, String name, String email, String dept, String password) {
        User user = accounts.create(username, password, Role.FACULTY);
        Faculty f = new Faculty();
        f.setUser(user);
        f.setFacultyCode(code);
        f.setFullName(name);
        f.setEmail(email);
        f.setDepartment(dept);
        return facultyRepository.save(f);
    }

    private Student newStudent(String username, String code, String name, String email, String password) {
        User user = accounts.create(username, password, Role.STUDENT);
        Student s = new Student();
        s.setUser(user);
        s.setStudentCode(code);
        s.setFullName(name);
        s.setEmail(email);
        s.setDepartment("Computer Applications");
        s.setProgramme("MCA");
        s.setAcademicYear("2025-26");
        return studentRepository.save(s);
    }

    private Course newCourse(String code, String name) {
        Course c = new Course();
        c.setCode(code);
        c.setName(name);
        c.setDepartment("Computer Applications");
        return courseRepository.save(c);
    }

    private void mark(ClassSession session, Student student, AttendanceStatus status, Faculty faculty) {
        AttendanceRecord r = new AttendanceRecord();
        r.setSession(session);
        r.setStudent(student);
        r.setStatus(status);
        r.setMarkedBy(faculty.getUser().getId());
        recordRepository.save(r);
    }
}
