package com.thurdass.system2a.controller;

import com.thurdass.system2a.dto.request.AnnouncementRequest;
import com.thurdass.system2a.dto.response.AnnouncementResponse;
import com.thurdass.system2a.entity.*;
import com.thurdass.system2a.exception.ResourceNotFoundException;
import com.thurdass.system2a.repository.AnnouncementRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {
    final AnnouncementRepository announcementRepository;

    public AnnouncementController(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    @GetMapping
    public List<AnnouncementResponse> list(@AuthenticationPrincipal User authenticatedUser) {
        return announcementRepository
                .findByClassroomIdAndActiveTrueOrderByPinnedDescCreatedAtDesc(
                        authenticatedUser.getClassroom().getId()
                )
                .stream()
                .map(AnnouncementResponse::of)
                .toList();
    }

    @GetMapping("/{id}")
    public AnnouncementResponse get(
            @PathVariable("id") Long announcementId,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        var announcement = find(announcementId);
        if (!announcement.getClassroom().getId().equals(authenticatedUser.getClassroom().getId())) {
            throw new ResourceNotFoundException("Announcement not found");
        }
        return AnnouncementResponse.of(announcement);
    }

    @PostMapping
    public AnnouncementResponse add(
            @Valid @RequestBody AnnouncementRequest announcementRequest,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        var announcement = new Announcement();
        announcement.setTitle(announcementRequest.title().trim());
        announcement.setContent(announcementRequest.content().trim());
        announcement.setCreatedBy(authenticatedUser);
        announcement.setClassroom(authenticatedUser.getClassroom());
        announcement.setPinned(
                announcementRequest.pinned()
                        && authenticatedUser.getRole().name().equals("ADMIN")
        );
        return AnnouncementResponse.of(announcementRepository.save(announcement));
    }

    @PutMapping("/{id}")
    public AnnouncementResponse edit(
            @PathVariable("id") Long announcementId,
            @Valid @RequestBody AnnouncementRequest announcementRequest,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        var announcement = find(announcementId);
        if (!announcement.getCreatedBy().getId().equals(authenticatedUser.getId())
                && !authenticatedUser.getRole().name().equals("ADMIN")) {
            throw new AccessDeniedException("Only creator or admin can edit");
        }
        announcement.setTitle(announcementRequest.title().trim());
        announcement.setContent(announcementRequest.content().trim());
        if (authenticatedUser.getRole().name().equals("ADMIN")) {
            announcement.setPinned(announcementRequest.pinned());
        }
        return AnnouncementResponse.of(announcementRepository.save(announcement));
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable("id") Long announcementId,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        var announcement = find(announcementId);
        if (!announcement.getCreatedBy().getId().equals(authenticatedUser.getId())
                && !authenticatedUser.getRole().name().equals("ADMIN")) {
            throw new AccessDeniedException("Only creator or admin can delete");
        }
        announcement.setActive(false);
        announcementRepository.save(announcement);
    }

    private Announcement find(Long announcementId) {
        return announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));
    }
}
