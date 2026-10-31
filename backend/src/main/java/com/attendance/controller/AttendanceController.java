package com.attendance.controller;

import com.attendance.dto.AttendanceDtos.CourseSummaryResponse;
import com.attendance.dto.AttendanceDtos.MarkAttendanceRequest;
import com.attendance.dto.AttendanceDtos.SessionDetailResponse;
import com.attendance.dto.AttendanceDtos.SessionResponse;
import com.attendance.dto.StudentDtos.StudentResponse;
import com.attendance.service.AttendanceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Course attendance endpoints shared by faculty and administrators.
 * Role is checked here (@PreAuthorize); "assigned to this course" is checked again in AttendanceService.
 */
@RestController
@RequestMapping("/api/attendance/courses/{courseId}")
public class AttendanceController {

    private final AttendanceService service;

    public AttendanceController(AttendanceService service) {
        this.service = service;
    }

    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public List<StudentResponse> roster(@PathVariable Long courseId) {
        return service.roster(courseId);
    }

    /** Create the session for (date, slot) or update it if it already exists. Faculty assigned to the course only. */
    @PostMapping("/sessions")
    @PreAuthorize("hasRole('FACULTY')")
    public SessionDetailResponse save(@PathVariable Long courseId, @Valid @RequestBody MarkAttendanceRequest request) {
        return service.markAttendance(courseId, request);
    }

    @GetMapping("/sessions")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public List<SessionResponse> sessions(@PathVariable Long courseId) {
        return service.sessions(courseId);
    }

    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public SessionDetailResponse sessionDetail(@PathVariable Long courseId, @PathVariable Long sessionId) {
        return service.sessionDetail(courseId, sessionId);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public CourseSummaryResponse summary(@PathVariable Long courseId) {
        return service.courseSummary(courseId);
    }
}
