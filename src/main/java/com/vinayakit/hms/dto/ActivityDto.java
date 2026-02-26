package com.vinayakit.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityDto {
    private Long id;
    private String guestName;
    private String roomNumber;
    private String type;
    private String time;
}
