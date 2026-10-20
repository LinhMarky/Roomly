package com.roomly.controller;

import com.roomly.dto.ApiResponse;
import com.roomly.dto.request.push.RegisterDeviceTokenDTO;
import com.roomly.dto.request.push.RemoveDeviceTokenDTO;
import com.roomly.service.PushService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile/device-token")
@RequiredArgsConstructor
public class PushController {
    private final PushService pushService;

    @PutMapping
    public ApiResponse<?> register(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RegisterDeviceTokenDTO request) {
        return ApiResponse.ok(pushService.registerDevice(jwt, request));
    }

    @DeleteMapping
    public ApiResponse<?> remove(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RemoveDeviceTokenDTO request) {
        pushService.removeDevice(jwt, request);
        return ApiResponse.ok(null);
    }
}
