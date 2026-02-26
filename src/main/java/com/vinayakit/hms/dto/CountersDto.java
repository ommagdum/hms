package com.vinayakit.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CountersDto {
    private Long totalRooms;
    private Long activeBookings;
    private BigDecimal totalRevenue;
    private Long totalGuests;
    private TrendsDto trendsDto;
}
