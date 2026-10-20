package com.roomly.controller;

import com.roomly.dto.ApiResponse;
import com.roomly.dto.request.auth.EmailCodeDTO;
import com.roomly.dto.request.auth.LoginDTO;
import com.roomly.dto.request.auth.LogoutDTO;
import com.roomly.dto.request.auth.RefreshTokenDTO;
import com.roomly.dto.request.auth.RegisterDTO;
import com.roomly.dto.request.auth.ResetPasswordDTO;
import com.roomly.dto.request.auth.UpdateProfileDTO;
import com.roomly.dto.request.auth.VerifyEmailDTO;
import com.roomly.service.AuthChallengeService;
import com.roomly.service.AuthService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final AuthChallengeService authChallengeService;

    @PostMapping("/auth/login")
    public ApiResponse<?> login(@Valid @RequestBody LoginDTO request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/auth/register")
    public ApiResponse<?> register(@Valid @RequestBody RegisterDTO request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/auth/refresh")
    public ApiResponse<?> refresh(@Valid @RequestBody RefreshTokenDTO request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @PostMapping("/auth/logout")
    public ApiResponse<?> logout(@AuthenticationPrincipal Jwt jwt, @RequestBody LogoutDTO request) {
        authService.logout(jwt);
        return ApiResponse.ok(null);
    }

    @GetMapping({"/auth/account", "/profile"})
    public ApiResponse<?> account(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(authService.getCurrentAccount(jwt));
    }

    @PutMapping("/profile")
    public ApiResponse<?> updateProfile(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileDTO request) {
        return ApiResponse.ok(authService.updateProfile(jwt, request));
    }

    @PostMapping("/auth/enable-host")
    public ApiResponse<?> enableHost(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(authService.enableHost(jwt));
    }

    @PostMapping("/auth/verification-code")
    public ApiResponse<?> requestVerificationCode(@Valid @RequestBody EmailCodeDTO request) {
        return ApiResponse.ok(authChallengeService.requestVerificationCode(request));
    }

    @PostMapping("/auth/recovery-code")
    public ApiResponse<?> requestRecoveryCode(@Valid @RequestBody EmailCodeDTO request) {
        return ApiResponse.ok(authChallengeService.requestRecoveryCode(request));
    }

    @PostMapping("/auth/verify-email")
    public ApiResponse<?> verifyEmail(@Valid @RequestBody VerifyEmailDTO request) {
        return ApiResponse.ok(authChallengeService.verifyEmail(request));
    }

    @PostMapping("/auth/reset-password")
    public ApiResponse<?> resetPassword(@Valid @RequestBody ResetPasswordDTO request) {
        return ApiResponse.ok(authChallengeService.resetPassword(request));
    }
}
