package com.thurdass.system2a.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUserRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 120) String displayName,
        @Size(max = 20) String role,
        @NotNull Long classroomId
) {
}
