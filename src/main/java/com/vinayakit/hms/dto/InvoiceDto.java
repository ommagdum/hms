package com.vinayakit.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceDto {

    private Long bookingId;
    private String invoiceNumber;
    private LocalDate issueDate;

    private String customerName;
    private String customerEmail;
    private String customerPhone;

    private String roomNumber;
    private String roomType;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int nights;

    private BigDecimal subtotal;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;

    private String companyName;
    private String companyEmail;
    private String companyPhone;
    private String companyAddress;
}
