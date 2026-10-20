package com.roomly.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileDTO {
    @NotBlank(message = "Vui lòng nhập họ tên.")
    private String fullName;

    private String phone;
}
