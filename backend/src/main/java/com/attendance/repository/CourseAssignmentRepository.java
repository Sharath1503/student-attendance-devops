package com.attendance.repository;

import com.attendance.entity.Course;
import com.attendance.entity.CourseAssignment;
import com.attendance.entity.Faculty;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseAssignmentRepository extends JpaRepository<CourseAssignment, Long> {

    boolean existsByFacultyIdAndCourseId(Long facultyId, Long courseId);

    Optional<CourseAssignment> findByFacultyIdAndCourseId(Long facultyId, Long courseId);

    long countByCourseId(Long courseId);

    @Query("select a.course from CourseAssignment a where a.faculty.id = :facultyId order by a.course.code")
    List<Course> findCoursesByFacultyId(@Param("facultyId") Long facultyId);

    @Query("select f from CourseAssignment a join a.faculty f join fetch f.user where a.course.id = :courseId order by f.fullName")
    List<Faculty> findFacultyByCourseId(@Param("courseId") Long courseId);
}
