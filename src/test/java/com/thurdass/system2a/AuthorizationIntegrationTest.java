package com.thurdass.system2a;

import com.thurdass.system2a.entity.*;
import com.thurdass.system2a.enums.Role;
import com.thurdass.system2a.repository.*;
import com.thurdass.system2a.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ClassroomRepository classroomRepository;
    @Autowired UserRepository userRepository;
    @Autowired SubjectRepository subjectRepository;
    @Autowired TeacherRepository teacherRepository;
    @Autowired ClassScheduleRepository scheduleRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    @Test
    void studentCannotChangeSchoolCatalogOrAccessAdminRoutes() throws Exception {
        Classroom classroom = classroom("student-denied");
        User student = user("student-denied", classroom, Role.STUDENT);
        String token = bearer(student);

        mockMvc.perform(post("/api/subjects")
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content(subjectJson("Denied subject", classroom.getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/teachers")
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Denied teacher\",\"subjectIds\":[]}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/schedules")
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content(scheduleJson(classroom.getId(), 1L)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/users").header("Authorization", token))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/users/1/status")
                        .header("Authorization", token)
                        .param("enabled", "false"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanManageSchoolCatalog() throws Exception {
        Classroom classroom = classroom("admin-catalog");
        User admin = user("admin-catalog", classroom, Role.ADMIN);
        User student = user("student-catalog", classroom, Role.STUDENT);

        var subjectResult = mockMvc.perform(post("/api/subjects")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(subjectJson("Web development", classroom.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Web development"))
                .andReturn();
        Subject subject = subjectRepository.findAll().stream()
                .filter(item -> item.getName().equals("Web development"))
                .findFirst().orElseThrow();

        mockMvc.perform(post("/api/teachers")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Maria Silva\",\"email\":\"maria@example.com\",\"subjectIds\":[" + subject.getId() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria Silva"));
        Teacher teacher = teacherRepository.findAll().stream()
                .filter(item -> item.getName().equals("Maria Silva"))
                .findFirst().orElseThrow();

        mockMvc.perform(put("/api/teachers/" + teacher.getId())
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Maria Souza\",\"email\":\"maria.souza@example.com\",\"subjectIds\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria Souza"));

        mockMvc.perform(post("/api/schedules")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(scheduleJson(classroom.getId(), subject.getId())))
                .andExpect(status().isOk());
        ClassSchedule schedule = scheduleRepository.findAll().stream()
                .filter(item -> item.getClassroom().getId().equals(classroom.getId()))
                .findFirst().orElseThrow();

        mockMvc.perform(delete("/api/schedules/" + schedule.getId())
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        assertTrue(scheduleRepository.findById(schedule.getId()).isEmpty());

        mockMvc.perform(post("/api/subjects")
                        .header("Authorization", bearer(student))
                        .contentType(APPLICATION_JSON)
                        .content(subjectJson("Student denied", classroom.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotPinAnnouncementButAdministratorCan() throws Exception {
        Classroom classroom = classroom("announcement-roles");
        User student = user("announcement-student", classroom, Role.STUDENT);
        User admin = user("announcement-admin", classroom, Role.ADMIN);

        mockMvc.perform(post("/api/announcements")
                        .header("Authorization", bearer(student))
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Student notice\",\"content\":\"Content\",\"pinned\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pinned").value(false));

        mockMvc.perform(post("/api/announcements")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Admin notice\",\"content\":\"Content\",\"pinned\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pinned").value(true));
    }

    @Test
    void studentCannotChangeUserStatusAndAdminCanAccessAdminArea() throws Exception {
        Classroom classroom = classroom("admin-area");
        User student = user("admin-area-student", classroom, Role.STUDENT);
        User admin = user("admin-area-admin", classroom, Role.ADMIN);
        User target = user("admin-area-target", classroom, Role.STUDENT);

        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(admin)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").exists());

        mockMvc.perform(patch("/api/admin/users/" + target.getId() + "/status")
                        .header("Authorization", bearer(student))
                        .param("enabled", "false"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/users/" + target.getId() + "/status")
                        .header("Authorization", bearer(admin))
                        .param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void protectedRoutesReturnUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/activities")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/announcements")).andExpect(status().isUnauthorized());
    }

    private Classroom classroom(String suffix) {
        return classroomRepository.save(new Classroom("Class " + suffix + unique(), "Informática", 2026, "Manhã"));
    }

    private User user(String suffix, Classroom classroom, Role role) {
        User user = new User(unique(suffix), passwordEncoder.encode("password123"), suffix, classroom);
        user.setRole(role);
        return userRepository.save(user);
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generate(user);
    }

    private String subjectJson(String name, Long classroomId) {
        return "{\"name\":\"" + name + "\",\"shortName\":\"SUB\",\"classroomId\":" + classroomId + "}";
    }

    private String scheduleJson(Long classroomId, Long subjectId) {
        return "{\"classroomId\":" + classroomId + ",\"subjectId\":" + subjectId + ",\"dayOfWeek\":\"MONDAY\",\"startTime\":\"07:30:00\",\"endTime\":\"08:20:00\"}";
    }

    private String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String unique() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }
}
