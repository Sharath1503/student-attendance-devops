package com.attendance.entity;

import com.attendance.domain.AttendanceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Attendance of one student in one class session. (session, student) is unique in the database. */
@Entity
@Table(name = "attendance_records", uniqueConstraints = @UniqueConstraint(name = "uq_attendance", columnNames = {"session_id", "student_id"}))
public class AttendanceRecord extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ClassSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(name = "marked_by")
    private Long markedBy;

    public Long getId() { return id; }
    public ClassSession getSession() { return session; }
    public void setSession(ClassSession session) { this.session = session; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }
    public Long getMarkedBy() { return markedBy; }
    public void setMarkedBy(Long markedBy) { this.markedBy = markedBy; }
}
