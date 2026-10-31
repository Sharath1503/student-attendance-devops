package com.attendance.controller;

import com.attendance.dto.AttendanceDtos.HistoryItem;
import com.attendance.dto.AttendanceDtos.StudentSummaryResponse;
import com.attendance.dto.CourseDtos.CourseResponse;
import com.attendance.dto.StudentDtos.StudentResponse;
import com.attendance.service.StudentPortalService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** A student's own data only (role enforced in SecurityConfig: /api/student/** = STUDENT). */
@RestController
@RequestMapping("/api/student")
public class StudentPortalController {

    private final StudentPortalService service;

    public StudentPortalController(StudentPortalService service) {
        this.service = service;
    }

    @GetMapping("/profile")
    public StudentResponse profile() {
        return service.profile();
    }

    @GetMapping("/courses")
    public List<CourseResponse> courses() {
        return service.courses();
    }

    @GetMapping("/attendance")
    public List<HistoryItem> history(@RequestParam(required = false) Long courseId) {
        return service.history(courseId);
    }

    @GetMapping("/summary")
    public StudentSummaryResponse summary() {
        return service.summary();
    }
}
