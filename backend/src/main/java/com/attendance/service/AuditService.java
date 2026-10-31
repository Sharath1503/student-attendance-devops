package com.attendance.service;

import com.attendance.entity.AuditRecord;
import com.attendance.repository.AuditRecordRepository;
import com.attendance.security.AppUserDetails;
import com.attendance.security.CurrentUserService;
import org.springframework.stereotype.Service;

/** Writes security-relevant actions to the audit_records table (never stores passwords or tokens). */
@Service
public class AuditService {

    private final AuditRecordRepository repository;
    private final CurrentUserService currentUser;

    public AuditService(AuditRecordRepository repository, CurrentUserService currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    /** Records an action for the currently authenticated user (or anonymous if there is none). */
    public void record(String action, String details) {
        AppUserDetails user = currentUser.find().orElse(null);
        repository.save(new AuditRecord(user == null ? null : user.getId(),
                user == null ? null : user.getUsername(), action, details));
    }

    /** Records an action for an explicit user (used for login events). */
    public void recordFor(Long userId, String username, String action, String details) {
        repository.save(new AuditRecord(userId, username, action, details));
    }
}
