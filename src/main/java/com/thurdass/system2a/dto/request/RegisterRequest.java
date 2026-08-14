package com.thurdass.system2a.dto.request;
import jakarta.validation.constraints.*;
public record RegisterRequest(@NotBlank @Size(max=50) String username, @NotBlank @Size(min=8,max=100) String password, @NotBlank @Size(max=120) String displayName, @NotNull Long classroomId) {}
