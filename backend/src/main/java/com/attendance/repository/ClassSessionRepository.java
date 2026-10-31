package com.attendance.repository;

import com.attendance.domain.SessionStatus;
import com.attendance.entity.ClassSession;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {

    Optional<ClassSession> findByCourseIdAndSessionDateAndSlot(Long courseId, LocalDate date, int slot);

    List<ClassSession> findByCourseIdOrderBySessionDateDescSlotDesc(Long courseId);

    long countByStatus(SessionStatus status);

    long countByStatusAndSessionDateGreaterThanEqual(SessionStatus status, LocalDate from);
}
