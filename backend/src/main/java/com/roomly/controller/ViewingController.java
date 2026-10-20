package com.roomly.controller;

import com.roomly.dto.ApiResponse;
import com.roomly.dto.request.viewing.CreateViewingDTO;
import com.roomly.dto.request.viewing.ViewingActionDTO;
import com.roomly.repository.DatabaseRepository;
import com.roomly.service.ViewingService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/viewings")
@RequiredArgsConstructor
public class ViewingController {
    private final ViewingService viewingService;

    @GetMapping("/my")
    public ApiResponse<?> my(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(DatabaseRepository.page(viewingService.findTenantAppointments(jwt)));
    }

    @GetMapping("/host")
    public ApiResponse<?> host(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(DatabaseRepository.page(viewingService.findHostAppointments(jwt)));
    }

    @GetMapping("/room/{id}")
    public ApiResponse<?> slots(
            @PathVariable("id") long roomId,
            @RequestParam(defaultValue = "false") boolean host,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(
                DatabaseRepository.page(viewingService.findRoomSlots(roomId, jwt, host)));
    }

    @PostMapping
    public ApiResponse<?> create(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateViewingDTO request) {
        return ApiResponse.ok(viewingService.createViewing(jwt, request));
    }

    @PostMapping("/{id}/{action:request|approve|reject|cancel|reopen}")
    public ApiResponse<?> action(
            @PathVariable("id") long viewingId,
            @PathVariable String action,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ViewingActionDTO request) {
        return ApiResponse.ok(viewingService.updateViewing(viewingId, jwt, action, request));
    }
}
