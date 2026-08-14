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

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {
    private static final String PASSWORD = "password123";
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
    void administratorCreatesNormalizedStudentWithEncryptedInitialPassword() throws Exception {
        Classroom classroom = classroom();
        User admin = user("creation-admin", classroom, Role.ADMIN, ADMIN_PASSWORD);
        String username = unique("Arthur");

        MvcResult result = mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(adminUserJson(username, "Arthur Silva", null, classroom.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username.toLowerCase()))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.classroomId").value(classroom.getId()))
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        assertFalse(result.getResponse().getContentAsString().contains(PASSWORD));
        User saved = userRepository.findByUsernameIgnoreCase(username).orElseThrow();
        assertEquals(username.toLowerCase(), saved.getUsername());
        assertTrue(passwordEncoder.matches(PASSWORD, saved.getPassword()));
        assertNotEquals(PASSWORD, saved.getPassword());
        assertEquals(Role.STUDENT, saved.getRole());
        assertTrue(saved.isMustChangePassword());
        assertEquals(classroom.getId(), saved.getClassroom().getId());
    }

    @Test
    void studentCannotCreateUserAndUnauthenticatedRequestIsRejected() throws Exception {
        Classroom classroom = classroom();
        User student = user("creation-student", classroom, Role.STUDENT, PASSWORD);

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", bearer(student))
                        .contentType(APPLICATION_JSON)
                        .content(adminUserJson("student-created", "Student Created", "STUDENT", classroom.getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/users")
                        .contentType(APPLICATION_JSON)
                        .content(adminUserJson("anonymous-created", "Anonymous Created", "STUDENT", classroom.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicRegistrationEndpointIsNoLongerAvailable() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(adminUserJson("public-user", "Public User", "STUDENT", classroom().getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsDuplicateUsernameAndInvalidAdministrativeData() throws Exception {
        Classroom classroom = classroom();
        User admin = user("duplicate-admin", classroom, Role.ADMIN, ADMIN_PASSWORD);
        String username = unique("duplicate");

        createUser(admin, username, "First User", "STUDENT", classroom.getId());

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(adminUserJson(username.toUpperCase(), "Other User", "STUDENT", classroom.getId())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(adminUserJson(" ", "Invalid User", "STUDENT", classroom.getId())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(adminUserJson("invalid-role", "Invalid Role", "MANAGER", classroom.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void administratorCreatedUserCanLogInAndResponseRequiresPasswordChange() throws Exception {
        Classroom classroom = classroom();
        User admin = user("login-admin", classroom, Role.ADMIN, ADMIN_PASSWORD);
        String username = unique("initial-login");

        createUser(admin, username, "Initial Login", "STUDENT", classroom.getId());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginJson(username.toUpperCase(), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(blankOrNullString())))
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andExpect(jsonPath("$.user.mustChangePassword").value(true))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andReturn();

        assertFalse(token(result).isBlank());
    }

    @Test
    void rejectsWrongPasswordUnknownUserAndDisabledUser() throws Exception {
        Classroom classroom = classroom();
        String username = unique("loginuser");
        User user = user(username, classroom, Role.STUDENT, PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginJson(username, "wrong-password")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginJson(unique("unknown"), PASSWORD)))
                .andExpect(status().isUnauthorized());

        user.setEnabled(false);
        userRepository.save(user);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginJson(username, PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanReadOwnProfileOnlyWithoutPassword() throws Exception {
        Classroom classroom = classroom();
        User user = user(unique("profile"), classroom, Role.STUDENT, PASSWORD);
        String token = bearer(user);

        mockMvc.perform(get("/api/auth/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(user.getUsername()))
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized());
    }

    private MvcResult createUser(User admin, String username, String displayName,
                                 String role, Long classroomId) throws Exception {
        return mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(adminUserJson(username, displayName, role, classroomId)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private User user(String username, Classroom classroom, Role role, String password) {
        User user = new User(username, passwordEncoder.encode(password), username, classroom);
        user.setRole(role);
        return userRepository.save(user);
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generate(user);
    }

    private String token(MvcResult result) throws Exception {
        Matcher matcher = TOKEN.matcher(result.getResponse().getContentAsString());
        assertTrue(matcher.find(), "Authentication response must contain a JWT");
        return matcher.group(1);
    }

    private Classroom classroom() {
        return classroomRepository.findAll().stream().findFirst().orElseThrow();
    }

    private String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String adminUserJson(String username, String displayName, String role, Long classroomId) {
        String roleProperty = role == null ? "" : ",\"role\":\"" + role + "\"";
        return "{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD
                + "\",\"displayName\":\"" + displayName + "\"" + roleProperty
                + ",\"classroomId\":" + classroomId + "}";
    }

    private String loginJson(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }
}
