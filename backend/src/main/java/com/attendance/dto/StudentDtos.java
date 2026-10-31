package com.attendance.dto;

import com.attendance.entity.Student;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class StudentDtos {

    private StudentDtos() {}

    public record StudentCreateRequest(
            @NotBlank(message = "Student ID is required") @Size(max = 30) String studentCode,
            @NotBlank(message = "Full name is required") @Size(max = 100) String fullName,
            @NotBlank(message = "Email is required") @Email(message = "Email is not valid") @Size(max = 120) String email,
            @NotBlank(message = "Department is required") @Size(max = 80) String department,
            @NotBlank(message = "Programme/course is required") @Size(max = 80) String programme,
            @NotBlank(message = "Academic year is required") @Size(max = 20) String academicYear,
            @NotBlank(message = "Username is required")
            @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
            @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Username may contain letters, digits, '.', '_' and '-' only") String username,
            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 72, message = "Password must be 8-72 characters") String password) {}

    public record StudentUpdateRequest(
            @NotBlank(message = "Full name is required") @Size(max = 100) String fullName,
            @NotBlank(message = "Email is required") @Email(message = "Email is not valid") @Size(max = 120) String email,
            @NotBlank(message = "Department is required") @Size(max = 80) String department,
            @NotBlank(message = "Programme/course is required") @Size(max = 80) String programme,
            @NotBlank(message = "Academic year is required") @Size(max = 20) String academicYear,
            @NotNull(message = "Active flag is required") Boolean active,
            @Size(min = 8, max = 72, message = "Password must be 8-72 characters") String newPassword) {}

    public record StudentResponse(Long id, String studentCode, String fullName, String email, String department,
                                  String programme, String academicYear, String username, boolean active) {
        public static StudentResponse from(Student s) {
            return new StudentResponse(s.getId(), s.getStudentCode(), s.getFullName(), s.getEmail(), s.getDepartment(),
                    s.getProgramme(), s.getAcademicYear(), s.getUser().getUsername(), s.isActive());
        }
    }
}
