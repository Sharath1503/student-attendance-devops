package com.attendance.repository;

import com.attendance.domain.SessionStatus;
import com.attendance.entity.AttendanceRecord;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    @Query("select r from AttendanceRecord r join fetch r.student st where r.session.id = :sessionId order by st.fullName")
    List<AttendanceRecord> findBySessionIdWithStudent(@Param("sessionId") Long sessionId);

    long countBySessionId(Long sessionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from AttendanceRecord r where r.session.id = :sessionId")
    void deleteAllBySessionId(@Param("sessionId") Long sessionId);

    /** Rows: [sessionId, status, count] */
    @Query("select r.session.id, r.status, count(r) from AttendanceRecord r "
            + "where r.session.id in :sessionIds group by r.session.id, r.status")
    List<Object[]> countBySessionIds(@Param("sessionIds") Collection<Long> sessionIds);

    /** Rows: [studentId, status, count] for HELD sessions of one course. */
    @Query("select r.student.id, r.status, count(r) from AttendanceRecord r "
            + "where r.session.course.id = :courseId and r.session.status = :held "
            + "group by r.student.id, r.status")
    List<Object[]> countByStudentForCourse(@Param("courseId") Long courseId, @Param("held") SessionStatus held);

    /** Rows: [courseId, status, count] for one student (HELD sessions only). */
    @Query("select r.session.course.id, r.status, count(r) from AttendanceRecord r "
            + "where r.student.id = :studentId and r.session.status = :held "
            + "group by r.session.course.id, r.status")
    List<Object[]> countByCourseForStudent(@Param("studentId") Long studentId, @Param("held") SessionStatus held);

    /** Rows: [studentId, status, count] over all courses (HELD sessions only). */
    @Query("select r.student.id, r.status, count(r) from AttendanceRecord r "
            + "where r.session.status = :held group by r.student.id, r.status")
    List<Object[]> countAllByStudent(@Param("held") SessionStatus held);

    @Query("select r from AttendanceRecord r join fetch r.session s join fetch s.course "
            + "where r.student.id = :studentId and s.status = :held "
            + "order by s.sessionDate desc, s.slot desc")
    List<AttendanceRecord> findHistoryForStudent(@Param("studentId") Long studentId, @Param("held") SessionStatus held);

    @Query("select r from AttendanceRecord r join fetch r.session s join fetch s.course c "
            + "where r.student.id = :studentId and c.id = :courseId and s.status = :held "
            + "order by s.sessionDate desc, s.slot desc")
    List<AttendanceRecord> findHistoryForStudentAndCourse(@Param("studentId") Long studentId,
                                                          @Param("courseId") Long courseId,
                                                          @Param("held") SessionStatus held);
}
