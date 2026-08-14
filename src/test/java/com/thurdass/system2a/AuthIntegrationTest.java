package com.thurdass.system2a;

import com.thurdass.system2a.entity.Classroom;
import com.thurdass.system2a.entity.User;
import com.thurdass.system2a.enums.Role;
import com.thurdass.system2a.repository.ClassroomRepository;
import com.thurdass.system2a.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {
    private static final String PASSWORD = "password123";
    private static final Pattern TOKEN = Pattern.compile("\"token\":\"([^\"]+)\"");

    @Autowired MockMvc mockMvc;
    @Autowired ClassroomRepository classroomRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void registersNormalizedUsernameWithEncryptedPasswordAndDefaultRole() throws Exception {
        String username = unique("Arthur");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(registerJson(username, "Arthur Silva", classroom().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(blankOrNullString())))
                .andExpect(jsonPath("$.user.username").value(username.toLowerCase()))
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.user.classroomId").value(classroom().getId()))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andReturn();

        assertFalse(result.getResponse().getContentAsString().contains(PASSWORD));
        User saved = userRepository.findByUsernameIgnoreCase(username).orElseThrow();
        assertEquals(username.toLowerCase(), saved.getUsername());
        assertTrue(passwordEncoder.matches(PASSWORD, saved.getPassword()));
        assertNotEquals(PASSWORD, saved.getPassword());
        assertEquals(Role.STUDENT, saved.getRole());
        assertEquals(classroom().getId(), saved.getClassroom().getId());
    }

    @Test
    void rejectsDuplicateUsernameAndInvalidRegistrationData() throws Exception {
        String username = unique("duplicate");
        register(username);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(registerJson(username.toUpperCase(), "Outra Pessoa", classroom().getId())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(registerJson(" ", "Pessoa", classroom().getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logsInWithValidCredentialsAndAcceptsUsernameCaseVariation() throws Exception {
        String username = unique("caseuser");
        register(username);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginJson(username.toUpperCase(), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(blankOrNullString())))
                .andExpect(jsonPath("$.user.username").value(username.toLowerCase()))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andReturn();

        assertFalse(token(result).isBlank());
    }

    @Test
    void rejectsWrongPasswordUnknownUserAndDisabledUser() throws Exception {
        String username = unique("loginuser");
        register(username);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginJson(username, "wrong-password")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginJson(unique("unknown"), PASSWORD)))
                .andExpect(status().isUnauthorized());

        User disabled = new User(unique("disabled"), passwordEncoder.encode(PASSWORD), "Disabled", classroom());
        disabled.setEnabled(false);
        userRepository.save(disabled);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginJson(disabled.getUsername(), PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanReadOwnProfileOnlyWithoutPassword() throws Exception {
        String username = unique("profile");
        String token = token(register(username));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username.toLowerCase()))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized());
    }

    private MvcResult register(String username) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(registerJson(username, "Test User", classroom().getId())))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String token(MvcResult result) throws Exception {
        Matcher matcher = TOKEN.matcher(result.getResponse().getContentAsString());
        assertTrue(matcher.find(), "Registration/login response must contain a JWT");
        return matcher.group(1);
    }

    private Classroom classroom() {
        return classroomRepository.findAll().stream().findFirst().orElseThrow();
    }

    private String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String registerJson(String username, String displayName, Long classroomId) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\",\"displayName\":\"" + displayName + "\",\"classroomId\":" + classroomId + "}";
    }

    private String loginJson(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }
}
