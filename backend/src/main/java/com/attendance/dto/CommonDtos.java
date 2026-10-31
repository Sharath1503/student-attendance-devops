package com.attendance.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;

/** Shared response shapes. */
public final class CommonDtos {

    private CommonDtos() {}

    /** Consistent error body returned for every failed request. */
    public record ApiError(Instant timestamp, int status, String error, String message, String path,
                           Map<String, String> fieldErrors) {

        public static ApiError of(int status, String error, String message, String path) {
            return new ApiError(Instant.now(), status, error, message, path, Map.of());
        }

        public static ApiError withFields(int status, String error, String message, String path,
                                          Map<String, String> fieldErrors) {
            return new ApiError(Instant.now(), status, error, message, path, fieldErrors);
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <T> PageResponse<T> from(Page<T> p) {
            return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
        }
    }

    public record MessageResponse(String message) {}

    public record AdminDashboardResponse(long totalStudents, long totalFaculty, long totalCourses,
                                         long totalHeldSessions, long sessionsLast7Days,
                                         Double overallAttendancePercentage, long lowAttendanceStudents,
                                         double lowThresholdPercent) {}

    public record AuditResponse(Long id, String username, String action, String details, java.time.LocalDateTime createdAt) {}
}
