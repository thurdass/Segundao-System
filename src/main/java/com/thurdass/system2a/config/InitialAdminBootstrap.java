package com.thurdass.system2a.config;

import com.thurdass.system2a.entity.Classroom;
import com.thurdass.system2a.entity.User;
import com.thurdass.system2a.enums.Role;
import com.thurdass.system2a.repository.ClassroomRepository;
import com.thurdass.system2a.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
@Order(2)
public class InitialAdminBootstrap implements CommandLineRunner {
    private final UserRepository users;
    private final ClassroomRepository classrooms;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String username;
    private final String password;
    private final String displayName;

    public InitialAdminBootstrap(
            UserRepository users,
            ClassroomRepository classrooms,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.initial-admin.enabled:false}") boolean enabled,
            @Value("${app.bootstrap.initial-admin.username:}") String username,
            @Value("${app.bootstrap.initial-admin.password:}") String password,
            @Value("${app.bootstrap.initial-admin.display-name:Initial Administrator}") String displayName
    ) {
        this.users = users;
        this.classrooms = classrooms;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        validateConfiguration();

        if (users.countByRole(Role.ADMIN) > 0) {
            return;
        }

        String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
        if (users.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new IllegalStateException(
                    "Initial admin username is already used by another account"
            );
        }

        Classroom classroom = classrooms.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot create initial admin without a classroom"
                ));

        User admin = new User(
                normalizedUsername,
                passwordEncoder.encode(password),
                displayName.trim(),
                classroom
        );
        admin.setRole(Role.ADMIN);
        admin.setMustChangePassword(true);
        users.save(admin);
    }

    private void validateConfiguration() {
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("Initial admin username is not configured");
        }

        if (password == null || password.length() < 8) {
            throw new IllegalStateException(
                    "Initial admin password must contain at least 8 characters"
            );
        }

        if (displayName == null || displayName.isBlank()) {
            throw new IllegalStateException("Initial admin display name is not configured");
        }
    }
}
