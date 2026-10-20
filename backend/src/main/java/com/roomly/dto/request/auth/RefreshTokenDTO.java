package com.roomly.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenDTO {
    @NotBlank(message = "Thiếu refresh token.")
    private String refreshToken;
}
