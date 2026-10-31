package com.attendance.dto;

import com.attendance.dto.FacultyDtos.FacultyResponse;
import com.attendance.dto.StudentDtos.StudentResponse;
import com.attendance.entity.Course;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class CourseDtos {

    private CourseDtos() {}

    public record CourseRequest(
            @NotBlank(message = "Course code is required") @Size(max = 30) String code,
            @NotBlank(message = "Course name is required") @Size(max = 120) String name,
            @NotBlank(message = "Department is required") @Size(max = 80) String department,
            @NotNull(message = "Active flag is required") Boolean active) {}

    public record CourseResponse(Long id, String code, String name, String department, boolean active) {
        public static CourseResponse from(Course c) {
            return new CourseResponse(c.getId(), c.getCode(), c.getName(), c.getDepartment(), c.isActive());
        }
    }

    public record CourseDetailResponse(CourseResponse course, List<FacultyResponse> faculty, List<StudentResponse> students) {}
}
