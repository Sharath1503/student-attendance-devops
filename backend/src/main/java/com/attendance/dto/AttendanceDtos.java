package com.attendance.dto;

import com.attendance.domain.AttendanceStatus;
import com.attendance.domain.SessionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public final class AttendanceDtos {

    private AttendanceDtos() {}

    public record AttendanceEntry(
            @NotNull(message = "studentId is required") Long studentId,
            @NotNull(message = "status is required") AttendanceStatus status) {}

    /**
     * Create or update one class session.
     * status HELD requires records; CANCELLED / HOLIDAY must have no records.
     */
    public record MarkAttendanceRequest(
            @NotNull(message = "date is required") LocalDate date,
            @Min(value = 1, message = "slot must be between 1 and 10") @Max(value = 10, message = "slot must be between 1 and 10") Integer slot,
            @NotNull(message = "session status is required") SessionStatus status,
            @Valid List<AttendanceEntry> records) {}

    public record SessionResponse(Long id, Long courseId, String courseCode, LocalDate date, int slot,
                                  SessionStatus status, long presentCount, long absentCount) {}

    public record RecordResponse(Long studentId, String studentCode, String studentName, AttendanceStatus status) {}

    public record SessionDetailResponse(SessionResponse session, List<RecordResponse> records) {}

    public record StudentCourseSummary(Long studentId, String studentCode, String studentName,
                                       long present, long absent, long total, Double percentage, boolean low) {}

    public record CourseSummaryResponse(Long courseId, String courseCode, String courseName, long heldSessions,
                                        double lowThresholdPercent, List<StudentCourseSummary> students) {}

    public record CourseAttendanceSummary(Long courseId, String courseCode, String courseName,
                                          long present, long absent, long total, Double percentage, boolean low) {}

    public record StudentSummaryResponse(Double overallPercentage, long overallPresent, long overallTotal,
                                         double lowThresholdPercent, List<CourseAttendanceSummary> courses,
                                         List<String> warnings) {}

    public record HistoryItem(LocalDate date, int slot, Long courseId, String courseCode, String courseName,
                              AttendanceStatus status) {}
}
