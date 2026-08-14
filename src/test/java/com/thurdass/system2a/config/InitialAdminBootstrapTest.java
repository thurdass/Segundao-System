package com.thurdass.system2a.config;

import com.thurdass.system2a.entity.Classroom;
import com.thurdass.system2a.entity.User;
import com.thurdass.system2a.enums.Role;
import com.thurdass.system2a.repository.ClassroomRepository;
import com.thurdass.system2a.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitialAdminBootstrapTest {
    @Mock
    UserRepository users;

    @Mock
    ClassroomRepository classrooms;

    @Mock
    PasswordEncoder passwordEncoder;

    @Test
    void doesNothingWhenBootstrapIsDisabled() {
        InitialAdminBootstrap bootstrap = bootstrap(false, "", "", "");

        bootstrap.run();

        verifyNoInteractions(users, classrooms, passwordEncoder);
    }

    @Test
    void createsBcryptAdminWithMandatoryPasswordChangeWhenNoAdminExists() {
        Classroom classroom = new Classroom("2A", "Informática", 2026, "Manhã");
        when(users.countByRole(Role.ADMIN)).thenReturn(0L);
        when(users.existsByUsernameIgnoreCase("thurdas")).thenReturn(false);
        when(classrooms.findAll()).thenReturn(List.of(classroom));
        when(passwordEncoder.encode("test-password")).thenReturn("bcrypt-hash");

        InitialAdminBootstrap bootstrap = bootstrap(
                true,
                "  Thurdas ",
                "test-password",
                "Initial Admin"
        );

        bootstrap.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        User saved = captor.getValue();

        assertEquals("thurdas", saved.getUsername());
        assertEquals("bcrypt-hash", saved.getPassword());
        assertEquals("Initial Admin", saved.getDisplayName());
        assertEquals(Role.ADMIN, saved.getRole());
        assertTrue(saved.isEnabled());
        assertTrue(saved.isMustChangePassword());
        assertEquals(classroom, saved.getClassroom());
        verify(passwordEncoder).encode("test-password");
    }

    @Test
    void doesNotCreateAnotherAdminWhenOneAlreadyExists() {
        when(users.countByRole(Role.ADMIN)).thenReturn(1L);

        InitialAdminBootstrap bootstrap = bootstrap(
                true,
                "thurdas",
                "test-password",
                "Initial Admin"
        );

        bootstrap.run();

        verify(users).countByRole(Role.ADMIN);
        verify(users, never()).save(org.mockito.ArgumentMatchers.any(User.class));
        verifyNoInteractions(classrooms, passwordEncoder);
    }

    @Test
    void rejectsEnabledBootstrapWithoutInitialCredentials() {
        InitialAdminBootstrap bootstrap = bootstrap(true, "", "", "Initial Admin");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                bootstrap::run
        );

        assertEquals("Initial admin username is not configured", exception.getMessage());
        verifyNoInteractions(users, classrooms, passwordEncoder);
    }

    private InitialAdminBootstrap bootstrap(
            boolean enabled,
            String username,
            String password,
            String displayName
    ) {
        return new InitialAdminBootstrap(
                users,
                classrooms,
                passwordEncoder,
                enabled,
                username,
                password,
                displayName
        );
    }
}
