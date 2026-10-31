package com.attendance.service;

import com.attendance.dto.AuthDtos.LoginRequest;
import com.attendance.dto.AuthDtos.LoginResponse;
import com.attendance.dto.AuthDtos.UserInfo;
import com.attendance.entity.Faculty;
import com.attendance.entity.Student;
import com.attendance.repository.FacultyRepository;
import com.attendance.repository.StudentRepository;
import com.attendance.security.AppUserDetails;
import com.attendance.security.CurrentUserService;
import com.attendance.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final CurrentUserService currentUser;
    private final AuditService auditService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService,
                       StudentRepository studentRepository, FacultyRepository facultyRepository,
                       CurrentUserService currentUser, AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim();
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.password()));
            AppUserDetails user = (AppUserDetails) auth.getPrincipal();
            String token = jwtService.generateToken(user.getUsername(), user.getRole());
            auditService.recordFor(user.getId(), user.getUsername(), "LOGIN", "Successful login");
            log.info("User '{}' logged in with role {}", user.getUsername(), user.getRole());
            return new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds(), toUserInfo(user));
        } catch (AuthenticationException ex) {
            String safeName = username.replaceAll("[\\r\\n\\t]", "_");
            log.warn("Failed login attempt for username '{}'", safeName);
            auditService.recordFor(null, safeName.length() > 50 ? safeName.substring(0, 50) : safeName,
                    "LOGIN_FAILED", "Invalid credentials");
            // Same message for unknown user, wrong password and disabled account (no user enumeration).
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    public UserInfo me() {
        return toUserInfo(currentUser.require());
    }

    public void logout() {
        // Tokens are stateless: the client discards the token. We only record the event.
        auditService.record("LOGOUT", "User logged out");
    }

    private UserInfo toUserInfo(AppUserDetails user) {
        String displayName = switch (user.getRole()) {
            case STUDENT -> studentRepository.findByUserId(user.getId()).map(Student::getFullName).orElse(user.getUsername());
            case FACULTY -> facultyRepository.findByUserId(user.getId()).map(Faculty::getFullName).orElse(user.getUsername());
            case ADMIN -> user.getUsername();
        };
        return new UserInfo(user.getId(), user.getUsername(), user.getRole(), displayName);
    }
}
