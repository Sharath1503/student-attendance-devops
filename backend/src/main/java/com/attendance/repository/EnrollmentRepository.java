package com.attendance.repository;

import com.attendance.entity.Course;
import com.attendance.entity.Enrollment;
import com.attendance.entity.Student;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    long countByCourseId(Long courseId);

    @Query("select s from Enrollment e join e.student s join fetch s.user where e.course.id = :courseId order by s.fullName")
    List<Student> findStudentsByCourseId(@Param("courseId") Long courseId);

    @Query("select e.student.id from Enrollment e where e.course.id = :courseId")
    Set<Long> findStudentIdsByCourseId(@Param("courseId") Long courseId);

    @Query("select e.course from Enrollment e where e.student.id = :studentId order by e.course.code")
    List<Course> findCoursesByStudentId(@Param("studentId") Long studentId);
}
