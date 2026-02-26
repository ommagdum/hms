package com.vinayakit.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardSummaryDto {
    private CountersDto counters;
    private List<OccupancyDto> occupancyDistribution;
    private List<RevenueDto> revenueChart;
    private List<ActivityDto> recentActivity;
}
