package com.vinayakit.hms.service;

import com.vinayakit.hms.dto.CustomerHistoryReportDto;
import com.vinayakit.hms.dto.DailyRevenueReportDto;
import com.vinayakit.hms.dto.MonthlyOccupancyReportDto;
import com.vinayakit.hms.entity.Booking;
import com.vinayakit.hms.entity.Customer;
import com.vinayakit.hms.entity.Room;
import com.vinayakit.hms.repository.BookingRepository;
import com.vinayakit.hms.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private ReportService reportService;

    // ========== getDailyRevenueReport (single day) ==========
    @Test
    void getDailyRevenueReport_Success() {
        LocalDate date = LocalDate.of(2026, 4, 1);
        when(bookingRepository.getDailyRevenue(date)).thenReturn(BigDecimal.valueOf(5000));
        when(bookingRepository.countByCheckInDateAndStatus(date, "CONFIRMED")).thenReturn(3L);

        DailyRevenueReportDto result = reportService.getDailyRevenueReport(date);

        assertThat(result.getDate()).isEqualTo(date);
        assertThat(result.getTotalRevenue()).isEqualByComparingTo("5000");
        assertThat(result.getTotalBookings()).isEqualTo(3L);
    }

    @Test
    void getDailyRevenueReport_NoData() {
        LocalDate date = LocalDate.of(2026, 4, 1);
        when(bookingRepository.getDailyRevenue(date)).thenReturn(BigDecimal.ZERO);
        when(bookingRepository.countByCheckInDateAndStatus(date, "CONFIRMED")).thenReturn(0L);

        DailyRevenueReportDto result = reportService.getDailyRevenueReport(date);

        assertThat(result.getDate()).isEqualTo(date);
        assertThat(result.getTotalRevenue()).isEqualByComparingTo("0");
        assertThat(result.getTotalBookings()).isZero();
    }

    // ========== getDailyRevenueReportRange ==========
    @Test
    void getDailyRevenueReportRange_Success() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 5);

        List<Map<String, Object>> mockResults = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("date", Date.valueOf("2026-04-01"));
        row1.put("revenue", BigDecimal.valueOf(1000));
        row1.put("count", 1L);
        mockResults.add(row1);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("date", Date.valueOf("2026-04-02"));
        row2.put("revenue", BigDecimal.valueOf(2000));
        row2.put("count", 2L);
        mockResults.add(row2);

        when(bookingRepository.getDailyRevenueBetween(start, end)).thenReturn(mockResults);

        List<DailyRevenueReportDto> result = reportService.getDailyRevenueReportRange(start, end);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(result.get(0).getTotalRevenue()).isEqualByComparingTo("1000");
        assertThat(result.get(0).getTotalBookings()).isEqualTo(1L);
        assertThat(result.get(1).getDate()).isEqualTo(LocalDate.of(2026, 4, 2));
        assertThat(result.get(1).getTotalRevenue()).isEqualByComparingTo("2000");
        assertThat(result.get(1).getTotalBookings()).isEqualTo(2L);
    }

    @Test
    void getDailyRevenueReportRange_Empty() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 5);
        when(bookingRepository.getDailyRevenueBetween(start, end)).thenReturn(Collections.emptyList());

        List<DailyRevenueReportDto> result = reportService.getDailyRevenueReportRange(start, end);

        assertThat(result).isEmpty();
    }

    // ========== getMonthlyOccupancyReport ==========
    @Test
    void getMonthlyOccupancyReport_Success() {
        int year = 2026;
        int month = 4; // April, 30 days
        when(bookingRepository.countOccupiedRoomNights(year, month)).thenReturn(45L);
        when(roomRepository.count()).thenReturn(10L); // 10 rooms

        MonthlyOccupancyReportDto result = reportService.getMonthlyOccupancyReport(year, month);

        assertThat(result.getMonth()).isEqualTo("2026-04");
        assertThat(result.getOccupiedRoomNights()).isEqualTo(45L);
        assertThat(result.getTotalAvailableRoomNights()).isEqualTo(300L); // 10 rooms * 30 days
        assertThat(result.getOccupancyPercentage()).isEqualTo(15.0); // (45/300)*100 = 15.0
    }

    @Test
    void getMonthlyOccupancyReport_NoOccupiedNights() {
        int year = 2026;
        int month = 4;
        when(bookingRepository.countOccupiedRoomNights(year, month)).thenReturn(0L);
        when(roomRepository.count()).thenReturn(10L);

        MonthlyOccupancyReportDto result = reportService.getMonthlyOccupancyReport(year, month);

        assertThat(result.getOccupiedRoomNights()).isZero();
        assertThat(result.getTotalAvailableRoomNights()).isEqualTo(300L);
        assertThat(result.getOccupancyPercentage()).isZero();
    }

    @Test
    void getMonthlyOccupancyReport_ZeroRooms() {
        int year = 2026;
        int month = 4;
        when(bookingRepository.countOccupiedRoomNights(year, month)).thenReturn(10L);
        when(roomRepository.count()).thenReturn(0L); // no rooms

        MonthlyOccupancyReportDto result = reportService.getMonthlyOccupancyReport(year, month);

        assertThat(result.getOccupiedRoomNights()).isEqualTo(10L);
        assertThat(result.getTotalAvailableRoomNights()).isZero();
        assertThat(result.getOccupancyPercentage()).isZero(); // division by zero avoided
    }

    @Test
    void getMonthlyOccupancyReport_OccupiedNightsNull() {
        int year = 2026;
        int month = 4;
        when(bookingRepository.countOccupiedRoomNights(year, month)).thenReturn(null); // should not happen, but safe
        when(roomRepository.count()).thenReturn(10L);

        MonthlyOccupancyReportDto result = reportService.getMonthlyOccupancyReport(year, month);
    }

    // ========== getCustomerHistory ==========
    @Test
    void getCustomerHistory_Success() {
        Long customerId = 1L;

        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setName("John Doe");

        Room room = new Room();
        room.setRoomId(10L);
        room.setRoomNumber("101");

        Booking booking1 = new Booking();
        booking1.setBookingId(100L);
        booking1.setCustomer(customer);
        booking1.setRoom(room);
        booking1.setCheckIn(LocalDate.of(2026, 4, 1));
        booking1.setCheckOut(LocalDate.of(2026, 4, 5));
        booking1.setTotalAmount(BigDecimal.valueOf(4000));
        booking1.setStatus("CONFIRMED");

        Booking booking2 = new Booking();
        booking2.setBookingId(101L);
        booking2.setCustomer(customer);
        booking2.setRoom(room);
        booking2.setCheckIn(LocalDate.of(2026, 5, 10));
        booking2.setCheckOut(LocalDate.of(2026, 5, 12));
        booking2.setTotalAmount(BigDecimal.valueOf(2000));
        booking2.setStatus("CANCELLED");

        List<Booking> bookings = List.of(booking1, booking2);
        when(bookingRepository.findByCustomerIdOrderByCheckInDesc(customerId)).thenReturn(bookings);

        List<CustomerHistoryReportDto> result = reportService.getCustomerHistory(customerId);

        assertThat(result).hasSize(2);
        CustomerHistoryReportDto dto1 = result.get(0);
        assertThat(dto1.getBookingId()).isEqualTo(100L);
        assertThat(dto1.getCheckIn()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(dto1.getCheckOut()).isEqualTo(LocalDate.of(2026, 4, 5));
        assertThat(dto1.getRoomNumber()).isEqualTo("101");
        assertThat(dto1.getTotalAmount()).isEqualByComparingTo("4000");
        assertThat(dto1.getStatus()).isEqualTo("CONFIRMED");

        CustomerHistoryReportDto dto2 = result.get(1);
        assertThat(dto2.getBookingId()).isEqualTo(101L);
        assertThat(dto2.getCheckIn()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(dto2.getCheckOut()).isEqualTo(LocalDate.of(2026, 5, 12));
        assertThat(dto2.getRoomNumber()).isEqualTo("101");
        assertThat(dto2.getTotalAmount()).isEqualByComparingTo("2000");
        assertThat(dto2.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void getCustomerHistory_NoBookings() {
        Long customerId = 1L;
        when(bookingRepository.findByCustomerIdOrderByCheckInDesc(customerId)).thenReturn(Collections.emptyList());

        List<CustomerHistoryReportDto> result = reportService.getCustomerHistory(customerId);

        assertThat(result).isEmpty();
    }

    @Test
    void getCustomerHistory_NullList() {
        Long customerId = 1L;
        when(bookingRepository.findByCustomerIdOrderByCheckInDesc(customerId)).thenReturn(null);

        List<CustomerHistoryReportDto> result = reportService.getCustomerHistory(customerId);
        assertThat(result).isEmpty();
    }
}