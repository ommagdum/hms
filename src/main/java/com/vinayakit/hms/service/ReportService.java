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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    public DailyRevenueReportDto getDailyRevenueReport(LocalDate date) {
        BigDecimal revenue = bookingRepository.getDailyRevenue(date);
        Long totalBookings = bookingRepository.countByCheckInDateAndStatus(date, "CONFIRMED");
        return new DailyRevenueReportDto(date, revenue, totalBookings);
    }

    public List<DailyRevenueReportDto> getDailyRevenueReportRange(LocalDate start, LocalDate end) {
        List<Map<String, Object>> results = bookingRepository.getDailyRevenueBetween(start, end);
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
        double occupancyPercentage = totalAvailableNights > 0
                ? (occupiedNights * 100.0) / totalAvailableNights
                : 0.0;

        return new MonthlyOccupancyReportDto(
                year + "-" + String.format("%02d", month),
                occupiedNights,
                totalAvailableNights,
                occupancyPercentage
        );
    }

    public List<CustomerHistoryReportDto> getCustomerHistory(Long customerId) {
        List<Booking> bookings = bookingRepository.findByCustomerIdOrderByCheckInDesc(customerId);
        return bookings.stream()
                .map(b -> new CustomerHistoryReportDto(
                        b.getBookingId(),
                        b.getCheckIn(),
                        b.getCheckOut(),
                        b.getRoom().getRoomNumber(),
                        b.getTotalAmount(),
                        b.getStatus()
                )).collect(Collectors.toList());
    }
}
