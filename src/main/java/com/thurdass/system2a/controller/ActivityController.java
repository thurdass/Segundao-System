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
    final NextClassService next;

    public ActivityController(ActivityRepository a, CompletionRepository c, SubjectRepository s, NextClassService n) {
        activities = a;
        completions = c;
        subjects = s;
        next = n;
    }

    @GetMapping
    public List<ActivityResponse> list(@AuthenticationPrincipal User u, @RequestParam(required = false) String status, @RequestParam(required = false) Long subjectId, @RequestParam(required = false) LocalDate dueBefore) {
        return activities.findByClassroomIdAndActiveTrueOrderByDueDateAsc(u.getClassroom().getId()).stream().filter(a -> subjectId == null || a.getSubject().getId().equals(subjectId)).filter(a -> dueBefore == null || !a.getDueDate().isAfter(dueBefore)).filter(a -> {
            boolean done = completions.existsByUserIdAndActivityId(u.getId(), a.getId());
            return status == null || status.equalsIgnoreCase(done ? "completed" : "pending");
        }).map(a -> ActivityResponse.of(a, completions.findByUserIdAndActivityId(u.getId(), a.getId()).orElse(null))).toList();
    }

    @GetMapping("/{id}")
    public ActivityResponse get(@PathVariable Long id, @AuthenticationPrincipal User u) {
        var a = activities.findById(id).orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
        if (!a.getClassroom().getId().equals(u.getClassroom().getId()))
            throw new ResourceNotFoundException("Activity not found");
        return ActivityResponse.of(a, completions.findByUserIdAndActivityId(u.getId(), id).orElse(null));
    }

    @PostMapping
    public ActivityResponse add(@Valid @RequestBody ActivityRequest r, @AuthenticationPrincipal User u) {
        var s = subjects.findById(r.subjectId()).orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        if (!s.getClassroom().getId().equals(u.getClassroom().getId()))
            throw new BusinessException("Activity must belong to your classroom");
        LocalDate due = r.dueDate();
        if (r.deadlineMode() == DeadlineMode.NEXT_CLASS)
            due = next.date(next.next(s.getId(), u.getClassroom().getId()));
        if (due == null || due.isBefore(LocalDate.now()))
            throw new BusinessException("Due date must be today or later");
        var a = new Activity();
        a.setTitle(r.title().trim());
        a.setDescription(r.description());
        a.setDueDate(due);
        a.setSubject(s);
        a.setClassroom(u.getClassroom());
        a.setCreatedBy(u);
        return ActivityResponse.of(activities.save(a), null);
    }

    @PostMapping("/{id}/complete")
    public ActivityResponse complete(@PathVariable Long id, @AuthenticationPrincipal User u) {
        var a = getActivity(id, u);
        if (completions.existsByUserIdAndActivityId(u.getId(), id))
            throw new BusinessException("Activity already completed");
        var c = new ActivityCompletion();
        c.setActivity(a);
        c.setUser(u);
        return ActivityResponse.of(a, completions.save(c));
    }

    @DeleteMapping("/{id}/complete")
    public void uncomplete(@PathVariable Long id, @AuthenticationPrincipal User u) {
        getActivity(id, u);
        var completion = completions.findByUserIdAndActivityId(u.getId(), id).orElseThrow(() -> new BusinessException("Activity is not completed by this user"));
        completions.delete(completion);
    }

    @PutMapping("/{id}")
    public ActivityResponse edit(@PathVariable Long id, @Valid @RequestBody ActivityRequest r, @AuthenticationPrincipal User u) {
        var a = getActivity(id, u);
        if (!a.getCreatedBy().getId().equals(u.getId()) && !u.getRole().name().equals("ADMIN"))
            throw new org.springframework.security.access.AccessDeniedException("Only creator or admin can edit");
        var s = subjects.findById(r.subjectId()).orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        if (!s.getClassroom().getId().equals(u.getClassroom().getId()))
            throw new BusinessException("Subject must belong to your classroom");
        a.setTitle(r.title().trim());
        a.setDescription(r.description());
        a.setSubject(s);
        a.setDueDate(r.deadlineMode() == DeadlineMode.NEXT_CLASS ? next.date(next.next(s.getId(), u.getClassroom().getId())) : r.dueDate());
        return ActivityResponse.of(activities.save(a), completions.findByUserIdAndActivityId(u.getId(), id).orElse(null));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal User u) {
        var a = getActivity(id, u);
        if (!a.getCreatedBy().getId().equals(u.getId()) && !u.getRole().name().equals("ADMIN"))
            throw new org.springframework.security.access.AccessDeniedException("Only creator or admin can delete");
        a.setActive(false);
        activities.save(a);
    }

    private Activity getActivity(Long id, User u) {
        var a = activities.findById(id).orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
        if (!a.getClassroom().getId().equals(u.getClassroom().getId()))
            throw new ResourceNotFoundException("Activity not found");
        return a;
    }
}
