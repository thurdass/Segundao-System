package com.thurdass.system2a;

import com.thurdass.system2a.entity.*;
import com.thurdass.system2a.enums.Role;
import com.thurdass.system2a.repository.*;
import com.thurdass.system2a.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.*;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(NextClassEndpointIntegrationTest.FixedClockConfig.class)
class NextClassEndpointIntegrationTest {
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate DATE = LocalDate.of(2026, 8, 13);

    @Autowired MockMvc mockMvc;
    @Autowired ClassroomRepository classroomRepository;
    @Autowired UserRepository userRepository;
    @Autowired SubjectRepository subjectRepository;
    @Autowired ClassScheduleRepository scheduleRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    @Test
    void returnsNextOccurrenceAndAllScheduleFields() throws Exception {
        Classroom classroom = classroom("same-day");
        User student = user("same-day-user", classroom, Role.STUDENT);
        Subject subject = subject("Web", classroom);
        schedule(subject, classroom, DayOfWeek.THURSDAY, LocalTime.of(9, 0));
        schedule(subject, classroom, DayOfWeek.THURSDAY, LocalTime.of(11, 0));
        schedule(subject, classroom, DayOfWeek.FRIDAY, LocalTime.of(8, 0));

        mockMvc.perform(get("/api/subjects/" + subject.getId() + "/next-class")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectId").value(subject.getId()))
                .andExpect(jsonPath("$.subjectName").value(subject.getName()))
                .andExpect(jsonPath("$.nextClassDate").value(DATE.toString()))
                .andExpect(jsonPath("$.dayOfWeek").value("THURSDAY"))
                .andExpect(jsonPath("$.startTime").value("11:00:00"))
                .andExpect(jsonPath("$.endTime").value("11:50:00"));
    }

    @Test
    void skipsPassedCurrentDayAndUsesFollowingWeekWhenNecessary() throws Exception {
        Classroom classroom = classroom("week-rollover");
        User student = user("week-rollover-user", classroom, Role.STUDENT);
        Subject subject = subject("Mathematics", classroom);
        schedule(subject, classroom, DayOfWeek.THURSDAY, LocalTime.of(9, 0));
        schedule(subject, classroom, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0));

        mockMvc.perform(get("/api/subjects/" + subject.getId() + "/next-class")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextClassDate").value(DATE.plusDays(6).toString()))
                .andExpect(jsonPath("$.dayOfWeek").value("WEDNESDAY"));
    }

    @Test
    void rejectsMissingSubjectScheduleAndDifferentClassroom() throws Exception {
        Classroom own = classroom("errors-own");
        Classroom other = classroom("errors-other");
        User student = user("errors-user", own, Role.STUDENT);
        Subject withoutSchedule = subject("No schedule", own);
        Subject otherSubject = subject("Other classroom", other);

        mockMvc.perform(get("/api/subjects/" + withoutSchedule.getId() + "/next-class")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No next class configured for this subject"));

        mockMvc.perform(get("/api/subjects/" + otherSubject.getId() + "/next-class")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/subjects/999999/next-class")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isNotFound());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/subjects/1/next-class"))
                .andExpect(status().isUnauthorized());
    }

    private ClassSchedule schedule(Subject subject, Classroom classroom, DayOfWeek day, LocalTime start) {
        ClassSchedule schedule = new ClassSchedule();
        schedule.setSubject(subject);
        schedule.setClassroom(classroom);
        schedule.setDayOfWeek(day);
        schedule.setStartTime(start);
        schedule.setEndTime(start.plusMinutes(50));
        return scheduleRepository.save(schedule);
    }

    private Classroom classroom(String suffix) {
        return classroomRepository.save(new Classroom("Class " + suffix + unique(), "Informática", 2026, "Manhã"));
    }

    private User user(String suffix, Classroom classroom, Role role) {
        User user = new User(unique(suffix), passwordEncoder.encode("password123"), suffix, classroom);
        user.setRole(role);
        return userRepository.save(user);
    }

    private Subject subject(String name, Classroom classroom) {
        return subjectRepository.save(new Subject(name + unique(), "SUB", classroom));
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generate(user);
    }

    private String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String unique() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(ZonedDateTime.of(DATE, LocalTime.of(10, 0), ZONE).toInstant(), ZONE);
        }
    }
}
