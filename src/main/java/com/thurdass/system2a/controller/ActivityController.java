package com.thurdass.system2a.controller;

import com.thurdass.system2a.dto.request.*;
import com.thurdass.system2a.dto.response.*;
import com.thurdass.system2a.entity.*;
import com.thurdass.system2a.enums.DeadlineMode;
import com.thurdass.system2a.exception.*;
import com.thurdass.system2a.repository.*;
import com.thurdass.system2a.service.NextClassService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {
    final ActivityRepository activities;
    final CompletionRepository completions;
    final SubjectRepository subjects;
    final NextClassService nextClassService;

    public ActivityController(
            ActivityRepository activityRepository,
            CompletionRepository completionRepository,
            SubjectRepository subjectRepository,
            NextClassService nextClassService
    ) {
        activities = activityRepository;
        completions = completionRepository;
        subjects = subjectRepository;
        this.nextClassService = nextClassService;
    }

    @GetMapping
    public List<ActivityResponse> list(
            @AuthenticationPrincipal User authenticatedUser,
            @RequestParam(name = "status", required = false) String statusFilter,
            @RequestParam(name = "subjectId", required = false) Long subjectId,
            @RequestParam(name = "dueBefore", required = false) LocalDate dueDateBefore
    ) {
        return activities.findByClassroomIdAndActiveTrueOrderByDueDateAsc(
                        authenticatedUser.getClassroom().getId()
                )
                .stream()
                .filter(activity -> subjectId == null
                        || activity.getSubject().getId().equals(subjectId))
                .filter(activity -> dueDateBefore == null
                        || !activity.getDueDate().isAfter(dueDateBefore))
                .filter(activity -> {
                    boolean isCompleted = completions.existsByUserIdAndActivityId(
                            authenticatedUser.getId(),
                            activity.getId()
                    );
                    return statusFilter == null
                            || statusFilter.equalsIgnoreCase(isCompleted ? "completed" : "pending");
                })
                .map(activity -> ActivityResponse.of(
                        activity,
                        completions.findByUserIdAndActivityId(
                                authenticatedUser.getId(),
                                activity.getId()
                        ).orElse(null)
                ))
                .toList();
    }

    @GetMapping("/{id}")
    public ActivityResponse get(
            @PathVariable("id") Long activityId,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        Activity activity = getActivity(activityId, authenticatedUser);
        return ActivityResponse.of(
                activity,
                completions.findByUserIdAndActivityId(
                        authenticatedUser.getId(),
                        activityId
                ).orElse(null)
        );
    }

    @PostMapping
    public ActivityResponse add(
            @Valid @RequestBody ActivityRequest activityRequest,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        var subject = subjects.findById(activityRequest.subjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        if (!subject.getClassroom().getId().equals(authenticatedUser.getClassroom().getId())) {
            throw new BusinessException("Activity must belong to your classroom");
        }

        LocalDate dueDate = activityRequest.dueDate();
        if (activityRequest.deadlineMode() == DeadlineMode.NEXT_CLASS) {
            dueDate = nextClassService.date(
                    nextClassService.next(subject.getId(), authenticatedUser.getClassroom().getId())
            );
        }
        if (dueDate == null || dueDate.isBefore(LocalDate.now())) {
            throw new BusinessException("Due date must be today or later");
        }

        var activity = new Activity();
        activity.setTitle(activityRequest.title().trim());
        activity.setDescription(activityRequest.description());
        activity.setDueDate(dueDate);
        activity.setSubject(subject);
        activity.setClassroom(authenticatedUser.getClassroom());
        activity.setCreatedBy(authenticatedUser);
        return ActivityResponse.of(activities.save(activity), null);
    }

    @PostMapping("/{id}/complete")
    public ActivityResponse complete(
            @PathVariable("id") Long activityId,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        Activity activity = getActivity(activityId, authenticatedUser);
        if (completions.existsByUserIdAndActivityId(authenticatedUser.getId(), activityId)) {
            throw new BusinessException("Activity already completed");
        }

        var completion = new ActivityCompletion();
        completion.setActivity(activity);
        completion.setUser(authenticatedUser);
        return ActivityResponse.of(activity, completions.save(completion));
    }

    @DeleteMapping("/{id}/complete")
    public void uncomplete(
            @PathVariable("id") Long activityId,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        getActivity(activityId, authenticatedUser);
        var completion = completions.findByUserIdAndActivityId(
                        authenticatedUser.getId(),
                        activityId
                )
                .orElseThrow(() -> new BusinessException("Activity is not completed by this user"));
        completions.delete(completion);
    }

    @PutMapping("/{id}")
    public ActivityResponse edit(
            @PathVariable("id") Long activityId,
            @Valid @RequestBody ActivityRequest activityRequest,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        Activity activity = getActivity(activityId, authenticatedUser);
        if (!activity.getCreatedBy().getId().equals(authenticatedUser.getId())
                && !authenticatedUser.getRole().name().equals("ADMIN")) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only creator or admin can edit"
            );
        }

        var subject = subjects.findById(activityRequest.subjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        if (!subject.getClassroom().getId().equals(authenticatedUser.getClassroom().getId())) {
            throw new BusinessException("Subject must belong to your classroom");
        }

        activity.setTitle(activityRequest.title().trim());
        activity.setDescription(activityRequest.description());
        activity.setSubject(subject);
        activity.setDueDate(
                activityRequest.deadlineMode() == DeadlineMode.NEXT_CLASS
                        ? nextClassService.date(
                                nextClassService.next(
                                        subject.getId(),
                                        authenticatedUser.getClassroom().getId()
                                )
                        )
                        : activityRequest.dueDate()
        );
        return ActivityResponse.of(
                activities.save(activity),
                completions.findByUserIdAndActivityId(
                        authenticatedUser.getId(),
                        activityId
                ).orElse(null)
        );
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable("id") Long activityId,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        Activity activity = getActivity(activityId, authenticatedUser);
        if (!activity.getCreatedBy().getId().equals(authenticatedUser.getId())
                && !authenticatedUser.getRole().name().equals("ADMIN")) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only creator or admin can delete"
            );
        }
        activity.setActive(false);
        activities.save(activity);
    }

    private Activity getActivity(Long activityId, User authenticatedUser) {
        var activity = activities.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
        if (!activity.getClassroom().getId().equals(authenticatedUser.getClassroom().getId())) {
            throw new ResourceNotFoundException("Activity not found");
        }
        return activity;
    }
}
