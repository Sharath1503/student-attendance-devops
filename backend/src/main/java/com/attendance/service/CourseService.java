package com.attendance.service;

import com.attendance.dto.CommonDtos.PageResponse;
import com.attendance.dto.CourseDtos.CourseDetailResponse;
import com.attendance.dto.CourseDtos.CourseRequest;
import com.attendance.dto.CourseDtos.CourseResponse;
import com.attendance.dto.FacultyDtos.FacultyResponse;
import com.attendance.dto.StudentDtos.StudentResponse;
import com.attendance.entity.Course;
import com.attendance.entity.CourseAssignment;
import com.attendance.entity.Enrollment;
import com.attendance.entity.Faculty;
import com.attendance.entity.Student;
import com.attendance.exception.BadRequestException;
import com.attendance.exception.ConflictException;
import com.attendance.exception.ResourceNotFoundException;
import com.attendance.repository.CourseAssignmentRepository;
import com.attendance.repository.CourseRepository;
import com.attendance.repository.EnrollmentRepository;
import com.attendance.repository.FacultyRepository;
import com.attendance.repository.StudentRepository;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;
    private final CourseAssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AuditService audit;

    public CourseService(CourseRepository courseRepository, FacultyRepository facultyRepository,
                         StudentRepository studentRepository, CourseAssignmentRepository assignmentRepository,
                         EnrollmentRepository enrollmentRepository, AuditService audit) {
        this.courseRepository = courseRepository;
        this.facultyRepository = facultyRepository;
        this.studentRepository = studentRepository;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> list(String q, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("code"));
        return PageResponse.from(courseRepository.search(q == null ? "" : q.trim(), pageable).map(CourseResponse::from));
    }

    @Transactional(readOnly = true)
    public CourseDetailResponse detail(Long id) {
        Course c = find(id);
        return new CourseDetailResponse(CourseResponse.from(c),
                assignmentRepository.findFacultyByCourseId(id).stream().map(FacultyResponse::from).toList(),
                enrollmentRepository.findStudentsByCourseId(id).stream().map(StudentResponse::from).toList());
    }

    @Transactional
    public CourseResponse create(CourseRequest r) {
        String code = normalise(r.code());
        if (courseRepository.existsByCode(code)) {
            throw new ConflictException("Course code '" + code + "' already exists");
        }
        Course c = new Course();
        c.setCode(code);
        c.setName(r.name().trim());
        c.setDepartment(r.department().trim());
        c.setActive(r.active());
        courseRepository.save(c);
        audit.record("COURSE_CREATED", "code=" + code);
        return CourseResponse.from(c);
    }

    @Transactional
    public CourseResponse update(Long id, CourseRequest r) {
        Course c = find(id);
        String code = normalise(r.code());
        courseRepository.findByCode(code).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new ConflictException("Course code '" + code + "' already exists");
            }
        });
        c.setCode(code);
        c.setName(r.name().trim());
        c.setDepartment(r.department().trim());
        c.setActive(r.active());
        courseRepository.save(c);
        audit.record("COURSE_UPDATED", "code=" + code);
        return CourseResponse.from(c);
    }

    /** "Delete" deactivates the course; history is kept and no new attendance can be recorded. */
    @Transactional
    public void deactivate(Long id) {
        Course c = find(id);
        c.setActive(false);
        courseRepository.save(c);
        audit.record("COURSE_DEACTIVATED", "code=" + c.getCode());
    }

    @Transactional
    public void assignFaculty(Long courseId, Long facultyId) {
        Course c = find(courseId);
        Faculty f = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty " + facultyId + " not found"));
        if (!f.isActive()) {
            throw new BadRequestException("Cannot assign an inactive faculty member");
        }
        if (assignmentRepository.existsByFacultyIdAndCourseId(facultyId, courseId)) {
            throw new ConflictException("Faculty is already assigned to this course");
        }
        assignmentRepository.save(new CourseAssignment(f, c));
        audit.record("FACULTY_ASSIGNED", "course=" + c.getCode() + ", faculty=" + f.getFacultyCode());
    }

    @Transactional
    public void unassignFaculty(Long courseId, Long facultyId) {
        CourseAssignment a = assignmentRepository.findByFacultyIdAndCourseId(facultyId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        assignmentRepository.delete(a);
        audit.record("FACULTY_UNASSIGNED", "courseId=" + courseId + ", facultyId=" + facultyId);
    }

    @Transactional
    public void enrollStudent(Long courseId, Long studentId) {
        Course c = find(courseId);
        Student s = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student " + studentId + " not found"));
        if (!s.isActive()) {
            throw new BadRequestException("Cannot enrol an inactive student");
        }
        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new ConflictException("Student is already enrolled in this course");
        }
        enrollmentRepository.save(new Enrollment(s, c));
        audit.record("STUDENT_ENROLLED", "course=" + c.getCode() + ", student=" + s.getStudentCode());
    }

    @Transactional
    public void unenrollStudent(Long courseId, Long studentId) {
        Enrollment e = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrolment not found"));
        enrollmentRepository.delete(e);
        audit.record("STUDENT_UNENROLLED", "courseId=" + courseId + ", studentId=" + studentId);
    }

    private Course find(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course " + id + " not found"));
    }

    private String normalise(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
