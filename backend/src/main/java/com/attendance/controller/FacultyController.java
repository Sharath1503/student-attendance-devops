package com.attendance.controller;

import com.attendance.dto.CourseDtos.CourseResponse;
import com.attendance.dto.FacultyDtos.FacultyResponse;
import com.attendance.service.FacultyService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints for the logged-in faculty member (role enforced in SecurityConfig: /api/faculty/** = FACULTY). */
@RestController
@RequestMapping("/api/faculty")
public class FacultyController {

    private final FacultyService service;

    public FacultyController(FacultyService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public FacultyResponse profile() {
        return service.myProfile();
    }

    @GetMapping("/me/courses")
    public List<CourseResponse> myCourses() {
        return service.myCourses();
    }
}
