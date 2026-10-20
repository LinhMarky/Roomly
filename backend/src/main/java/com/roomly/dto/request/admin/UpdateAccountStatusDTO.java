package com.roomly.dto.request.admin;

import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAccountStatusDTO {
    @Size(max = 500)
    private String reason;
}
