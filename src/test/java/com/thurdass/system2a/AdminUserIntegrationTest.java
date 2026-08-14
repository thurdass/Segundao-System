package com.thurdass.system2a;

import com.thurdass.system2a.entity.Classroom;
import com.thurdass.system2a.entity.User;
import com.thurdass.system2a.enums.Role;
import com.thurdass.system2a.repository.ClassroomRepository;
import com.thurdass.system2a.repository.UserRepository;
import com.thurdass.system2a.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminUserIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ClassroomRepository classroomRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    @Test
    void administratorCanReadUserDetailsWithoutSensitiveFields() throws Exception {
        Classroom classroom = classroomRepository.save(new Classroom("Admin details", "Informática", 2026, "Manhã"));
        User admin = user("details-admin", classroom, Role.ADMIN);
        User target = user("details-target", classroom, Role.STUDENT);

        mockMvc.perform(get("/api/admin/users/" + target.getId())
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(target.getId()))
                .andExpect(jsonPath("$.username").value(target.getUsername()))
                .andExpect(jsonPath("$.displayName").value("details-target"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.classroomId").value(classroom.getId()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void protectsIndividualUserEndpoint() throws Exception {
        Classroom classroom = classroomRepository.save(new Classroom("Admin protection", "Informática", 2026, "Manhã"));
        User admin = user("protection-admin", classroom, Role.ADMIN);
        User student = user("protection-student", classroom, Role.STUDENT);

        mockMvc.perform(get("/api/admin/users/" + student.getId())
                        .header("Authorization", bearer(student)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/users/999999")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/admin/users/" + student.getId()))
                .andExpect(status().isUnauthorized());
    }

    private User user(String suffix, Classroom classroom, Role role) {
        User user = new User(suffix + UUID.randomUUID().toString().replace("-", "").substring(0, 8), passwordEncoder.encode("password123"), suffix, classroom);
        user.setRole(role);
        return userRepository.save(user);
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generate(user);
    }
}
