package com.vinayakit.hms.service;

import com.vinayakit.hms.dto.*;
import com.vinayakit.hms.entity.Booking;
import com.vinayakit.hms.entity.Customer;
import com.vinayakit.hms.entity.Room;
import com.vinayakit.hms.repository.BookingRepository;
import com.vinayakit.hms.repository.CustomerRepository;
import com.vinayakit.hms.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private LocalDate fixedToday;
    private LocalDateTime fixedTodayStart;
    private LocalDateTime fixedTodayEnd;
    private LocalDateTime fixedSevenDaysAgo;
    private LocalDateTime fixedLastWeekStart;
    private LocalDateTime fixedLastWeekEnd;
    private LocalDateTime fixedPrevWeekStart;
    private LocalDateTime fixedPrevWeekEnd;

    @BeforeEach
    void setUp() {
        // Fix the current date to 2026-03-01 to make tests deterministic
        fixedToday = LocalDate.of(2026, 3, 1);
        fixedTodayStart = fixedToday.atStartOfDay();
        fixedTodayEnd = fixedToday.atTime(LocalTime.MAX);
        fixedSevenDaysAgo = fixedToday.minusDays(6).atStartOfDay();
        fixedLastWeekStart = fixedToday.minusDays(7).atStartOfDay();
        fixedLastWeekEnd = fixedToday.atStartOfDay();
        fixedPrevWeekStart = fixedToday.minusDays(14).atStartOfDay();
        fixedPrevWeekEnd = fixedToday.minusDays(7).atStartOfDay();
    }

    // ========== Test: All data present ==========
    @Test
    void getDashboardSummary_AllDataPresent() {
        try (MockedStatic<LocalDate> mockedStatic = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedStatic.when(() -> LocalDate.now()).thenReturn(fixedToday);

            // Mock counters
            when(roomRepository.count()).thenReturn(10L);
            when(bookingRepository.countActiveBookings()).thenReturn(5L);
            when(bookingRepository.sumTotalRevenue()).thenReturn(BigDecimal.valueOf(5000));
            when(customerRepository.countTotalCustomers()).thenReturn(20L);

            // Mock occupancy distribution
            List<Object[]> occupancyRaw = new ArrayList<>();
            occupancyRaw.add(new Object[]{"AVAILABLE", 7L});
            occupancyRaw.add(new Object[]{"OCCUPIED", 3L});
            when(roomRepository.countByStatus()).thenReturn(occupancyRaw);

            // Mock daily revenue for last 7 days
            List<Object[]> dailyRevenueRaw = new ArrayList<>();
            dailyRevenueRaw.add(new Object[]{Date.valueOf("2026-02-24"), BigDecimal.valueOf(100)});
            dailyRevenueRaw.add(new Object[]{Date.valueOf("2026-02-25"), BigDecimal.valueOf(200)});
            dailyRevenueRaw.add(new Object[]{Date.valueOf("2026-02-26"), BigDecimal.valueOf(150)});
            dailyRevenueRaw.add(new Object[]{Date.valueOf("2026-02-27"), BigDecimal.valueOf(300)});
            dailyRevenueRaw.add(new Object[]{Date.valueOf("2026-02-28"), BigDecimal.valueOf(250)});
            dailyRevenueRaw.add(new Object[]{Date.valueOf("2026-03-01"), BigDecimal.valueOf(400)});
            when(bookingRepository.findDailyRevenueSince(fixedSevenDaysAgo)).thenReturn(dailyRevenueRaw);

            // Mock check-ins and check-outs
            Customer customer = new Customer();
            customer.setName("John Doe");
            Room room = new Room();
            room.setRoomNumber("101");

            Booking checkInBooking = new Booking();
            checkInBooking.setBookingId(1L);
            checkInBooking.setCustomer(customer);
            checkInBooking.setRoom(room);
            checkInBooking.setCreatedAt(LocalDateTime.of(2026, 3, 1, 9, 30));

            Booking checkOutBooking = new Booking();
            checkOutBooking.setBookingId(2L);
            checkOutBooking.setCustomer(customer);
            checkOutBooking.setRoom(room);
            checkOutBooking.setUpdatedAt(LocalDateTime.of(2026, 3, 1, 11, 15));

            when(bookingRepository.findTodayCheckIns()).thenReturn(List.of(checkInBooking));
            when(bookingRepository.findTodayCheckOuts()).thenReturn(List.of(checkOutBooking));

            // Mock trends calculations
            when(bookingRepository.sumRevenueBetween(fixedLastWeekStart, fixedLastWeekEnd))
                    .thenReturn(BigDecimal.valueOf(2000));
            when(bookingRepository.sumRevenueBetween(fixedPrevWeekStart, fixedPrevWeekEnd))
                    .thenReturn(BigDecimal.valueOf(1500));
            when(bookingRepository.countByCreatedAtBetweenAndStatus(fixedTodayStart, fixedTodayEnd, "CONFIRMED"))
                    .thenReturn(3L);

            // Execute
            DashboardSummaryDto result = dashboardService.getDashboardSummary();

            // Assert counters
            CountersDto counters = result.getCounters();
            assertThat(counters.getTotalRooms()).isEqualTo(10L);
            assertThat(counters.getActiveBookings()).isEqualTo(5L);
            assertThat(counters.getTotalRevenue()).isEqualByComparingTo("5000");
            assertThat(counters.getTotalGuests()).isEqualTo(20L);

            // Assert trends
            TrendsDto trends = counters.getTrendsDto();
            assertThat(trends.getRevenueGrowth()).isEqualTo("+33.3%");
            assertThat(trends.getBookingGrowth()).isEqualTo("+3 today");

            // Assert occupancy distribution
            List<OccupancyDto> occupancy = result.getOccupancyDistribution();
            assertThat(occupancy).hasSize(2);
            assertThat(occupancy.get(0).getStatus()).isEqualTo("AVAILABLE");
            assertThat(occupancy.get(0).getCount()).isEqualTo(7L);
            assertThat(occupancy.get(1).getStatus()).isEqualTo("OCCUPIED");
            assertThat(occupancy.get(1).getCount()).isEqualTo(3L);

            // Assert revenue chart (7 days, with zeros for missing dates)
            List<RevenueDto> revenueChart = result.getRevenueChart();
            assertThat(revenueChart).hasSize(7);
            assertThat(revenueChart.get(0).getDate()).isEqualTo(LocalDate.of(2026, 2, 23));
            assertThat(revenueChart.get(0).getAmount()).isEqualByComparingTo("0");
            assertThat(revenueChart.get(1).getDate()).isEqualTo(LocalDate.of(2026, 2, 24));
            assertThat(revenueChart.get(1).getAmount()).isEqualByComparingTo("100");
            assertThat(revenueChart.get(2).getDate()).isEqualTo(LocalDate.of(2026, 2, 25));
            assertThat(revenueChart.get(2).getAmount()).isEqualByComparingTo("200");
            assertThat(revenueChart.get(3).getDate()).isEqualTo(LocalDate.of(2026, 2, 26));
            assertThat(revenueChart.get(3).getAmount()).isEqualByComparingTo("150");
            assertThat(revenueChart.get(4).getDate()).isEqualTo(LocalDate.of(2026, 2, 27));
            assertThat(revenueChart.get(4).getAmount()).isEqualByComparingTo("300");
            assertThat(revenueChart.get(5).getDate()).isEqualTo(LocalDate.of(2026, 2, 28));
            assertThat(revenueChart.get(5).getAmount()).isEqualByComparingTo("250");
            assertThat(revenueChart.get(6).getDate()).isEqualTo(LocalDate.of(2026, 3, 1));
            assertThat(revenueChart.get(6).getAmount()).isEqualByComparingTo("400");
        }
    }

    // ========== Test: No data ==========
    @Test
    void getDashboardSummary_NoData() {
        try (MockedStatic<LocalDate> mockedStatic = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedStatic.when(() -> LocalDate.now()).thenReturn(fixedToday);

            when(roomRepository.count()).thenReturn(0L);
            when(bookingRepository.countActiveBookings()).thenReturn(0L);
            when(bookingRepository.sumTotalRevenue()).thenReturn(BigDecimal.ZERO);
            when(customerRepository.countTotalCustomers()).thenReturn(0L);
            when(roomRepository.countByStatus()).thenReturn(new ArrayList<>());
            when(bookingRepository.findDailyRevenueSince(fixedSevenDaysAgo)).thenReturn(new ArrayList<>());
            when(bookingRepository.findTodayCheckIns()).thenReturn(new ArrayList<>());
            when(bookingRepository.findTodayCheckOuts()).thenReturn(new ArrayList<>());
            when(bookingRepository.sumRevenueBetween(fixedLastWeekStart, fixedLastWeekEnd)).thenReturn(BigDecimal.ZERO);
            when(bookingRepository.sumRevenueBetween(fixedPrevWeekStart, fixedPrevWeekEnd)).thenReturn(BigDecimal.ZERO);
            when(bookingRepository.countByCreatedAtBetweenAndStatus(fixedTodayStart, fixedTodayEnd, "CONFIRMED"))
                    .thenReturn(0L);

            DashboardSummaryDto result = dashboardService.getDashboardSummary();

            assertThat(result.getCounters().getTotalRooms()).isZero();
            assertThat(result.getCounters().getActiveBookings()).isZero();
            assertThat(result.getCounters().getTotalRevenue()).isEqualByComparingTo("0");
            assertThat(result.getCounters().getTotalGuests()).isZero();
            assertThat(result.getCounters().getTrendsDto().getRevenueGrowth()).isEqualTo("N/A");
            assertThat(result.getCounters().getTrendsDto().getBookingGrowth()).isEqualTo("+0 today");
            assertThat(result.getOccupancyDistribution()).isEmpty();
            assertThat(result.getRevenueChart()).hasSize(7);
            result.getRevenueChart().forEach(dto -> assertThat(dto.getAmount()).isEqualByComparingTo("0"));
            assertThat(result.getRecentActivity()).isEmpty();
        }
    }

    // ========== Test: Revenue growth negative ==========
    @Test
    void getDashboardSummary_RevenueGrowthNegative() {
        try (MockedStatic<LocalDate> mockedStatic = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedStatic.when(() -> LocalDate.now()).thenReturn(fixedToday);

            // Minimal mocks to avoid NPE
            when(roomRepository.count()).thenReturn(0L);
            when(bookingRepository.countActiveBookings()).thenReturn(0L);
            when(bookingRepository.sumTotalRevenue()).thenReturn(BigDecimal.ZERO);
            when(customerRepository.countTotalCustomers()).thenReturn(0L);
            when(roomRepository.countByStatus()).thenReturn(new ArrayList<>());
            when(bookingRepository.findDailyRevenueSince(fixedSevenDaysAgo)).thenReturn(new ArrayList<>());
            when(bookingRepository.findTodayCheckIns()).thenReturn(new ArrayList<>());
            when(bookingRepository.findTodayCheckOuts()).thenReturn(new ArrayList<>());
            when(bookingRepository.countByCreatedAtBetweenAndStatus(fixedTodayStart, fixedTodayEnd, "CONFIRMED"))
                    .thenReturn(0L);

            when(bookingRepository.sumRevenueBetween(fixedLastWeekStart, fixedLastWeekEnd))
                    .thenReturn(BigDecimal.valueOf(800));
            when(bookingRepository.sumRevenueBetween(fixedPrevWeekStart, fixedPrevWeekEnd))
                    .thenReturn(BigDecimal.valueOf(1000));

            DashboardSummaryDto result = dashboardService.getDashboardSummary();
            assertThat(result.getCounters().getTrendsDto().getRevenueGrowth()).isEqualTo("-20.0%");
        }
    }

    // ========== Test: Revenue growth when previous week zero ==========
    @Test
    void getDashboardSummary_RevenuePrevWeekZero() {
        try (MockedStatic<LocalDate> mockedStatic = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedStatic.when(() -> LocalDate.now()).thenReturn(fixedToday);

            // Minimal mocks
            when(roomRepository.count()).thenReturn(0L);
            when(bookingRepository.countActiveBookings()).thenReturn(0L);
            when(bookingRepository.sumTotalRevenue()).thenReturn(BigDecimal.ZERO);
            when(customerRepository.countTotalCustomers()).thenReturn(0L);
            when(roomRepository.countByStatus()).thenReturn(new ArrayList<>());
            when(bookingRepository.findDailyRevenueSince(fixedSevenDaysAgo)).thenReturn(new ArrayList<>());
            when(bookingRepository.findTodayCheckIns()).thenReturn(new ArrayList<>());
            when(bookingRepository.findTodayCheckOuts()).thenReturn(new ArrayList<>());
            when(bookingRepository.countByCreatedAtBetweenAndStatus(fixedTodayStart, fixedTodayEnd, "CONFIRMED"))
                    .thenReturn(0L);

            when(bookingRepository.sumRevenueBetween(fixedLastWeekStart, fixedLastWeekEnd))
                    .thenReturn(BigDecimal.valueOf(500));
            when(bookingRepository.sumRevenueBetween(fixedPrevWeekStart, fixedPrevWeekEnd))
                    .thenReturn(BigDecimal.ZERO);

            DashboardSummaryDto result = dashboardService.getDashboardSummary();
            assertThat(result.getCounters().getTrendsDto().getRevenueGrowth()).isEqualTo("N/A");
        }
    }

    // ========== Test: Occupancy distribution mapping ==========
    @Test
    void getDashboardSummary_OccupancyDistribution() {
        try (MockedStatic<LocalDate> mockedStatic = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedStatic.when(() -> LocalDate.now()).thenReturn(fixedToday);

            // Minimal mocks
            when(roomRepository.count()).thenReturn(0L);
            when(bookingRepository.countActiveBookings()).thenReturn(0L);
            when(bookingRepository.sumTotalRevenue()).thenReturn(BigDecimal.ZERO);
            when(customerRepository.countTotalCustomers()).thenReturn(0L);
            when(bookingRepository.findDailyRevenueSince(fixedSevenDaysAgo)).thenReturn(new ArrayList<>());
            when(bookingRepository.findTodayCheckIns()).thenReturn(new ArrayList<>());
            when(bookingRepository.findTodayCheckOuts()).thenReturn(new ArrayList<>());
            when(bookingRepository.sumRevenueBetween(any(), any())).thenReturn(BigDecimal.ZERO);
            when(bookingRepository.countByCreatedAtBetweenAndStatus(any(), any(), any())).thenReturn(0L);

            List<Object[]> occupancyRaw = List.of(
                    new Object[]{"AVAILABLE", 5L},
                    new Object[]{"MAINTENANCE", 2L}
            );
            when(roomRepository.countByStatus()).thenReturn(occupancyRaw);

            DashboardSummaryDto result = dashboardService.getDashboardSummary();
            List<OccupancyDto> occupancy = result.getOccupancyDistribution();

            assertThat(occupancy).hasSize(2);
            assertThat(occupancy.get(0).getStatus()).isEqualTo("AVAILABLE");
            assertThat(occupancy.get(0).getCount()).isEqualTo(5L);
            assertThat(occupancy.get(1).getStatus()).isEqualTo("MAINTENANCE");
            assertThat(occupancy.get(1).getCount()).isEqualTo(2L);
        }
    }

    // ========== Test: Revenue chart with missing dates ==========
    @Test
    void getDashboardSummary_RevenueChart_FillMissingDates() {
        try (MockedStatic<LocalDate> mockedStatic = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedStatic.when(() -> LocalDate.now()).thenReturn(fixedToday);

            // Minimal mocks
            when(roomRepository.count()).thenReturn(0L);
            when(bookingRepository.countActiveBookings()).thenReturn(0L);
            when(bookingRepository.sumTotalRevenue()).thenReturn(BigDecimal.ZERO);
            when(customerRepository.countTotalCustomers()).thenReturn(0L);
            when(roomRepository.countByStatus()).thenReturn(new ArrayList<>());
            when(bookingRepository.findTodayCheckIns()).thenReturn(new ArrayList<>());
            when(bookingRepository.findTodayCheckOuts()).thenReturn(new ArrayList<>());
            when(bookingRepository.sumRevenueBetween(any(), any())).thenReturn(BigDecimal.ZERO);
            when(bookingRepository.countByCreatedAtBetweenAndStatus(any(), any(), any())).thenReturn(0L);

            List<Object[]> dailyRevenueRaw = List.of(
                    new Object[]{Date.valueOf("2026-02-24"), BigDecimal.valueOf(100)},
                    new Object[]{Date.valueOf("2026-02-27"), BigDecimal.valueOf(200)}
            );
            when(bookingRepository.findDailyRevenueSince(fixedSevenDaysAgo)).thenReturn(dailyRevenueRaw);

            DashboardSummaryDto result = dashboardService.getDashboardSummary();
            List<RevenueDto> revenueChart = result.getRevenueChart();

            assertThat(revenueChart).hasSize(7);
            assertThat(revenueChart.get(0).getDate()).isEqualTo(LocalDate.of(2026, 2, 23));
            assertThat(revenueChart.get(0).getAmount()).isEqualByComparingTo("0");
            assertThat(revenueChart.get(1).getDate()).isEqualTo(LocalDate.of(2026, 2, 24));
            assertThat(revenueChart.get(1).getAmount()).isEqualByComparingTo("100");
            assertThat(revenueChart.get(2).getDate()).isEqualTo(LocalDate.of(2026, 2, 25));
            assertThat(revenueChart.get(2).getAmount()).isEqualByComparingTo("0");
            assertThat(revenueChart.get(3).getDate()).isEqualTo(LocalDate.of(2026, 2, 26));
            assertThat(revenueChart.get(3).getAmount()).isEqualByComparingTo("0");
            assertThat(revenueChart.get(4).getDate()).isEqualTo(LocalDate.of(2026, 2, 27));
            assertThat(revenueChart.get(4).getAmount()).isEqualByComparingTo("200");
            assertThat(revenueChart.get(5).getDate()).isEqualTo(LocalDate.of(2026, 2, 28));
            assertThat(revenueChart.get(5).getAmount()).isEqualByComparingTo("0");
            assertThat(revenueChart.get(6).getDate()).isEqualTo(LocalDate.of(2026, 3, 1));
            assertThat(revenueChart.get(6).getAmount()).isEqualByComparingTo("0");
        }
    }

    // ========== Test: Recent activity sorting ==========
    @Test
    void getDashboardSummary_RecentActivity_Sorting() {
        try (MockedStatic<LocalDate> mockedStatic = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedStatic.when(() -> LocalDate.now()).thenReturn(fixedToday);

            // Minimal mocks
            when(roomRepository.count()).thenReturn(0L);
            when(bookingRepository.countActiveBookings()).thenReturn(0L);
            when(bookingRepository.sumTotalRevenue()).thenReturn(BigDecimal.ZERO);
            when(customerRepository.countTotalCustomers()).thenReturn(0L);
            when(roomRepository.countByStatus()).thenReturn(new ArrayList<>());
            when(bookingRepository.findDailyRevenueSince(fixedSevenDaysAgo)).thenReturn(new ArrayList<>());
            when(bookingRepository.sumRevenueBetween(any(), any())).thenReturn(BigDecimal.ZERO);
            when(bookingRepository.countByCreatedAtBetweenAndStatus(any(), any(), any())).thenReturn(0L);

            Customer customer = new Customer();
            customer.setName("John Doe");
            Room room = new Room();
            room.setRoomNumber("101");

            Booking checkIn1 = new Booking();
            checkIn1.setBookingId(1L);
            checkIn1.setCustomer(customer);
            checkIn1.setRoom(room);
            checkIn1.setCreatedAt(LocalDateTime.of(2026, 3, 1, 10, 0));

            Booking checkOut1 = new Booking();
            checkOut1.setBookingId(2L);
            checkOut1.setCustomer(customer);
            checkOut1.setRoom(room);
            checkOut1.setUpdatedAt(LocalDateTime.of(2026, 3, 1, 9, 0));

            Booking checkIn2 = new Booking();
            checkIn2.setBookingId(3L);
            checkIn2.setCustomer(customer);
            checkIn2.setRoom(room);
            checkIn2.setCreatedAt(LocalDateTime.of(2026, 3, 1, 8, 30));

            when(bookingRepository.findTodayCheckIns()).thenReturn(List.of(checkIn1, checkIn2));
            when(bookingRepository.findTodayCheckOuts()).thenReturn(List.of(checkOut1));

            DashboardSummaryDto result = dashboardService.getDashboardSummary();
            List<ActivityDto> recentActivity = result.getRecentActivity();

            assertThat(recentActivity).hasSize(3);

            assertThat(recentActivity.get(0).getTime()).isEqualTo("9:00 am");
            assertThat(recentActivity.get(0).getType()).isEqualTo("CHECK_OUT");
            assertThat(recentActivity.get(1).getTime()).isEqualTo("8:30 am");
            assertThat(recentActivity.get(1).getType()).isEqualTo("CHECK_IN");
            assertThat(recentActivity.get(2).getTime()).isEqualTo("10:00 am");
            assertThat(recentActivity.get(2).getType()).isEqualTo("CHECK_IN");
        }
    }
}