package com.attendance.repository;

import com.attendance.entity.Student;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUserId(Long userId);

    boolean existsByStudentCode(String studentCode);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Student> findByEmailIgnoreCase(String email);

    /** q = "" matches everything. */
    @EntityGraph(attributePaths = "user")
    @Query("select s from Student s where lower(s.fullName) like lower(concat('%', :q, '%')) "
            + "or lower(s.studentCode) like lower(concat('%', :q, '%')) "
            + "or lower(s.department) like lower(concat('%', :q, '%'))")
    Page<Student> search(@Param("q") String q, Pageable pageable);
}
