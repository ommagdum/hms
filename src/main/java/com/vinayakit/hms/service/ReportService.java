package com.vinayakit.hms.service;

import com.vinayakit.hms.dto.CustomerHistoryReportDto;
import com.vinayakit.hms.dto.DailyRevenueReportDto;
import com.vinayakit.hms.dto.MonthlyOccupancyReportDto;
import com.vinayakit.hms.entity.Booking;
import com.vinayakit.hms.repository.BookingRepository;
import com.vinayakit.hms.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    public DailyRevenueReportDto getDailyRevenueReport(LocalDate date) {
        BigDecimal revenue = bookingRepository.getDailyRevenue(date);
        Long totalBookings = bookingRepository.countByCheckInDateAndStatus(date, "CONFIRMED");

        // Handle null from repository
        if (revenue == null) {
            revenue = BigDecimal.ZERO;
        }
        if (totalBookings == null) {
            totalBookings = 0L;
        }

        return new DailyRevenueReportDto(date, revenue, totalBookings);
    }

    public List<DailyRevenueReportDto> getDailyRevenueReportRange(LocalDate start, LocalDate end) {
        List<Map<String, Object>> results = bookingRepository.getDailyRevenueBetween(start, end);
        if (results == null) {
            return Collections.emptyList();
        }

        return results.stream()
                .map(map -> {
                    java.sql.Date sqlDate = (java.sql.Date) map.get("date");
                    LocalDate date = sqlDate.toLocalDate();
                    return new DailyRevenueReportDto(
                            date,
                            (BigDecimal) map.get("revenue"),
                            (Long) map.get("count")
                    );
                })
                .collect(Collectors.toList());
    }

    public MonthlyOccupancyReportDto getMonthlyOccupancyReport(int year, int month) {
        Long occupiedNights = bookingRepository.countOccupiedRoomNights(year, month);
        long totalRooms = roomRepository.count();
        YearMonth yearMonth = YearMonth.of(year, month);
        int daysInMonth = yearMonth.lengthOfMonth();
        long totalAvailableNights = totalRooms * daysInMonth;

        // Handle null occupiedNights
        long occupied = occupiedNights != null ? occupiedNights : 0L;

        double occupancyPercentage = totalAvailableNights > 0
                ? (occupied * 100.0) / totalAvailableNights
                : 0.0;

        return new MonthlyOccupancyReportDto(
                year + "-" + String.format("%02d", month),
                occupied,
                totalAvailableNights,
                occupancyPercentage
        );
    }

    public List<CustomerHistoryReportDto> getCustomerHistory(Long customerId) {
        List<Booking> bookings = bookingRepository.findByCustomerIdOrderByCheckInDesc(customerId);
        if (bookings == null) {
            return Collections.emptyList();
        }

        return bookings.stream()
                .filter(Objects::nonNull)
                .map(b -> new CustomerHistoryReportDto(
                        b.getBookingId(),
                        b.getCheckIn(),
                        b.getCheckOut(),
                        b.getRoom() != null ? b.getRoom().getRoomNumber() : "N/A",
                        b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO,
                        b.getStatus() != null ? b.getStatus() : "UNKNOWN"
                ))
                .collect(Collectors.toList());
    }
}