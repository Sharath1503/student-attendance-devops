package com.attendance.it;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.attendance.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthIT extends IntegrationTestBase {

    private static String loginBody(String u, String p) {
        return "{\"username\":\"" + u + "\",\"password\":\"" + p + "\"}";
    }

    @Test
    @DisplayName("AC-01: valid credentials return a bearer token and the user's role")
    void loginSucceeds() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody("fac1", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.role").value("FACULTY"))
                .andExpect(jsonPath("$.user.displayName").value("Faculty fac1"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("wrong password and unknown user give the same 401 message")
    void invalidCredentialsReturn401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody("fac1", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody("nobody", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("a disabled account cannot log in")
    void disabledUserCannotLogin() throws Exception {
        User u = userRepository.findByUsername("stu1").orElseThrow();
        u.setEnabled(false);
        userRepository.saveAndFlush(u);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody("stu1", PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("blank credentials are rejected by validation with field errors")
    void blankCredentialsReturn400() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody("", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    @DisplayName("protected endpoints return 401 without a token or with an invalid one")
    void protectedEndpointsNeedAValidToken() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/student/summary").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void meReturnsTheLoggedInUser() throws Exception {
        String token = login("stu1");
        mvc.perform(withToken(get("/api/auth/me"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("stu1"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    @DisplayName("role-based access: each role is blocked from the other roles' endpoints (403)")
    void roleBasedAccess() throws Exception {
        String student = login("stu1");
        String faculty = login("fac1");
        String admin = login("admin1");

        mvc.perform(withToken(get("/api/admin/students"), student)).andExpect(status().isForbidden());
        mvc.perform(withToken(get("/api/admin/dashboard"), faculty)).andExpect(status().isForbidden());
        mvc.perform(withToken(get("/api/faculty/me/courses"), student)).andExpect(status().isForbidden());
        mvc.perform(withToken(get("/api/student/summary"), admin)).andExpect(status().isForbidden());
        mvc.perform(withToken(get("/api/student/summary"), faculty)).andExpect(status().isForbidden());

        mvc.perform(withToken(get("/api/admin/dashboard"), admin)).andExpect(status().isOk());
        mvc.perform(withToken(get("/api/faculty/me/courses"), faculty)).andExpect(status().isOk());
        mvc.perform(withToken(get("/api/student/summary"), student)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("health endpoint is public and does not leak component details")
    void healthIsPublicAndMinimal() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    @DisplayName("other actuator endpoints are not exposed")
    void otherActuatorEndpointsAreClosed() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().is4xxClientError());
        mvc.perform(get("/actuator/beans")).andExpect(status().is4xxClientError());
    }
}
