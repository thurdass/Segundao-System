package com.thurdass.system2a.dto.request;

import jakarta.validation.constraints.*;

public record AnnouncementRequest(@NotBlank String title, @NotBlank String content, boolean pinned) {
}
