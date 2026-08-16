package com.thurdass.system2a.dto.response;

import com.thurdass.system2a.entity.User;

import java.time.LocalDateTime;

public record UserResponse(Long id, String username, String displayName, String role, boolean enabled,
                           boolean mustChangePassword, Long classroomId, LocalDateTime createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole().name(),
                user.isEnabled(),
                user.isMustChangePassword(),
                user.getClassroom().getId(),
                user.getCreatedAt()
        );
    }
}
