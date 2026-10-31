package com.attendance.controller;

import com.attendance.dto.CommonDtos.PageResponse;
import com.attendance.dto.StudentDtos.StudentCreateRequest;
import com.attendance.dto.StudentDtos.StudentResponse;
import com.attendance.dto.StudentDtos.StudentUpdateRequest;
import com.attendance.service.StudentService;
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
@RequestMapping("/api/admin/students")
public class AdminStudentController {

    private final StudentService service;

    public AdminStudentController(StudentService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<StudentResponse> list(@RequestParam(defaultValue = "") String q,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return service.list(q, page, size);
    }

    @GetMapping("/{id}")
    public StudentResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse create(@Valid @RequestBody StudentCreateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentUpdateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        service.deactivate(id);
    }
}
