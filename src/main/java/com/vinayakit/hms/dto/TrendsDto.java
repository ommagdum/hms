package com.vinayakit.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrendsDto {
    private String revenueGrowth;
    private String bookingGrowth;
}
