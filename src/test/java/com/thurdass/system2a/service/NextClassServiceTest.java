package com.thurdass.system2a.service;

import com.thurdass.system2a.entity.ClassSchedule;
import com.thurdass.system2a.exception.BusinessException;
import com.thurdass.system2a.repository.ClassScheduleRepository;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NextClassServiceTest {
    private static final ZoneId SAO_PAULO_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final ZonedDateTime FIXED_CURRENT_TIME =
            ZonedDateTime.of(2026, 8, 13, 10, 0, 0, 0, SAO_PAULO_ZONE);

    @Test
    void selectsLaterClassOnTheCurrentDayAndIgnoresEarlierClass() {
        ClassSchedule earlier = schedule(DayOfWeek.THURSDAY, LocalTime.of(9, 0));
        ClassSchedule later = schedule(DayOfWeek.THURSDAY, LocalTime.of(11, 0));
        ClassSchedule tomorrow = schedule(DayOfWeek.FRIDAY, LocalTime.of(7, 30));
        NextClassService service = serviceWith(earlier, tomorrow, later);

        assertSame(later, service.next(1L, 1L));
        assertEquals(LocalDate.of(2026, 8, 13), service.date(later));
    }

    @Test
    void handlesWeekRolloverAndMultipleOccurrences() {
        ClassSchedule mondayMorning = schedule(DayOfWeek.MONDAY, LocalTime.of(7, 30));
        ClassSchedule wednesday = schedule(DayOfWeek.WEDNESDAY, LocalTime.of(8, 0));
        ClassSchedule mondayAfternoon = schedule(DayOfWeek.MONDAY, LocalTime.of(14, 0));
        NextClassService service = serviceWith(mondayAfternoon, wednesday, mondayMorning);

        assertSame(mondayMorning, service.next(1L, 1L));
        assertEquals(LocalDate.of(2026, 8, 17), service.date(mondayMorning));
        assertEquals(DayOfWeek.MONDAY, service.next(1L, 1L).getDayOfWeek());
    }

    @Test
    void rejectsSubjectWithoutSchedule() {
        ClassScheduleRepository scheduleRepository = mock(ClassScheduleRepository.class);
        when(scheduleRepository.findBySubjectIdAndClassroomId(1L, 1L)).thenReturn(List.of());

        assertThrows(
                BusinessException.class,
                () -> new NextClassService(scheduleRepository, fixedClock()).next(1L, 1L)
        );
    }

    private NextClassService serviceWith(ClassSchedule... schedules) {
        ClassScheduleRepository scheduleRepository = mock(ClassScheduleRepository.class);
        when(scheduleRepository.findBySubjectIdAndClassroomId(1L, 1L)).thenReturn(List.of(schedules));
        return new NextClassService(scheduleRepository, fixedClock());
    }

    private Clock fixedClock() {
        return Clock.fixed(FIXED_CURRENT_TIME.toInstant(), SAO_PAULO_ZONE);
    }

    private ClassSchedule schedule(DayOfWeek dayOfWeek, LocalTime startTime) {
        ClassSchedule schedule = new ClassSchedule();
        schedule.setDayOfWeek(dayOfWeek);
        schedule.setStartTime(startTime);
        schedule.setEndTime(startTime.plusMinutes(50));
        return schedule;
    }
}
