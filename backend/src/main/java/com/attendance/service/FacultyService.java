package com.attendance.service;

import com.attendance.domain.Role;
import com.attendance.dto.CommonDtos.PageResponse;
import com.attendance.dto.CourseDtos.CourseResponse;
import com.attendance.dto.FacultyDtos.FacultyCreateRequest;
import com.attendance.dto.FacultyDtos.FacultyResponse;
import com.attendance.dto.FacultyDtos.FacultyUpdateRequest;
import com.attendance.entity.Faculty;
import com.attendance.entity.User;
import com.attendance.exception.ConflictException;
import com.attendance.exception.ResourceNotFoundException;
import com.attendance.repository.CourseAssignmentRepository;
import com.attendance.repository.FacultyRepository;
import com.attendance.security.CurrentUserService;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FacultyService {

    private final FacultyRepository facultyRepository;
    private final CourseAssignmentRepository assignmentRepository;
    private final UserAccountService accounts;
    private final AuditService audit;
    private final CurrentUserService currentUser;

    public FacultyService(FacultyRepository facultyRepository, CourseAssignmentRepository assignmentRepository,
                          UserAccountService accounts, AuditService audit, CurrentUserService currentUser) {
        this.facultyRepository = facultyRepository;
        this.assignmentRepository = assignmentRepository;
        this.accounts = accounts;
        this.audit = audit;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public PageResponse<FacultyResponse> list(String q, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("fullName"));
        return PageResponse.from(facultyRepository.search(q == null ? "" : q.trim(), pageable).map(FacultyResponse::from));
    }

    @Transactional(readOnly = true)
    public FacultyResponse get(Long id) {
        return FacultyResponse.from(find(id));
    }

    @Transactional
    public FacultyResponse create(FacultyCreateRequest r) {
        String code = r.facultyCode().trim();
        String email = r.email().trim();
        if (facultyRepository.existsByFacultyCode(code)) {
            throw new ConflictException("Faculty ID '" + code + "' already exists");
        }
        if (facultyRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email '" + email + "' is already used by another faculty member");
        }
        User user = accounts.create(r.username(), r.password(), Role.FACULTY);
        Faculty f = new Faculty();
        f.setUser(user);
        f.setFacultyCode(code);
        f.setFullName(r.fullName().trim());
        f.setEmail(email);
        f.setDepartment(r.department().trim());
        f.setActive(true);
        facultyRepository.save(f);
        audit.record("FACULTY_CREATED", "facultyCode=" + code);
        return FacultyResponse.from(f);
    }

    @Transactional
    public FacultyResponse update(Long id, FacultyUpdateRequest r) {
        Faculty f = find(id);
        String email = r.email().trim();
        facultyRepository.findByEmailIgnoreCase(email).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new ConflictException("Email '" + email + "' is already used by another faculty member");
            }
        });
        f.setFullName(r.fullName().trim());
        f.setEmail(email);
        f.setDepartment(r.department().trim());
        f.setActive(r.active());
        facultyRepository.save(f);
        accounts.update(f.getUser(), r.active(), r.newPassword());
        audit.record("FACULTY_UPDATED", "facultyCode=" + f.getFacultyCode());
        return FacultyResponse.from(f);
    }

    @Transactional
    public void deactivate(Long id) {
        Faculty f = find(id);
        f.setActive(false);
        facultyRepository.save(f);
        accounts.update(f.getUser(), false, null);
        audit.record("FACULTY_DEACTIVATED", "facultyCode=" + f.getFacultyCode());
    }

    /** Courses assigned to the logged-in faculty member. */
    @Transactional(readOnly = true)
    public List<CourseResponse> myCourses() {
        Faculty me = currentFaculty();
        return assignmentRepository.findCoursesByFacultyId(me.getId()).stream().map(CourseResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public FacultyResponse myProfile() {
        return FacultyResponse.from(currentFaculty());
    }

    public Faculty currentFaculty() {
        Long userId = currentUser.require().getId();
        return facultyRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("You do not have a faculty profile"));
    }

    private Faculty find(Long id) {
        return facultyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty " + id + " not found"));
    }
}
