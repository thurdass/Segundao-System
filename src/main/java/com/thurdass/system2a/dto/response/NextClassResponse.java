package com.thurdass.system2a.dto.response;

import com.thurdass.system2a.entity.ClassSchedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public record NextClassResponse(Long subjectId, String subjectName, LocalDate nextClassDate,
                                DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
    public static NextClassResponse of(Long subjectId, String subjectName, LocalDate date, ClassSchedule schedule) {
        return new NextClassResponse(subjectId, subjectName, date, schedule.getDayOfWeek(), schedule.getStartTime(), schedule.getEndTime());
    }
}
