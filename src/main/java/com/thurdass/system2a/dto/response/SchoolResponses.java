package com.thurdass.system2a.dto.response;

import com.thurdass.system2a.entity.*;

import java.time.*;
import java.util.*;

public final class SchoolResponses {
    private SchoolResponses() {
    }

    public record SubjectView(Long id, String name, String shortName, Long classroomId) {
        public static SubjectView of(Subject subject) {
            return new SubjectView(
                    subject.getId(),
                    subject.getName(),
                    subject.getShortName(),
                    subject.getClassroom().getId()
            );
        }
    }

    public record TeacherView(Long id, String name, String email, boolean active) {
        public static TeacherView of(Teacher teacher) {
            return new TeacherView(teacher.getId(), teacher.getName(), teacher.getEmail(), teacher.isActive());
        }
    }

    public record ScheduleView(Long id, Long classroomId, Long subjectId, Long teacherId, DayOfWeek dayOfWeek,
                               LocalTime startTime, LocalTime endTime) {
        public static ScheduleView of(ClassSchedule schedule) {
            return new ScheduleView(
                    schedule.getId(),
                    schedule.getClassroom().getId(),
                    schedule.getSubject().getId(),
                    schedule.getTeacher() == null ? null : schedule.getTeacher().getId(),
                    schedule.getDayOfWeek(),
                    schedule.getStartTime(),
                    schedule.getEndTime()
            );
        }
    }
}
