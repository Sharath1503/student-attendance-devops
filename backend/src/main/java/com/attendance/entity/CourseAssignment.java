package com.attendance.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Links a faculty member to a course they are authorised to manage. */
@Entity
@Table(name = "course_assignments", uniqueConstraints = @UniqueConstraint(name = "uq_assignment", columnNames = {"faculty_id", "course_id"}))
public class CourseAssignment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    public CourseAssignment() {}

    public CourseAssignment(Faculty faculty, Course course) {
        this.faculty = faculty;
        this.course = course;
    }

    public Long getId() { return id; }
    public Faculty getFaculty() { return faculty; }
    public Course getCourse() { return course; }
}
