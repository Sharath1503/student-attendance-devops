package com.attendance.controller;

import com.attendance.dto.CommonDtos.AdminDashboardResponse;
import com.attendance.dto.CommonDtos.AuditResponse;
import com.attendance.dto.CommonDtos.PageResponse;
import com.attendance.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    private final DashboardService service;

    public AdminDashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return service.adminDashboard();
    }

    @GetMapping("/audit")
    public PageResponse<AuditResponse> audit(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return service.audit(page, size);
    }
}
