package com.vinayakit.hms.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateStaffRequest {
    private String name;
    private String role;
    private String contact;
    private BigDecimal salary;
    private String username;
    private String password;
}
