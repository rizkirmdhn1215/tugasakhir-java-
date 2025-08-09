package com.sttp.skripsi.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String email;
    private String fullName;
    private Boolean isActive;
} 