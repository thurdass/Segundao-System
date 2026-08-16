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
    final ClassScheduleRepository scheduleRepository;
    final Clock clock;

    public NextClassService(ClassScheduleRepository scheduleRepository) {
        this(scheduleRepository, Clock.system(ZoneId.of("America/Sao_Paulo")));
    }

    @Autowired
    public NextClassService(ClassScheduleRepository scheduleRepository, Clock clock) {
        this.scheduleRepository = scheduleRepository;
        this.clock = clock;
    }

    public ClassSchedule next(Long subjectId, Long classroomId) {
        var currentDateTime = ZonedDateTime.now(clock);
        return scheduleRepository.findBySubjectIdAndClassroomId(subjectId, classroomId)
                .stream()
                .filter(schedule -> {
                    int daysUntilClass = (schedule.getDayOfWeek().getValue()
                            - currentDateTime.getDayOfWeek().getValue() + 7) % 7;
                    return daysUntilClass > 0
                            || schedule.getStartTime().isAfter(currentDateTime.toLocalTime());
                })
                .min(Comparator.comparingLong(schedule -> {
                    int daysUntilClass = (schedule.getDayOfWeek().getValue()
                            - currentDateTime.getDayOfWeek().getValue() + 7) % 7;
                    return daysUntilClass * 1440L
                            + schedule.getStartTime().toSecondOfDay() / 60;
                }))
                .orElseThrow(() -> new BusinessException("No next class configured for this subject"));
    }

    public LocalDate date(ClassSchedule classSchedule) {
        var currentDate = LocalDate.now(clock);
        return currentDate.plusDays(
                (classSchedule.getDayOfWeek().getValue()
                        - currentDate.getDayOfWeek().getValue() + 7) % 7
        );
    }
}
