package com.vinayakit.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomBookingDto {
    private Long roomId;
    private String roomNumber;
    private List<BookingRangeItemDto> bookings;
}
