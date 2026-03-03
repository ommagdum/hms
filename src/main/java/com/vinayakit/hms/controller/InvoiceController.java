package com.vinayakit.hms.controller;

import com.vinayakit.hms.dto.ApiResponse;
import com.vinayakit.hms.exception.ResourceNotFoundException;
import com.vinayakit.hms.repository.BookingRepository;
import com.vinayakit.hms.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;

@RestController
@RequestMapping("/api/invoice")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final BookingRepository bookingRepository;

    @GetMapping("/{bookingId}")
    public ResponseEntity<InputStreamResource> getInvoicePdf(@PathVariable Long bookingId) {
        ByteArrayInputStream pdf = invoiceService.generateInvoicePdf(bookingId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=invoice-" + bookingId + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdf));
    }

    @PostMapping("/{bookingId}/send")
    public ResponseEntity<ApiResponse<String>> sendInvoiceEmail(@PathVariable Long bookingId) {
        if (!bookingRepository.existsById(bookingId)) {
            throw new ResourceNotFoundException("Booking", "id", bookingId);
        }
        invoiceService.sendInvoiceEmail(bookingId, null);
        return ResponseEntity.ok(ApiResponse.success("Invoice sent successfully to customer email"));
    }
}