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
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository users,
            ClassroomRepository classrooms,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.users = users;
        this.classrooms = classrooms;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse createByAdmin(AdminUserRequest adminUserRequest) {
        String normalizedUsername = normalizeUsername(adminUserRequest.username());
        if (users.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new BusinessException("Username already in use");
        }

        var classroom = classrooms.findById(adminUserRequest.classroomId())
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));
        Role role = resolveRole(adminUserRequest.role());
        if (role != Role.STUDENT) {
            throw new BusinessException("Only STUDENT users can be created by an administrator");
        }

        User newUser = new User(
                normalizedUsername,
                passwordEncoder.encode(adminUserRequest.password()),
                adminUserRequest.displayName().trim(),
                classroom
        );
        newUser.setRole(role);
        newUser.setMustChangePassword(true);

        return UserResponse.from(users.save(newUser));
    }

    public AuthResponse login(LoginRequest loginRequest) {
        String username = normalizeUsername(loginRequest.username());
        User authenticatedUser = users.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, loginRequest.password())
        );
        return new AuthResponse(
                jwtService.generate(authenticatedUser),
                UserResponse.from(authenticatedUser),
                authenticatedUser.isMustChangePassword()
        );
    }

    @Transactional
    public UserResponse changePassword(User authenticatedUser, PasswordChangeRequest passwordChangeRequest) {
        if (!passwordEncoder.matches(passwordChangeRequest.currentPassword(), authenticatedUser.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }
        if (passwordEncoder.matches(passwordChangeRequest.newPassword(), authenticatedUser.getPassword())) {
            throw new BusinessException("New password must be different");
        }

        authenticatedUser.setPassword(passwordEncoder.encode(passwordChangeRequest.newPassword()));
        authenticatedUser.setMustChangePassword(false);
        return UserResponse.from(users.save(authenticatedUser));
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private Role resolveRole(String requestedRole) {
        if (requestedRole == null || requestedRole.isBlank()) {
            return Role.STUDENT;
        }

        try {
            return Role.valueOf(requestedRole.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("Invalid role");
        }
    }
}
