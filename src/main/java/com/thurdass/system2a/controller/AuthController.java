package com.thurdass.system2a.controller;

import com.thurdass.system2a.dto.request.LoginRequest;
import com.thurdass.system2a.dto.request.PasswordChangeRequest;
import com.thurdass.system2a.dto.response.AuthResponse;
import com.thurdass.system2a.dto.response.UserResponse;
import com.thurdass.system2a.entity.User;
import com.thurdass.system2a.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal User authenticatedUser) {
        return UserResponse.from(authenticatedUser);
    }

    @PatchMapping("/password")
    public UserResponse changePassword(
            @Valid @RequestBody PasswordChangeRequest passwordChangeRequest,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        return authService.changePassword(authenticatedUser, passwordChangeRequest);
    }
}
