package com.thurdass.system2a.service;

import com.thurdass.system2a.dto.request.AdminUserRequest;
import com.thurdass.system2a.dto.request.LoginRequest;
import com.thurdass.system2a.dto.request.PasswordChangeRequest;
import com.thurdass.system2a.dto.response.AuthResponse;
import com.thurdass.system2a.dto.response.UserResponse;
import com.thurdass.system2a.entity.User;
import com.thurdass.system2a.enums.Role;
import com.thurdass.system2a.exception.BusinessException;
import com.thurdass.system2a.exception.ResourceNotFoundException;
import com.thurdass.system2a.repository.ClassroomRepository;
import com.thurdass.system2a.repository.UserRepository;
import com.thurdass.system2a.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository users;
    private final ClassroomRepository classrooms;
    private final PasswordEncoder encoder;
    private final AuthenticationManager auth;
    private final JwtService jwt;

    public AuthService(UserRepository users, ClassroomRepository classrooms, PasswordEncoder encoder,
                       AuthenticationManager auth, JwtService jwt) {
        this.users = users;
        this.classrooms = classrooms;
        this.encoder = encoder;
        this.auth = auth;
        this.jwt = jwt;
    }

    @Transactional
    public UserResponse createByAdmin(AdminUserRequest request) {
        String username = normalizeUsername(request.username());
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException("Username already in use");
        }

        var classroom = classrooms.findById(request.classroomId())
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));
        Role role = resolveRole(request.role());
        if (role != Role.STUDENT) {
            throw new BusinessException("Only STUDENT users can be created by an administrator");
        }

        User user = new User(
                username,
                encoder.encode(request.password()),
                request.displayName().trim(),
                classroom
        );
        user.setRole(role);
        user.setMustChangePassword(true);

        return UserResponse.from(users.save(user));
    }

    public AuthResponse login(LoginRequest r) {
        String username = normalizeUsername(r.username());
        User user = users.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        auth.authenticate(new UsernamePasswordAuthenticationToken(username, r.password()));
        return new AuthResponse(jwt.generate(user), UserResponse.from(user), user.isMustChangePassword());
    }

    @Transactional
    public UserResponse changePassword(User user, PasswordChangeRequest request) {
        if (!encoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }
        if (encoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessException("New password must be different");
        }

        user.setPassword(encoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        return UserResponse.from(users.save(user));
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private Role resolveRole(String role) {
        if (role == null || role.isBlank()) {
            return Role.STUDENT;
        }

        try {
            return Role.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("Invalid role");
        }
    }
}
