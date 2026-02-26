package com.vinayakit.hms.controller;

import com.vinayakit.hms.dto.ApiResponse;
import com.vinayakit.hms.dto.BookingDto;
import com.vinayakit.hms.dto.CustomerDto;
import com.vinayakit.hms.service.BookingService;
import com.vinayakit.hms.service.CustomerService;
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
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@CrossOrigin("*")
public class CustomerController {

    private final CustomerService customerService;
    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerDto>> createCustomer(
            @Valid @RequestBody CustomerDto customerDto
    ) {
        CustomerDto created = customerService.createCustomer(customerDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Customer created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CustomerDto>>> getAllCustomers(
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<CustomerDto> customers = customerService.getAllCustomers(pageable);
        return ResponseEntity
                .ok(ApiResponse.success(customers));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDto>> getCustomerById(@PathVariable Long id) {
        CustomerDto customer = customerService.getCustomerById(id);
        return ResponseEntity
                .ok(ApiResponse.success(customer));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDto>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerDto customerDto
    ) {
        CustomerDto updated = customerService.updateCustomer(id,customerDto);
        return ResponseEntity
                .ok(ApiResponse.success(updated, "Customer updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Customer deleted successfully"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<CustomerDto>>> searchCustomers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        if (name != null && phone != null) {
            throw new IllegalArgumentException("Provide only one search parameter: name or phone");
        }
        if (name != null) {
            Page<CustomerDto> results = customerService.searchCustomerByName(name, pageable);
            return ResponseEntity.ok(ApiResponse.success(results));
        } else if (phone != null) {
            Page<CustomerDto> results = customerService.searchCustomerByPhone(phone, pageable);
            return ResponseEntity.ok(ApiResponse.success(results));
        } else {
            throw new IllegalArgumentException("Provide a search parameter: name or phone");
        }
    }

    @GetMapping("/{id}/bookings")
    public ResponseEntity<ApiResponse<Page<BookingDto>>> getCustomerBookingHistory(
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = "checkIn", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<BookingDto> bookings = bookingService.getBookingsByCustomer(id, pageable);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }
}
