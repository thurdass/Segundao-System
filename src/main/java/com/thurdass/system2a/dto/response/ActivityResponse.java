package com.thurdass.system2a.dto.response;

import com.thurdass.system2a.entity.*;

import java.time.*;

public record ActivityResponse(Long id, String title, String description, LocalDate dueDate, Long subjectId,
                               Long classroomId, String createdBy, boolean completed, LocalDateTime completedAt) {
    public static ActivityResponse of(Activity activity, ActivityCompletion completion) {
        return new ActivityResponse(
                activity.getId(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getDueDate(),
                activity.getSubject().getId(),
                activity.getClassroom().getId(),
                activity.getCreatedBy().getUsername(),
                completion != null,
                completion == null ? null : completion.getCompletedAt()
        );
    }
}
