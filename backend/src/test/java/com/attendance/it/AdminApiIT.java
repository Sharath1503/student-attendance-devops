package com.attendance.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AdminApiIT extends IntegrationTestBase {

    private String admin;

    @BeforeEach
    void loginAdmin() throws Exception {
        admin = login("admin1");
    }

    private static String studentJson(String code, String username, String email, String password) {
        return """
                {"studentCode":"%s","fullName":"New Student","email":"%s","department":"CA","programme":"MCA",
                 "academicYear":"2025-26","username":"%s","password":"%s"}
                """.formatted(code, email, username, password);
    }

    @Test
    @DisplayName("AC-02: admin creates a student; the response never contains password data and the student can log in")
    void createStudent() throws Exception {
        mvc.perform(withToken(post("/api/admin/students"), admin).contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson("S100", "new.student", "new.student@example.edu", "LongPassw0rd")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.studentCode").value("S100"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new.student\",\"password\":\"LongPassw0rd\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("STUDENT"));
    }

    @Test
    @DisplayName("duplicate student ID, e-mail or username are rejected with 409")
    void duplicateStudentsRejected() throws Exception {
        mvc.perform(withToken(post("/api/admin/students"), admin).contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson("S1", "other", "other@example.edu", "LongPassw0rd")))
                .andExpect(status().isConflict());
        mvc.perform(withToken(post("/api/admin/students"), admin).contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson("S200", "other", "stu1@example.edu", "LongPassw0rd")))
                .andExpect(status().isConflict());
        mvc.perform(withToken(post("/api/admin/students"), admin).contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson("S201", "stu1", "other2@example.edu", "LongPassw0rd")))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("invalid student input is rejected by server-side validation (400 + field errors)")
    void invalidStudentRejected() throws Exception {
        mvc.perform(withToken(post("/api/admin/students"), admin).contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson("S300", "ok.name", "not-an-email", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    @DisplayName("student list is paginated and searchable")
    void studentListIsPaginated() throws Exception {
        mvc.perform(withToken(get("/api/admin/students").param("size", "2").param("page", "0"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(withToken(get("/api/admin/students").param("q", "stu2"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].studentCode").value("S2"));
    }

    @Test
    @DisplayName("updating a student works; deactivating also blocks the login")
    void updateAndDeactivateStudent() throws Exception {
        mvc.perform(withToken(put("/api/admin/students/" + stu1.getId()), admin).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Renamed","email":"stu1@example.edu","department":"CA","programme":"MCA",
                                 "academicYear":"2026-27","active":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Renamed"))
                .andExpect(jsonPath("$.academicYear").value("2026-27"));

        mvc.perform(withToken(delete("/api/admin/students/" + stu1.getId()), admin)).andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"stu1\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(withToken(get("/api/admin/students/99999"), admin)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("faculty accounts: create, duplicate rejected")
    void facultyManagement() throws Exception {
        String body = """
                {"facultyCode":"F100","fullName":"New Faculty","email":"nf@example.edu","department":"CA",
                 "username":"new.faculty","password":"LongPassw0rd"}
                """;
        mvc.perform(withToken(post("/api/admin/faculty"), admin).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.facultyCode").value("F100"));
        mvc.perform(withToken(post("/api/admin/faculty"), admin).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("courses: create, duplicate code rejected, assign faculty, enrol students, duplicates rejected")
    void courseManagementAndAssociations() throws Exception {
        String course = "{\"code\":\"cs900\",\"name\":\"Cloud\",\"department\":\"CA\",\"active\":true}";
        String created = mvc.perform(withToken(post("/api/admin/courses"), admin).contentType(MediaType.APPLICATION_JSON).content(course))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CS900"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();

        mvc.perform(withToken(post("/api/admin/courses"), admin).contentType(MediaType.APPLICATION_JSON).content(course))
                .andExpect(status().isConflict());

        mvc.perform(withToken(post("/api/admin/courses/" + id + "/faculty/" + fac1.getId()), admin)).andExpect(status().isNoContent());
        mvc.perform(withToken(post("/api/admin/courses/" + id + "/faculty/" + fac1.getId()), admin)).andExpect(status().isConflict());
        mvc.perform(withToken(post("/api/admin/courses/" + id + "/students/" + stu3.getId()), admin)).andExpect(status().isNoContent());
        mvc.perform(withToken(post("/api/admin/courses/" + id + "/students/" + stu3.getId()), admin)).andExpect(status().isConflict());

        mvc.perform(withToken(get("/api/admin/courses/" + id), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faculty.length()").value(1))
                .andExpect(jsonPath("$.students.length()").value(1));

        mvc.perform(withToken(delete("/api/admin/courses/" + id + "/students/" + stu3.getId()), admin)).andExpect(status().isNoContent());
        mvc.perform(withToken(delete("/api/admin/courses/" + id + "/faculty/" + fac1.getId()), admin)).andExpect(status().isNoContent());
        mvc.perform(withToken(post("/api/admin/courses/" + id + "/students/99999"), admin)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("dashboard statistics come from the database")
    void dashboardCounts() throws Exception {
        mvc.perform(withToken(get("/api/admin/dashboard"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudents").value(3))
                .andExpect(jsonPath("$.totalFaculty").value(2))
                .andExpect(jsonPath("$.totalCourses").value(2))
                .andExpect(jsonPath("$.totalHeldSessions").value(0));
        assertEquals(3, studentRepository.count());
    }

    @Test
    @DisplayName("unknown ids return a consistent JSON 404")
    void notFoundIsJson() throws Exception {
        mvc.perform(withToken(get("/api/admin/courses/424242"), admin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
