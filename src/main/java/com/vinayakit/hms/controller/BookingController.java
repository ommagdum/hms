package com.vinayakit.hms.controller;

import com.vinayakit.hms.dto.ApiResponse;
import com.vinayakit.hms.dto.BookingDto;
import com.vinayakit.hms.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingDto>> createBooking(
            @Valid @RequestBody BookingDto bookingDto) {
        BookingDto createdBooking = bookingService.createBooking(bookingDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdBooking, "Booking created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BookingDto>>> getAllBookings(
            @PageableDefault(size = 10, sort = "checkIn", direction = Sort.Direction.DESC) Pageable pageable
            ) {
        Page<BookingDto> bookings = bookingService.getAllBookings(pageable);
        return ResponseEntity
                .ok(ApiResponse.success(bookings));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingDto>> getBookingById(@PathVariable Long id){
        BookingDto booking = bookingService.getBookingById(id);
        return ResponseEntity
                .ok(ApiResponse.success(booking));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingDto>> cancelBooking(@PathVariable Long id) {
        BookingDto cancelledBooking = bookingService.cancelBooking(id);
        return ResponseEntity
                .ok(ApiResponse.success(cancelledBooking, "Booking cancelled successfully"));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<Page<BookingDto>>> getBookingsByCustomer(
            @PathVariable Long customerId,
            @PageableDefault(size = 10, sort = "checkIn", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<BookingDto> bookings = bookingService.getBookingsByCustomer(customerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

}
