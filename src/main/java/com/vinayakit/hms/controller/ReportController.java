package com.vinayakit.hms.controller;

import com.vinayakit.hms.dto.ApiResponse;
import com.vinayakit.hms.dto.CustomerHistoryReportDto;
import com.vinayakit.hms.dto.DailyRevenueReportDto;
import com.vinayakit.hms.dto.MonthlyOccupancyReportDto;
import com.vinayakit.hms.service.CustomerService;
import com.vinayakit.hms.service.ReportExportService;
import com.vinayakit.hms.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ReportController {

    private final ReportService reportService;
    private final ReportExportService exportService;
    private final CustomerService customerService;

    // JSON endpoints
    @GetMapping("/daily-revenue")
    public ResponseEntity<ApiResponse<List<DailyRevenueReportDto>>> getDailyRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<DailyRevenueReportDto> report = reportService.getDailyRevenueReportRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/monthly-occupancy")
    public ResponseEntity<ApiResponse<MonthlyOccupancyReportDto>> getMonthlyOccupancyReport(
            @RequestParam int year, @RequestParam int month) {
        MonthlyOccupancyReportDto report = reportService.getMonthlyOccupancyReport(year, month);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/customer-history/{customerId}")
    public ResponseEntity<ApiResponse<List<CustomerHistoryReportDto>>> getCustomerHistory(
            @PathVariable Long customerId) {
        List<CustomerHistoryReportDto> history = reportService.getCustomerHistory(customerId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    // Export endpoints
    @GetMapping("/daily-revenue/export")
    public ResponseEntity<byte[]> exportDailyRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String format) { // format = "pdf" or "excel"

        List<DailyRevenueReportDto> data = reportService.getDailyRevenueReportRange(startDate, endDate);
        byte[] reportBytes;
        String filename;
        MediaType contentType;

        if ("pdf".equalsIgnoreCase(format)) {
            reportBytes = exportService.generateDailyRevenuePdf(data, startDate, endDate);
            filename = "daily-revenue-" + startDate + "-to-" + endDate + ".pdf";
            contentType = MediaType.APPLICATION_PDF;
        } else if ("excel".equalsIgnoreCase(format)) {
            reportBytes = exportService.generateDailyRevenueExcel(data, startDate, endDate);
            filename = "daily-revenue-" + startDate + "-to-" + endDate + ".xlsx";
            contentType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else {
            return ResponseEntity.badRequest().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(reportBytes.length);

        return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/monthly-occupancy/export")
    public ResponseEntity<byte[]> exportMonthlyOccupancyReport(
            @RequestParam int year, @RequestParam int month, @RequestParam String format) {
        MonthlyOccupancyReportDto data = reportService.getMonthlyOccupancyReport(year, month);
        byte[] reportBytes;
        String filename;
        MediaType contentType;

        if ("pdf".equalsIgnoreCase(format)) {
            reportBytes = exportService.generateMonthlyOccupancyPdf(data);
            filename = "monthly-occupancy-" + year + "-" + month + ".pdf";
            contentType = MediaType.APPLICATION_PDF;
        } else if ("excel".equalsIgnoreCase(format)) {
            reportBytes = exportService.generateMonthlyOccupancyExcel(data);
            filename = "monthly-occupancy-" + year + "-" + month + ".xlsx";
            contentType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else {
            return ResponseEntity.badRequest().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(reportBytes.length);

        return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/customer-history/{customerId}/export")
    public ResponseEntity<byte[]> exportCustomerHistory(
            @PathVariable Long customerId,
            @RequestParam String format) {
        List<CustomerHistoryReportDto> data = reportService.getCustomerHistory(customerId);
        // Need customer name – you may fetch it from customerService
        String customerName = customerService.getCustomerById(customerId).getName();
        byte[] reportBytes;
        String filename;
        MediaType contentType;

        if ("pdf".equalsIgnoreCase(format)) {
            reportBytes = exportService.generateCustomerHistoryPdf(data, customerId, customerName);
            filename = "customer-history-" + customerId + ".pdf";
            contentType = MediaType.APPLICATION_PDF;
        } else if ("excel".equalsIgnoreCase(format)) {
            reportBytes = exportService.generateCustomerHistoryExcel(data, customerId, customerName);
            filename = "customer-history-" + customerId + ".xlsx";
            contentType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else {
            return ResponseEntity.badRequest().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(reportBytes.length);

        return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
    }

}
