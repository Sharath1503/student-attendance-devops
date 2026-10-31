package com.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_records")
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 50)
    private String username;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(length = 500)
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public AuditRecord() {}

    public AuditRecord(Long userId, String username, String action, String details) {
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.details = details != null && details.length() > 500 ? details.substring(0, 500) : details;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
