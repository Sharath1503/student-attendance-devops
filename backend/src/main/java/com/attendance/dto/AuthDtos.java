package com.attendance.dto;

import com.attendance.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank(message = "Username is required") @Size(max = 50) String username,
            @NotBlank(message = "Password is required") @Size(max = 72) String password) {}

    public record UserInfo(Long id, String username, Role role, String displayName) {}

    public record LoginResponse(String token, String tokenType, long expiresInSeconds, UserInfo user) {}
}
