package com.thurdass.system2a.service;

import com.thurdass.system2a.entity.*;
import com.thurdass.system2a.exception.BusinessException;
import com.thurdass.system2a.repository.ClassScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Service
public class NextClassService {
    final ClassScheduleRepository schedules;
    final Clock clock;

    public NextClassService(ClassScheduleRepository s) {
        this(s, Clock.system(ZoneId.of("America/Sao_Paulo")));
    }

    @Autowired
    public NextClassService(ClassScheduleRepository s, Clock c) {
        schedules = s;
        clock = c;
    }

    public ClassSchedule next(Long subjectId, Long classroomId) {
        var now = ZonedDateTime.now(clock);
        return schedules.findBySubjectIdAndClassroomId(subjectId, classroomId).stream().filter(x -> {
            int d = (x.getDayOfWeek().getValue() - now.getDayOfWeek().getValue() + 7) % 7;
            return d > 0 || x.getStartTime().isAfter(now.toLocalTime());
        }).min(Comparator.comparingLong(x -> {
            int d = (x.getDayOfWeek().getValue() - now.getDayOfWeek().getValue() + 7) % 7;
            return d * 1440L + x.getStartTime().toSecondOfDay() / 60;
        })).orElseThrow(() -> new BusinessException("No next class configured for this subject"));
    }

    public LocalDate date(ClassSchedule x) {
        var n = LocalDate.now(clock);
        return n.plusDays((x.getDayOfWeek().getValue() - n.getDayOfWeek().getValue() + 7) % 7);
    }
}
