package com.vinayakit.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRangeItemDto {
    private String guestName;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private String status;
}
