package com.thurdass.system2a.dto.response;

import com.thurdass.system2a.entity.*;

import java.time.*;

public record ActivityResponse(Long id, String title, String description, LocalDate dueDate, Long subjectId,
                               Long classroomId, String createdBy, boolean completed, LocalDateTime completedAt) {
    public static ActivityResponse of(Activity a, ActivityCompletion c) {
        return new ActivityResponse(a.getId(), a.getTitle(), a.getDescription(), a.getDueDate(), a.getSubject().getId(), a.getClassroom().getId(), a.getCreatedBy().getUsername(), c != null, c == null ? null : c.getCompletedAt());
    }
}
