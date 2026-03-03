package com.vinayakit.hms.integration;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.vinayakit.hms.entity.Booking;
import com.vinayakit.hms.entity.Customer;
import com.vinayakit.hms.entity.Room;
import com.vinayakit.hms.repository.BookingRepository;
import com.vinayakit.hms.repository.CustomerRepository;
import com.vinayakit.hms.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @BeforeEach
    void setUp() {
        // Clean up any previous test data (but not staff)
        bookingRepository.deleteAll();
        customerRepository.deleteAll();
        roomRepository.deleteAll();

        // Create rooms
        Room room1 = new Room();
        room1.setRoomNumber("101");
        room1.setRoomType("Standard");
        room1.setPrice(BigDecimal.valueOf(1000));
        room1.setStatus("AVAILABLE");
        roomRepository.save(room1);

        Room room2 = new Room();
        room2.setRoomNumber("102");
        room2.setRoomType("Deluxe");
        room2.setPrice(BigDecimal.valueOf(2000));
        room2.setStatus("OCCUPIED"); // for occupancy distribution
        roomRepository.save(room2);

        // Create customer
        Customer customer = new Customer();
        customer.setName("Dashboard Tester");
        customer.setEmail("dashboard@example.com");
        customer.setPhone("1112223333");
        customer.setAddress("123 Test St");
        customer = customerRepository.save(customer);

        // Create a booking for today (check-in) – for recent activity
        Booking todayCheckIn = new Booking();
        todayCheckIn.setCustomer(customer);
        todayCheckIn.setRoom(room2);
        todayCheckIn.setCheckIn(LocalDate.now());
        todayCheckIn.setCheckOut(LocalDate.now().plusDays(2));
        todayCheckIn.setTotalAmount(BigDecimal.valueOf(4000)); // 2 nights * 2000
        todayCheckIn.setStatus("CONFIRMED");
        todayCheckIn.setCreatedAt(LocalDateTime.now().with(LocalTime.of(9, 30))); // 9:30 AM
        bookingRepository.save(todayCheckIn);

        // Create a booking with check-out today – for recent activity
        Booking todayCheckOut = new Booking();
        todayCheckOut.setCustomer(customer);
        todayCheckOut.setRoom(room1);
        todayCheckOut.setCheckIn(LocalDate.now().minusDays(3));
        todayCheckOut.setCheckOut(LocalDate.now());
        todayCheckOut.setTotalAmount(BigDecimal.valueOf(3000)); // 3 nights * 1000
        todayCheckOut.setStatus("CONFIRMED");
        todayCheckOut.setUpdatedAt(LocalDateTime.now().with(LocalTime.of(11, 0))); // 11:00 AM
        bookingRepository.save(todayCheckOut);

        // Create a booking in the past (for revenue)
        Booking pastBooking = new Booking();
        pastBooking.setCustomer(customer);
        pastBooking.setRoom(room1);
        pastBooking.setCheckIn(LocalDate.now().minusDays(5));
        pastBooking.setCheckOut(LocalDate.now().minusDays(3));
        pastBooking.setTotalAmount(BigDecimal.valueOf(2000));
        pastBooking.setStatus("CONFIRMED");
        bookingRepository.save(pastBooking);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void getDashboardSummary_Admin_Success() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/dashboard/summary",
                HttpMethod.GET,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext json = JsonPath.parse(response.getBody());

        // Verify counters
        int totalRooms = json.read("$.data.counters.totalRooms");
        assertThat(totalRooms).isEqualTo(2);

        int activeBookings = json.read("$.data.counters.activeBookings");
        assertThat(activeBookings).isGreaterThanOrEqualTo(1); // at least today's check-in

        // Occupancy distribution
        int occupancyCount = json.read("$.data.occupancyDistribution.length()");
        assertThat(occupancyCount).isGreaterThanOrEqualTo(1);

        // Revenue chart (should have 7 entries)
        int revenueChartSize = json.read("$.data.revenueChart.length()");
        assertThat(revenueChartSize).isEqualTo(7);

        // Recent activity (should have at least 2 entries)
        int recentActivitySize = json.read("$.data.recentActivity.length()");
        assertThat(recentActivitySize).isGreaterThanOrEqualTo(2);
    }

    @Test
    void getDashboardSummary_Manager_Success() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getManagerToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/dashboard/summary",
                HttpMethod.GET,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getDashboardSummary_Receptionist_Forbidden() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getReceptionistToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/dashboard/summary",
                HttpMethod.GET,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getDashboardSummary_Unauthorized_Forbidden() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/dashboard/summary",
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}