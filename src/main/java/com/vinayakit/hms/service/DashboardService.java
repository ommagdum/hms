package com.vinayakit.hms.service;

import com.vinayakit.hms.dto.*;
import com.vinayakit.hms.entity.Booking;
import com.vinayakit.hms.repository.BookingRepository;
import com.vinayakit.hms.repository.CustomerRepository;
import com.vinayakit.hms.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;

    public DashboardSummaryDto getDashboardSummary() {
        // 1. Counters
        Long totalRooms = roomRepository.count();
        Long activeBookings = bookingRepository.countActiveBookings();
        BigDecimal totalRevenue = bookingRepository.sumTotalRevenue();
        Long totalGuests = customerRepository.countTotalCustomers();

        // Trends
        TrendsDto trends = calculateTrends();

        CountersDto counters = new CountersDto(totalRooms, activeBookings, totalRevenue, totalGuests, trends);

        // 2. Occupancy Distribution
        List<Object[]> occupancyRaw = roomRepository.countByStatus();
        List<OccupancyDto> occupancyDistribution = occupancyRaw.stream()
                .map(row -> new OccupancyDto((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());

        // 3. Revenue Chart (last 7 days)
        LocalDateTime sevenDaysAgo = LocalDate.now().minusDays(6).atStartOfDay(); // inclusive of today
        List<Object[]> dailyRevenueRaw = bookingRepository.findDailyRevenueSince(sevenDaysAgo);
        List<RevenueDto> revenueChart = new ArrayList<>();
        // Fill missing dates with zero
        for (int i = 0; i < 7; i++) {
            LocalDate date = LocalDate.now().minusDays(6 - i);
            BigDecimal amount = dailyRevenueRaw.stream()
                    .filter(row -> ((java.sql.Date) row[0]).toLocalDate().equals(date))
                    .map(row -> (BigDecimal) row[1])
                    .findFirst()
                    .orElse(BigDecimal.ZERO);
            revenueChart.add(new RevenueDto(date, amount));
        }

        // 4. Recent Activity (today's check-ins and check-outs)
        List<ActivityDto> recentActivity = new ArrayList<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");

        // Check-ins
        List<Booking> checkIns = bookingRepository.findTodayCheckIns();
        for (Booking b : checkIns) {
            String time = b.getCreatedAt() != null ? b.getCreatedAt().format(timeFormatter) : "N/A";
            recentActivity.add(new ActivityDto(
                    b.getBookingId(),
                    b.getCustomer().getName(),
                    b.getRoom().getRoomNumber(),
                    "CHECK_IN",
                    time
            ));
        }

        // Check-outs
        List<Booking> checkOuts = bookingRepository.findTodayCheckOuts();
        for (Booking b : checkOuts) {
            String time = b.getUpdatedAt() != null ? b.getUpdatedAt().format(timeFormatter) : "N/A";
            recentActivity.add(new ActivityDto(
                    b.getBookingId(),
                    b.getCustomer().getName(),
                    b.getRoom().getRoomNumber(),
                    "CHECK_OUT",
                    time
            ));
        }

        // Sort by time (most recent first)
        recentActivity.sort((a, b) -> b.getTime().compareTo(a.getTime()));

        return new DashboardSummaryDto(counters, occupancyDistribution, revenueChart, recentActivity);
    }

    private TrendsDto calculateTrends() {
        // Revenue growth: compare last 7 days vs previous 7 days
        LocalDate today = LocalDate.now();
        LocalDateTime startLastWeek = today.minusDays(7).atStartOfDay();
        LocalDateTime endLastWeek = today.atStartOfDay();
        LocalDateTime startPrevWeek = today.minusDays(14).atStartOfDay();
        LocalDateTime endPrevWeek = today.minusDays(7).atStartOfDay();

        BigDecimal revenueLastWeek = bookingRepository.sumRevenueBetween(startLastWeek, endLastWeek);
        BigDecimal revenuePrevWeek = bookingRepository.sumRevenueBetween(startPrevWeek, endPrevWeek);

        String revenueGrowth;
        if (revenuePrevWeek.compareTo(BigDecimal.ZERO) == 0) {
            revenueGrowth = "N/A";
        } else {
            BigDecimal growth = revenueLastWeek.subtract(revenuePrevWeek)
                    .divide(revenuePrevWeek, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            revenueGrowth = (growth.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") +
                    growth.setScale(1, RoundingMode.HALF_UP) + "%";
        }

        // Booking growth: difference in active bookings today vs yesterday
        Long activeToday = bookingRepository.countActiveBookings();
        LocalDateTime startToday = today.atStartOfDay();
        LocalDateTime endToday = today.atTime(LocalTime.MAX);
        Long newBookingsToday = bookingRepository.countByCreatedAtBetweenAndStatus(startToday, endToday, "CONFIRMED");
        String bookingGrowth = "+" + newBookingsToday + " today";

        return new TrendsDto(revenueGrowth, bookingGrowth);
    }
}
