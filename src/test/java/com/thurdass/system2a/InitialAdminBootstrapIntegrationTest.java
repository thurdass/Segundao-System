package com.thurdass.system2a;

import com.thurdass.system2a.entity.User;
import com.thurdass.system2a.enums.Role;
import com.thurdass.system2a.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "app.bootstrap.initial-admin.enabled=true",
        "app.bootstrap.initial-admin.username=bootstrap-admin",
        "app.bootstrap.initial-admin.password=bootstrap-password",
        "app.bootstrap.initial-admin.display-name=Bootstrap Administrator"
})
class InitialAdminBootstrapIntegrationTest {
    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void createsInitialAdminWithBcryptAndMandatoryPasswordChange() {
        User admin = userRepository.findByUsernameIgnoreCase("bootstrap-admin")
                .orElseThrow();

        assertEquals(Role.ADMIN, admin.getRole());
        assertTrue(admin.isEnabled());
        assertTrue(admin.isMustChangePassword());
        assertTrue(passwordEncoder.matches("bootstrap-password", admin.getPassword()));
        assertEquals(1, userRepository.countByRole(Role.ADMIN));
    }
}
