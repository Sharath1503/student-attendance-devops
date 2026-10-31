package com.attendance.service;

import com.attendance.domain.Role;
import com.attendance.dto.CommonDtos.PageResponse;
import com.attendance.dto.StudentDtos.StudentCreateRequest;
import com.attendance.dto.StudentDtos.StudentResponse;
import com.attendance.dto.StudentDtos.StudentUpdateRequest;
import com.attendance.entity.Student;
import com.attendance.entity.User;
import com.attendance.exception.ConflictException;
import com.attendance.exception.ResourceNotFoundException;
import com.attendance.repository.StudentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserAccountService accounts;
    private final AuditService audit;

    public StudentService(StudentRepository studentRepository, UserAccountService accounts, AuditService audit) {
        this.studentRepository = studentRepository;
        this.accounts = accounts;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> list(String q, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("fullName"));
        return PageResponse.from(studentRepository.search(q == null ? "" : q.trim(), pageable).map(StudentResponse::from));
    }

    @Transactional(readOnly = true)
    public StudentResponse get(Long id) {
        return StudentResponse.from(find(id));
    }

    @Transactional
    public StudentResponse create(StudentCreateRequest r) {
        String code = r.studentCode().trim();
        String email = r.email().trim();
        if (studentRepository.existsByStudentCode(code)) {
            throw new ConflictException("Student ID '" + code + "' already exists");
        }
        if (studentRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email '" + email + "' is already used by another student");
        }
        User user = accounts.create(r.username(), r.password(), Role.STUDENT);
        Student s = new Student();
        s.setUser(user);
        s.setStudentCode(code);
        s.setFullName(r.fullName().trim());
        s.setEmail(email);
        s.setDepartment(r.department().trim());
        s.setProgramme(r.programme().trim());
        s.setAcademicYear(r.academicYear().trim());
        s.setActive(true);
        studentRepository.save(s);
        audit.record("STUDENT_CREATED", "studentCode=" + code);
        return StudentResponse.from(s);
    }

    @Transactional
    public StudentResponse update(Long id, StudentUpdateRequest r) {
        Student s = find(id);
        String email = r.email().trim();
        studentRepository.findByEmailIgnoreCase(email).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new ConflictException("Email '" + email + "' is already used by another student");
            }
        });
        s.setFullName(r.fullName().trim());
        s.setEmail(email);
        s.setDepartment(r.department().trim());
        s.setProgramme(r.programme().trim());
        s.setAcademicYear(r.academicYear().trim());
        s.setActive(r.active());
        studentRepository.save(s);
        accounts.update(s.getUser(), r.active(), r.newPassword());
        audit.record("STUDENT_UPDATED", "studentCode=" + s.getStudentCode());
        return StudentResponse.from(s);
    }

    /** "Delete" deactivates the student and disables the login, keeping attendance history intact. */
    @Transactional
    public void deactivate(Long id) {
        Student s = find(id);
        s.setActive(false);
        studentRepository.save(s);
        accounts.update(s.getUser(), false, null);
        audit.record("STUDENT_DEACTIVATED", "studentCode=" + s.getStudentCode());
    }

    private Student find(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student " + id + " not found"));
    }
}
