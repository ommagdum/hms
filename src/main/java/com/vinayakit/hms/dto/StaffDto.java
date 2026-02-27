package com.vinayakit.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StaffDto {
    private Long staffId;
    private String name;
    private String role;
    private String contact;
    private BigDecimal salary;
    private String username;
}
