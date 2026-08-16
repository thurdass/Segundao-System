package com.thurdass.system2a.controller;

import com.thurdass.system2a.dto.request.AdminUserRequest;
import com.thurdass.system2a.dto.response.UserResponse;
import com.thurdass.system2a.entity.User;
import com.thurdass.system2a.exception.ResourceNotFoundException;
import com.thurdass.system2a.repository.ActivityRepository;
import com.thurdass.system2a.repository.AnnouncementRepository;
import com.thurdass.system2a.repository.UserRepository;
import com.thurdass.system2a.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserRepository users;
    private final ActivityRepository activities;
    private final AnnouncementRepository announcements;
    private final AuthService authService;

    public AdminController(
            UserRepository userRepository,
            ActivityRepository activityRepository,
            AnnouncementRepository announcementRepository,
            AuthService authService
    ) {
        this.users = userRepository;
        this.activities = activityRepository;
        this.announcements = announcementRepository;
        this.authService = authService;
    }

    @PostMapping("/users")
    public UserResponse createUser(@Valid @RequestBody AdminUserRequest adminUserRequest) {
        return authService.createByAdmin(adminUserRequest);
    }

    @GetMapping("/users")
    public List<UserResponse> users() {
        return users.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @GetMapping("/users/{id}")
    public UserResponse user(@PathVariable("id") Long userId) {
        User userAccount = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserResponse.from(userAccount);
    }

    @PatchMapping("/users/{id}/status")
    public UserResponse status(
            @PathVariable("id") Long userId,
            @RequestParam("enabled") boolean accountEnabled
    ) {
        User userAccount = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userAccount.setEnabled(accountEnabled);
        return UserResponse.from(users.save(userAccount));
    }

    @GetMapping("/dashboard")
    public Map<String, Long> dashboard() {
        return Map.of(
                "users", users.count(),
                "activeUsers", users.findAll()
                        .stream()
                        .filter(User::isEnabled)
                        .count(),
                "activities", activities.countByActiveTrue(),
                "announcements", announcements.countByActiveTrue()
        );
    }
}
