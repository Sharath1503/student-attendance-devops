package com.attendance.it;

import com.attendance.domain.Role;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the full application on an in-memory H2 database (MySQL mode, real Flyway migrations) and creates a small
 * world for every test. Each test runs in a transaction that is rolled back, so tests are independent.
 *
 * <pre>
 *   admin1                       ADMIN
 *   fac1  -> courseA             FACULTY (assigned to courseA only)
 *   fac2  -> courseB             FACULTY (assigned to courseB only)
 *   stu1, stu2 enrolled in A     STUDENT
 *   stu3 enrolled in B           STUDENT
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTestBase {

    protected static final String PASSWORD = "Passw0rd!test";

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected PasswordEncoder encoder;
    @Autowired protected UserRepository userRepository;
    @Autowired protected StudentRepository studentRepository;
    @Autowired protected FacultyRepository facultyRepository;
    @Autowired protected CourseRepository courseRepository;
    @Autowired protected EnrollmentRepository enrollmentRepository;
    @Autowired protected CourseAssignmentRepository assignmentRepository;
    @Autowired protected ClassSessionRepository sessionRepository;
    @Autowired protected AttendanceRecordRepository recordRepository;

    protected Course courseA;
    protected Course courseB;
    protected Student stu1;
    protected Student stu2;
    protected Student stu3;
    protected Faculty fac1;
    protected Faculty fac2;

    @BeforeEach
    void createFixtures() {
        newUser("admin1", Role.ADMIN, true);
        fac1 = newFaculty("fac1", "F1");
        fac2 = newFaculty("fac2", "F2");
        stu1 = newStudent("stu1", "S1");
        stu2 = newStudent("stu2", "S2");
        stu3 = newStudent("stu3", "S3");
        courseA = newCourse("CS101", "Algorithms");
        courseB = newCourse("CS102", "Databases");
        assignmentRepository.save(new CourseAssignment(fac1, courseA));
        assignmentRepository.save(new CourseAssignment(fac2, courseB));
        enrollmentRepository.save(new Enrollment(stu1, courseA));
        enrollmentRepository.save(new Enrollment(stu2, courseA));
        enrollmentRepository.save(new Enrollment(stu3, courseB));
    }

    protected User newUser(String username, Role role, boolean enabled) {
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(PASSWORD));
        u.setRole(role);
        u.setEnabled(enabled);
        return userRepository.save(u);
    }

    private Faculty newFaculty(String username, String code) {
        Faculty f = new Faculty();
        f.setUser(newUser(username, Role.FACULTY, true));
        f.setFacultyCode(code);
        f.setFullName("Faculty " + username);
        f.setEmail(username + "@example.edu");
        f.setDepartment("CA");
        return facultyRepository.save(f);
    }

    private Student newStudent(String username, String code) {
        Student s = new Student();
        s.setUser(newUser(username, Role.STUDENT, true));
        s.setStudentCode(code);
        s.setFullName("Student " + username);
        s.setEmail(username + "@example.edu");
        s.setDepartment("CA");
        s.setProgramme("MCA");
        s.setAcademicYear("2025-26");
        return studentRepository.save(s);
    }

    private Course newCourse(String code, String name) {
        Course c = new Course();
        c.setCode(code);
        c.setName(name);
        c.setDepartment("CA");
        return courseRepository.save(c);
    }

    /** Logs in through the real endpoint and returns the JWT. */
    protected String login(String username) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", bearer(token));
    }
}
