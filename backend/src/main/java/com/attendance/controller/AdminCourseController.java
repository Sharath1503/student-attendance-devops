package com.attendance.controller;

import com.attendance.dto.CommonDtos.PageResponse;
import com.attendance.dto.CourseDtos.CourseDetailResponse;
import com.attendance.dto.CourseDtos.CourseRequest;
import com.attendance.dto.CourseDtos.CourseResponse;
import com.attendance.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/courses")
public class AdminCourseController {

    private final CourseService service;

    public AdminCourseController(CourseService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<CourseResponse> list(@RequestParam(defaultValue = "") String q,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return service.list(q, page, size);
    }

    @GetMapping("/{id}")
    public CourseDetailResponse detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@Valid @RequestBody CourseRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        service.deactivate(id);
    }

    @PostMapping("/{id}/faculty/{facultyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignFaculty(@PathVariable Long id, @PathVariable Long facultyId) {
        service.assignFaculty(id, facultyId);
    }

    @DeleteMapping("/{id}/faculty/{facultyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassignFaculty(@PathVariable Long id, @PathVariable Long facultyId) {
        service.unassignFaculty(id, facultyId);
    }

    @PostMapping("/{id}/students/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enrollStudent(@PathVariable Long id, @PathVariable Long studentId) {
        service.enrollStudent(id, studentId);
    }

    @DeleteMapping("/{id}/students/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unenrollStudent(@PathVariable Long id, @PathVariable Long studentId) {
        service.unenrollStudent(id, studentId);
    }
}
