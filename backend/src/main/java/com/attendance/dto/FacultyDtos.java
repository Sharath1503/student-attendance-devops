package com.attendance.dto;

import com.attendance.entity.Faculty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class FacultyDtos {

    private FacultyDtos() {}

    public record FacultyCreateRequest(
            @NotBlank(message = "Faculty ID is required") @Size(max = 30) String facultyCode,
            @NotBlank(message = "Full name is required") @Size(max = 100) String fullName,
            @NotBlank(message = "Email is required") @Email(message = "Email is not valid") @Size(max = 120) String email,
            @NotBlank(message = "Department is required") @Size(max = 80) String department,
            @NotBlank(message = "Username is required")
            @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
            @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Username may contain letters, digits, '.', '_' and '-' only") String username,
            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 72, message = "Password must be 8-72 characters") String password) {}

    public record FacultyUpdateRequest(
            @NotBlank(message = "Full name is required") @Size(max = 100) String fullName,
            @NotBlank(message = "Email is required") @Email(message = "Email is not valid") @Size(max = 120) String email,
            @NotBlank(message = "Department is required") @Size(max = 80) String department,
            @NotNull(message = "Active flag is required") Boolean active,
            @Size(min = 8, max = 72, message = "Password must be 8-72 characters") String newPassword) {}

    public record FacultyResponse(Long id, String facultyCode, String fullName, String email, String department,
                                  String username, boolean active) {
        public static FacultyResponse from(Faculty f) {
            return new FacultyResponse(f.getId(), f.getFacultyCode(), f.getFullName(), f.getEmail(), f.getDepartment(),
                    f.getUser().getUsername(), f.isActive());
        }
    }
}
