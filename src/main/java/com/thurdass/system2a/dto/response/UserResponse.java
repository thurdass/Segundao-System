package com.thurdass.system2a.dto.response;

import com.thurdass.system2a.entity.User;

import java.time.LocalDateTime;

public record UserResponse(Long id, String username, String displayName, String role, boolean enabled, Long classroomId,
                           LocalDateTime createdAt) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getDisplayName(), u.getRole().name(), u.isEnabled(), u.getClassroom().getId(), u.getCreatedAt());
    }
}
