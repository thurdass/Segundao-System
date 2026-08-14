package com.thurdass.system2a;

import com.thurdass.system2a.entity.Classroom;
import com.thurdass.system2a.entity.User;
import com.thurdass.system2a.enums.Role;
import com.thurdass.system2a.repository.ClassroomRepository;
import com.thurdass.system2a.repository.UserRepository;
import com.thurdass.system2a.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ManagedUserOnboardingIntegrationTest {
    private static final String INITIAL_PASSWORD = "initial123";
    private static final String NEW_PASSWORD = "updated123";
    private static final String ADMIN_PASSWORD = "adminpass123";
    private static final Pattern TOKEN = Pattern.compile("\"token\":\"([^\"]+)\"");

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ClassroomRepository classroomRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    JwtService jwtService;

    @Test
    void forcedUserCanUseOnlyProfileAndPasswordEndpointsUntilPasswordChange() throws Exception {
        Classroom classroom = classroom("restricted-access");
        User admin = user("restricted-admin", classroom, Role.ADMIN, ADMIN_PASSWORD);
        String username = unique("restricted");

        createManagedUser(admin, username, classroom.getId());
        String token = loginToken(username, INITIAL_PASSWORD);

        mockMvc.perform(get("/api/auth/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true));

        mockMvc.perform(get("/api/activities").header("Authorization", token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/announcements").header("Authorization", token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/subjects")
                        .param("classroomId", classroom.getId().toString())
                        .header("Authorization", token))
                .andExpect(status().isForbidden());

        changePassword(token, INITIAL_PASSWORD, NEW_PASSWORD);

        mockMvc.perform(get("/api/activities").header("Authorization", token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/announcements").header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    void passwordChangeValidatesCurrentPasswordAndPersistsNewBcryptPassword() throws Exception {
        Classroom classroom = classroom("password-change");
        User admin = user("password-admin", classroom, Role.ADMIN, ADMIN_PASSWORD);
        String username = unique("password-user");

        createManagedUser(admin, username, classroom.getId());
        String token = loginToken(username, INITIAL_PASSWORD);

        mockMvc.perform(patch("/api/auth/password")
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content(passwordChangeJson("wrong-password", NEW_PASSWORD)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/auth/password")
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content(passwordChangeJson(INITIAL_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andExpect(jsonPath("$.password").doesNotExist());

        User saved = userRepository.findByUsernameIgnoreCase(username).orElseThrow();
        assertTrue(passwordEncoder.matches(NEW_PASSWORD, saved.getPassword()));
        assertFalse(passwordEncoder.matches(INITIAL_PASSWORD, saved.getPassword()));
        assertFalse(saved.isMustChangePassword());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginJson(username, INITIAL_PASSWORD)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginJson(username, NEW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    @Test
    void authenticatedUserCannotChangeAnotherUsersPassword() throws Exception {
        Classroom classroom = classroom("other-password");
        User admin = user("other-password-admin", classroom, Role.ADMIN, ADMIN_PASSWORD);
        User otherUser = user("other-password-user", classroom, Role.STUDENT, NEW_PASSWORD);
        String username = unique("password-target");

        createManagedUser(admin, username, classroom.getId());
        String otherToken = bearer(otherUser);

        mockMvc.perform(patch("/api/auth/password")
                        .header("Authorization", otherToken)
                        .contentType(APPLICATION_JSON)
                        .content(passwordChangeJson(INITIAL_PASSWORD, "attacker123")))
                .andExpect(status().isBadRequest());

        User target = userRepository.findByUsernameIgnoreCase(username).orElseThrow();
        assertTrue(passwordEncoder.matches(INITIAL_PASSWORD, target.getPassword()));
        assertTrue(target.isMustChangePassword());
    }

    private void createManagedUser(User admin, String username, Long classroomId) throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(adminUserJson(username, classroomId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    private void changePassword(String token, String currentPassword, String newPassword) throws Exception {
        mockMvc.perform(patch("/api/auth/password")
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content(passwordChangeJson(currentPassword, newPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    private String loginToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginJson(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andReturn();

        Matcher matcher = TOKEN.matcher(result.getResponse().getContentAsString());
        assertTrue(matcher.find(), "Authentication response must contain a JWT");
        return "Bearer " + matcher.group(1);
    }

    private User user(String username, Classroom classroom, Role role, String password) {
        User user = new User(username, passwordEncoder.encode(password), username, classroom);
        user.setRole(role);
        return userRepository.save(user);
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generate(user);
    }

    private Classroom classroom(String suffix) {
        return classroomRepository.save(
                new Classroom("2A " + suffix, "Informática", 2026, "Manhã")
        );
    }

    private String adminUserJson(String username, Long classroomId) {
        return "{\"username\":\"" + username
                + "\",\"password\":\"" + INITIAL_PASSWORD
                + "\",\"displayName\":\"Managed User\",\"classroomId\":"
                + classroomId + "}";
    }

    private String passwordChangeJson(String currentPassword, String newPassword) {
        return "{\"currentPassword\":\"" + currentPassword
                + "\",\"newPassword\":\"" + newPassword + "\"}";
    }

    private String loginJson(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
