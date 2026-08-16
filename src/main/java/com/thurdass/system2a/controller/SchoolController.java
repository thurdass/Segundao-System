package com.thurdass.system2a.controller;

import com.thurdass.system2a.dto.request.*;
import com.thurdass.system2a.dto.response.NextClassResponse;
import com.thurdass.system2a.dto.response.SchoolResponses.*;
import com.thurdass.system2a.entity.*;
import com.thurdass.system2a.exception.ResourceNotFoundException;
import com.thurdass.system2a.repository.*;
import com.thurdass.system2a.service.NextClassService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class SchoolController {
    final ClassroomRepository classrooms;
    final SubjectRepository subjects;
    final TeacherRepository teachers;
    final ClassScheduleRepository schedules;
    final NextClassService nextClassService;

    public SchoolController(
            ClassroomRepository classroomRepository,
            SubjectRepository subjectRepository,
            TeacherRepository teacherRepository,
            ClassScheduleRepository scheduleRepository,
            NextClassService nextClassService
    ) {
        this.classrooms = classroomRepository;
        this.subjects = subjectRepository;
        this.teachers = teacherRepository;
        this.schedules = scheduleRepository;
        this.nextClassService = nextClassService;
    }

    @GetMapping("/subjects")
    public List<SubjectView> subjects(@RequestParam("classroomId") Long classroomId) {
        return subjects.findByClassroomIdAndActiveTrue(classroomId).stream().map(SubjectView::of).toList();
    }

    @GetMapping("/subjects/{subjectId}/next-class")
    public NextClassResponse nextClass(
            @PathVariable("subjectId") Long subjectId,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        var subject = subjects.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        if (!authenticatedUser.getRole().name().equals("ADMIN")
                && !subject.getClassroom().getId().equals(authenticatedUser.getClassroom().getId())) {
            throw new ResourceNotFoundException("Subject not found");
        }
        var nextClassSchedule = nextClassService.next(subjectId, subject.getClassroom().getId());
        return NextClassResponse.of(
                subject.getId(),
                subject.getName(),
                nextClassService.date(nextClassSchedule),
                nextClassSchedule
        );
    }

    @PostMapping("/subjects")
    @PreAuthorize("hasRole('ADMIN')")
    public SubjectView addSubject(@Valid @RequestBody SubjectRequest subjectRequest) {
        var classroom = classrooms.findById(subjectRequest.classroomId())
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));
        return SubjectView.of(subjects.save(new Subject(
                subjectRequest.name().trim(),
                subjectRequest.shortName(),
                classroom
        )));
    }

    @GetMapping("/teachers")
    public List<TeacherView> teachers() {
        return teachers.findAll().stream().map(TeacherView::of).toList();
    }

    @GetMapping("/teachers/{id}")
    public TeacherView teacher(@PathVariable("id") Long teacherId) {
        return TeacherView.of(teachers.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found")));
    }

    @PostMapping("/teachers")
    @PreAuthorize("hasRole('ADMIN')")
    public TeacherView addTeacher(@Valid @RequestBody TeacherRequest teacherRequest) {
        var teacher = new Teacher(teacherRequest.name().trim(), teacherRequest.email());
        if (teacherRequest.subjectIds() != null) {
            teacher.getSubjects().addAll(subjects.findAllById(teacherRequest.subjectIds()));
        }
        return TeacherView.of(teachers.save(teacher));
    }

    @PutMapping("/teachers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public TeacherView editTeacher(
            @PathVariable("id") Long teacherId,
            @Valid @RequestBody TeacherRequest teacherRequest
    ) {
        Teacher teacher = teachers.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));

        teacher.setName(teacherRequest.name().trim());
        teacher.setEmail(teacherRequest.email());

        if (teacherRequest.subjectIds() != null) {
            teacher.getSubjects().clear();
            teacher.getSubjects().addAll(subjects.findAllById(teacherRequest.subjectIds()));
        }

        return TeacherView.of(teachers.save(teacher));
    }

    @GetMapping("/teachers/{id}/subjects")
    @Transactional(readOnly = true)
    public List<SubjectView> teacherSubjects(@PathVariable("id") Long teacherId) {
        return teachers.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"))
                .getSubjects()
                .stream()
                .map(SubjectView::of)
                .toList();
    }

    @GetMapping("/schedules/classroom/{id}")
    public List<ScheduleView> schedule(@PathVariable("id") Long classroomId) {
        return schedules.findByClassroomIdOrderByDayOfWeekAscStartTimeAsc(classroomId)
                .stream()
                .map(ScheduleView::of)
                .toList();
    }

    @PostMapping("/schedules")
    @PreAuthorize("hasRole('ADMIN')")
    public ScheduleView addSchedule(@Valid @RequestBody ScheduleRequest scheduleRequest) {
        var classSchedule = new ClassSchedule();
        classSchedule.setClassroom(classrooms.findById(scheduleRequest.classroomId())
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found")));
        classSchedule.setSubject(subjects.findById(scheduleRequest.subjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found")));
        classSchedule.setTeacher(scheduleRequest.teacherId() == null
                ? null
                : teachers.findById(scheduleRequest.teacherId())
                        .orElseThrow(() -> new ResourceNotFoundException("Teacher not found")));
        classSchedule.setDayOfWeek(scheduleRequest.dayOfWeek());
        classSchedule.setStartTime(scheduleRequest.startTime());
        classSchedule.setEndTime(scheduleRequest.endTime());
        return ScheduleView.of(schedules.save(classSchedule));
    }

    @DeleteMapping("/schedules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteSchedule(@PathVariable("id") Long scheduleId) {
        if (!schedules.existsById(scheduleId)) {
            throw new ResourceNotFoundException("Schedule not found");
        }
        schedules.deleteById(scheduleId);
    }
}
