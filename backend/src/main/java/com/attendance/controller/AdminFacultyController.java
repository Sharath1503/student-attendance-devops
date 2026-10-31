package com.attendance.controller;

import com.attendance.dto.CommonDtos.PageResponse;
import com.attendance.dto.FacultyDtos.FacultyCreateRequest;
import com.attendance.dto.FacultyDtos.FacultyResponse;
import com.attendance.dto.FacultyDtos.FacultyUpdateRequest;
import com.attendance.service.FacultyService;
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
@RequestMapping("/api/admin/faculty")
public class AdminFacultyController {

    private final FacultyService service;

    public AdminFacultyController(FacultyService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<FacultyResponse> list(@RequestParam(defaultValue = "") String q,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return service.list(q, page, size);
    }

    @GetMapping("/{id}")
    public FacultyResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FacultyResponse create(@Valid @RequestBody FacultyCreateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public FacultyResponse update(@PathVariable Long id, @Valid @RequestBody FacultyUpdateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        service.deactivate(id);
    }
}
