package com.vinayakit.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDto {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String role;
}
