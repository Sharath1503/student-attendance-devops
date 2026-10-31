package com.attendance.repository;

import com.attendance.entity.Course;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {

    boolean existsByCode(String code);

    Optional<Course> findByCode(String code);

    @Query("select c from Course c where lower(c.name) like lower(concat('%', :q, '%')) "
            + "or lower(c.code) like lower(concat('%', :q, '%')) "
            + "or lower(c.department) like lower(concat('%', :q, '%'))")
    Page<Course> search(@Param("q") String q, Pageable pageable);
}
