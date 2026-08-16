package com.thurdass.system2a.dto.response;

import com.thurdass.system2a.entity.Announcement;

import java.time.*;

public record AnnouncementResponse(Long id, String title, String content, LocalDateTime createdAt, String createdBy,
                                   boolean pinned) {
    public static AnnouncementResponse of(Announcement announcement) {
        return new AnnouncementResponse(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getCreatedAt(),
                announcement.getCreatedBy().getUsername(),
                announcement.isPinned()
        );
    }
}
