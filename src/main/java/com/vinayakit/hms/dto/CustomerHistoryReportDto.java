package com.vinayakit.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerHistoryReportDto {
    private Long bookingId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private String roomNumber;
    private BigDecimal totalAmount;
    private String status;
}
