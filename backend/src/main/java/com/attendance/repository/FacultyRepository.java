package com.attendance.repository;

import com.attendance.entity.Faculty;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {

    Optional<Faculty> findByUserId(Long userId);

    boolean existsByFacultyCode(String facultyCode);

    Optional<Faculty> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "user")
    @Query("select f from Faculty f where lower(f.fullName) like lower(concat('%', :q, '%')) "
            + "or lower(f.facultyCode) like lower(concat('%', :q, '%')) "
            + "or lower(f.department) like lower(concat('%', :q, '%'))")
    Page<Faculty> search(@Param("q") String q, Pageable pageable);
}
