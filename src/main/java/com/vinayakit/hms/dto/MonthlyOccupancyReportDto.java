package com.vinayakit.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyOccupancyReportDto {
    public String month;
    private Long occupiedRoomNights;
    private Long totalAvailableRoomNights;
    private Double occupancyPercentage;
}
