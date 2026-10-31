package com.attendance.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class AttendanceFlowIT extends IntegrationTestBase {

    private String fac1Token;
    private String fac2Token;
    private String admin;
    private String stu1Token;
    private String stu2Token;

    @BeforeEach
    void tokens() throws Exception {
        fac1Token = login("fac1");
        fac2Token = login("fac2");
        admin = login("admin1");
        stu1Token = login("stu1");
        stu2Token = login("stu2");
    }

    private String sessionsUrl(Long courseId) {
        return "/api/attendance/courses/" + courseId + "/sessions";
    }

    private String held(LocalDate date, int slot, String s1, String s2) {
        return """
                {"date":"%s","slot":%d,"status":"HELD","records":[
                  {"studentId":%d,"status":"%s"},{"studentId":%d,"status":"%s"}]}
                """.formatted(date, slot, stu1.getId(), s1, stu2.getId(), s2);
    }

    private ResultActions save(String token, Long courseId, String body) throws Exception {
        return mvc.perform(withToken(post(sessionsUrl(courseId)), token).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    @DisplayName("AC-03: assigned faculty records attendance and can read it back")
    void facultyMarksAttendance() throws Exception {
        save(fac1Token, courseA.getId(), held(LocalDate.now(), 1, "PRESENT", "ABSENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.status").value("HELD"))
                .andExpect(jsonPath("$.session.presentCount").value(1))
                .andExpect(jsonPath("$.session.absentCount").value(1))
                .andExpect(jsonPath("$.records.length()").value(2));

        mvc.perform(withToken(get(sessionsUrl(courseA.getId())), fac1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("submitting the same class twice updates it; it never creates duplicate sessions or records")
    void resubmitUpdatesInsteadOfDuplicating() throws Exception {
        LocalDate today = LocalDate.now();
        save(fac1Token, courseA.getId(), held(today, 1, "PRESENT", "PRESENT")).andExpect(status().isOk());
        save(fac1Token, courseA.getId(), held(today, 1, "ABSENT", "PRESENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.presentCount").value(1))
                .andExpect(jsonPath("$.session.absentCount").value(1));

        assertEquals(1, sessionRepository.count());
        assertEquals(2, recordRepository.count());
        // a different slot on the same day is a separate class
        save(fac1Token, courseA.getId(), held(today, 2, "PRESENT", "PRESENT")).andExpect(status().isOk());
        assertEquals(2, sessionRepository.count());
        assertEquals(4, recordRepository.count());
    }

    @Test
    @DisplayName("faculty cannot record or view attendance for a course they are not assigned to")
    void unassignedFacultyIsForbidden() throws Exception {
        save(fac2Token, courseA.getId(), held(LocalDate.now(), 1, "PRESENT", "PRESENT"))
                .andExpect(status().isForbidden());
        mvc.perform(withToken(get("/api/attendance/courses/" + courseA.getId() + "/summary"), fac2Token))
                .andExpect(status().isForbidden());
        mvc.perform(withToken(get("/api/attendance/courses/" + courseA.getId() + "/students"), fac2Token))
                .andExpect(status().isForbidden());
        assertEquals(0, sessionRepository.count());
    }

    @Test
    @DisplayName("students and admins cannot record attendance")
    void onlyFacultyMayRecord() throws Exception {
        save(stu1Token, courseA.getId(), held(LocalDate.now(), 1, "PRESENT", "PRESENT")).andExpect(status().isForbidden());
        save(admin, courseA.getId(), held(LocalDate.now(), 1, "PRESENT", "PRESENT")).andExpect(status().isForbidden());
        assertEquals(0, sessionRepository.count());
    }

    @Test
    @DisplayName("input rules: not-enrolled student, duplicate student, future date, outside edit window, bad status")
    void inputRules() throws Exception {
        LocalDate today = LocalDate.now();
        save(fac1Token, courseA.getId(), """
                {"date":"%s","slot":1,"status":"HELD","records":[{"studentId":%d,"status":"PRESENT"}]}
                """.formatted(today, stu3.getId())).andExpect(status().isBadRequest());

        save(fac1Token, courseA.getId(), """
                {"date":"%s","slot":1,"status":"HELD","records":[
                  {"studentId":%d,"status":"PRESENT"},{"studentId":%d,"status":"ABSENT"}]}
                """.formatted(today, stu1.getId(), stu1.getId())).andExpect(status().isBadRequest());

        save(fac1Token, courseA.getId(), held(today.plusDays(1), 1, "PRESENT", "PRESENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Attendance cannot be recorded for a future date"));

        save(fac1Token, courseA.getId(), held(today.minusDays(30), 1, "PRESENT", "PRESENT"))
                .andExpect(status().isBadRequest());

        save(fac1Token, courseA.getId(), """
                {"date":"%s","slot":1,"status":"HELD","records":[{"studentId":%d,"status":"LATE"}]}
                """.formatted(today, stu1.getId())).andExpect(status().isBadRequest());

        save(fac1Token, courseA.getId(), "{\"status\":\"HELD\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.date").exists());

        assertEquals(0, sessionRepository.count());
    }

    @Test
    @DisplayName("AC-04: percentages count HELD sessions only; cancelled classes are excluded; low attendance is flagged")
    void studentSeesCorrectPercentages() throws Exception {
        LocalDate today = LocalDate.now();
        save(fac1Token, courseA.getId(), held(today.minusDays(1), 1, "PRESENT", "PRESENT")).andExpect(status().isOk());
        save(fac1Token, courseA.getId(), held(today.minusDays(2), 1, "PRESENT", "ABSENT")).andExpect(status().isOk());
        save(fac1Token, courseA.getId(), held(today.minusDays(3), 1, "ABSENT", "ABSENT")).andExpect(status().isOk());
        save(fac1Token, courseA.getId(), "{\"date\":\"" + today.minusDays(4) + "\",\"slot\":1,\"status\":\"CANCELLED\"}")
                .andExpect(status().isOk());
        save(fac1Token, courseA.getId(), "{\"date\":\"" + today.minusDays(5) + "\",\"slot\":1,\"status\":\"HOLIDAY\"}")
                .andExpect(status().isOk());

        // stu1: P, P, A = 2/3 -> 66.67 (below 75 -> low)
        mvc.perform(withToken(get("/api/student/summary"), stu1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallPercentage").value(66.67))
                .andExpect(jsonPath("$.overallPresent").value(2))
                .andExpect(jsonPath("$.overallTotal").value(3))
                .andExpect(jsonPath("$.courses.length()").value(1))
                .andExpect(jsonPath("$.courses[0].percentage").value(66.67))
                .andExpect(jsonPath("$.courses[0].low").value(true))
                .andExpect(jsonPath("$.warnings.length()").value(2));

        // stu2: P, A, A = 1/3 -> 33.33
        mvc.perform(withToken(get("/api/student/summary"), stu2Token))
                .andExpect(jsonPath("$.overallPercentage").value(33.33));

        // history: own records only, HELD sessions only, newest first
        mvc.perform(withToken(get("/api/student/attendance"), stu1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].date").value(today.minusDays(1).toString()))
                .andExpect(jsonPath("$[0].status").value("PRESENT"))
                .andExpect(jsonPath("$[2].status").value("ABSENT"));

        // course summary seen by faculty / admin
        mvc.perform(withToken(get("/api/attendance/courses/" + courseA.getId() + "/summary"), fac1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heldSessions").value(3))
                .andExpect(jsonPath("$.students.length()").value(2));
        mvc.perform(withToken(get("/api/attendance/courses/" + courseA.getId() + "/summary"), admin))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("a student with no recorded classes has no percentage (not 0 and not 100) and no warning")
    void noClassesMeansNoPercentage() throws Exception {
        mvc.perform(withToken(get("/api/student/summary"), stu1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallPercentage").doesNotExist())
                .andExpect(jsonPath("$.courses[0].total").value(0))
                .andExpect(jsonPath("$.courses[0].percentage").doesNotExist())
                .andExpect(jsonPath("$.warnings.length()").value(0));
    }

    @Test
    @DisplayName("students only ever see their own data: each student's history contains only their own rows")
    void studentsAreIsolated() throws Exception {
        save(fac1Token, courseA.getId(), held(LocalDate.now(), 1, "PRESENT", "ABSENT")).andExpect(status().isOk());
        mvc.perform(withToken(get("/api/student/attendance"), stu1Token))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PRESENT"));
        mvc.perform(withToken(get("/api/student/attendance"), stu2Token))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("ABSENT"));
        // stu3 is enrolled elsewhere and sees nothing of course A
        String stu3Token = login("stu3");
        mvc.perform(withToken(get("/api/student/attendance"), stu3Token))
                .andExpect(jsonPath("$.length()").value(0));
        // students cannot use the staff endpoints at all
        mvc.perform(withToken(get(sessionsUrl(courseA.getId())), stu1Token)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("changing a held class to cancelled removes its records and drops it from percentages")
    void heldClassCanBeCancelled() throws Exception {
        LocalDate today = LocalDate.now();
        save(fac1Token, courseA.getId(), held(today, 1, "ABSENT", "ABSENT")).andExpect(status().isOk());
        save(fac1Token, courseA.getId(), "{\"date\":\"" + today + "\",\"slot\":1,\"status\":\"CANCELLED\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.status").value("CANCELLED"))
                .andExpect(jsonPath("$.records.length()").value(0));
        assertEquals(0, recordRepository.count());
        mvc.perform(withToken(get("/api/student/summary"), stu1Token))
                .andExpect(jsonPath("$.overallPercentage").doesNotExist());
    }

    @Test
    @DisplayName("faculty sees the roster of their own course")
    void rosterForAssignedFaculty() throws Exception {
        mvc.perform(withToken(get("/api/attendance/courses/" + courseA.getId() + "/students"), fac1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(withToken(get("/api/faculty/me/courses"), fac1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("CS101"));
    }
}
