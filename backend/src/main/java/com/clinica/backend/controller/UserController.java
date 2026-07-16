package com.clinica.backend.controller;

import com.clinica.backend.dto.UpdatePasswordRequest;
import com.clinica.backend.dto.UpdateProfileRequest;
import com.clinica.backend.dto.UserProfileDTO;
import com.clinica.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(userService.getUserProfile(username));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDTO> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request) {
        String username = authentication.getName();
        return ResponseEntity.ok(userService.updateProfile(username, request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> updatePassword(
            Authentication authentication,
            @RequestBody UpdatePasswordRequest request) {
        String username = authentication.getName();
        userService.updatePassword(username, request);
        return ResponseEntity.ok().build();
    }
}
