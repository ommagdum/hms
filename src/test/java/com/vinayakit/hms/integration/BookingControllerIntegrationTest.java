package com.vinayakit.hms.integration;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.vinayakit.hms.dto.ApiResponse;
import com.vinayakit.hms.dto.BookingDto;
import com.vinayakit.hms.dto.RoomBookingDto;
import com.vinayakit.hms.entity.Booking;
import com.vinayakit.hms.entity.Customer;
import com.vinayakit.hms.entity.Room;
import com.vinayakit.hms.repository.BookingRepository;
import com.vinayakit.hms.repository.CustomerRepository;
import com.vinayakit.hms.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BookingControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private Customer testCustomer;
    private Room testRoom;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        customerRepository.deleteAll();
        roomRepository.deleteAll();

        testCustomer = new Customer();
        testCustomer.setName("John Doe");
        testCustomer.setEmail("john@example.com");
        testCustomer.setPhone("1234567890");
        testCustomer.setAddress("123 Main St");
        testCustomer = customerRepository.save(testCustomer);

        testRoom = new Room();
        testRoom.setRoomNumber("101");
        testRoom.setRoomType("Standard");
        testRoom.setPrice(BigDecimal.valueOf(1000));
        testRoom.setStatus("AVAILABLE");
        testRoom = roomRepository.save(testRoom);

        testBooking = new Booking();
        testBooking.setCustomer(testCustomer);
        testBooking.setRoom(testRoom);
        testBooking.setCheckIn(LocalDate.now().plusDays(5));
        testBooking.setCheckOut(LocalDate.now().plusDays(7));
        testBooking.setTotalAmount(BigDecimal.valueOf(2000));
        testBooking.setStatus("CONFIRMED");
        testBooking = bookingRepository.save(testBooking);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    // ========== POST /api/bookings ==========
    @Test
    void createBooking_Success() {
        BookingDto newBooking = new BookingDto();
        newBooking.setCustomerId(testCustomer.getCustomerId());
        newBooking.setRoomId(testRoom.getRoomId());
        newBooking.setCheckIn(LocalDate.now().plusDays(10));
        newBooking.setCheckOut(LocalDate.now().plusDays(12));

        HttpEntity<BookingDto> entity = new HttpEntity<>(newBooking, authHeaders(getAdminToken()));

        ResponseEntity<ApiResponse<BookingDto>> response = restTemplate.exchange(
                baseUrl + "/bookings",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<ApiResponse<BookingDto>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ApiResponse<BookingDto> apiResponse = response.getBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();
        BookingDto created = apiResponse.getData();
        assertThat(created).isNotNull();
        assertThat(created.getBookingId()).isNotNull();
        assertThat(created.getTotalAmount()).isEqualByComparingTo("2000");
    }

    @Test
    void createBooking_RoomUnavailable() {
        BookingDto newBooking = new BookingDto();
        newBooking.setCustomerId(testCustomer.getCustomerId());
        newBooking.setRoomId(testRoom.getRoomId());
        newBooking.setCheckIn(LocalDate.now().plusDays(5));
        newBooking.setCheckOut(LocalDate.now().plusDays(8));

        HttpEntity<BookingDto> entity = new HttpEntity<>(newBooking, authHeaders(getAdminToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/bookings",
                HttpMethod.POST,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createBooking_InvalidDate_PastCheckIn() {
        BookingDto newBooking = new BookingDto();
        newBooking.setCustomerId(testCustomer.getCustomerId());
        newBooking.setRoomId(testRoom.getRoomId());
        newBooking.setCheckIn(LocalDate.now().minusDays(1));
        newBooking.setCheckOut(LocalDate.now().plusDays(2));

        HttpEntity<BookingDto> entity = new HttpEntity<>(newBooking, authHeaders(getAdminToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/bookings",
                HttpMethod.POST,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ========== GET /api/bookings ==========
    @Test
    void getAllBookings_NoFilter() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/bookings",
                HttpMethod.GET,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext jsonContext = JsonPath.parse(response.getBody());
        int totalElements = jsonContext.read("$.data.totalElements");
        assertThat(totalElements).isGreaterThanOrEqualTo(1);
    }

    // ========== GET /api/bookings/{id} ==========
    @Test
    void getBookingById_Success() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));

        ResponseEntity<ApiResponse<BookingDto>> response = restTemplate.exchange(
                baseUrl + "/bookings/" + testBooking.getBookingId(),
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<ApiResponse<BookingDto>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<BookingDto> apiResponse = response.getBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();
        BookingDto booking = apiResponse.getData();
        assertThat(booking).isNotNull();
        assertThat(booking.getBookingId()).isEqualTo(testBooking.getBookingId());
    }

    @Test
    void getBookingById_NotFound() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/bookings/9999",
                HttpMethod.GET,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ========== PUT /api/bookings/{id}/cancel ==========
    @Test
    void cancelBooking_Success() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));

        ResponseEntity<ApiResponse<BookingDto>> response = restTemplate.exchange(
                baseUrl + "/bookings/" + testBooking.getBookingId() + "/cancel",
                HttpMethod.PUT,
                entity,
                new ParameterizedTypeReference<ApiResponse<BookingDto>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<BookingDto> apiResponse = response.getBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();
        BookingDto cancelled = apiResponse.getData();
        assertThat(cancelled).isNotNull();
        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelBooking_AlreadyCancelled() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));
        restTemplate.exchange(
                baseUrl + "/bookings/" + testBooking.getBookingId() + "/cancel",
                HttpMethod.PUT,
                entity,
                new ParameterizedTypeReference<ApiResponse<BookingDto>>() {});

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/bookings/" + testBooking.getBookingId() + "/cancel",
                HttpMethod.PUT,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ========== GET /api/bookings/customer/{customerId} ==========
    @Test
    void getBookingsByCustomer_Success() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/bookings/customer/" + testCustomer.getCustomerId(),
                HttpMethod.GET,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Use JsonPath to parse the response
        DocumentContext jsonContext = JsonPath.parse(response.getBody());
        int totalElements = jsonContext.read("$.data.totalElements");
        assertThat(totalElements).isEqualTo(1);
    }

    @Test
    void getBookingsInRange_Success() {
        LocalDate start = LocalDate.now().plusDays(4);
        LocalDate end = LocalDate.now().plusDays(8);

        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/bookings/range?startDate=" + start + "&endDate=" + end,
                HttpMethod.GET,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext jsonContext = JsonPath.parse(response.getBody());
        List<?> data = jsonContext.read("$.data");
        assertThat(data).isNotEmpty();
    }

    // ========== PUT /api/bookings/{id} ==========
    @Test
    void updateBooking_Success() {
        BookingDto updateDto = new BookingDto();
        updateDto.setCustomerId(testCustomer.getCustomerId());
        updateDto.setRoomId(testRoom.getRoomId());
        updateDto.setCheckIn(LocalDate.now().plusDays(8));
        updateDto.setCheckOut(LocalDate.now().plusDays(10));

        HttpEntity<BookingDto> entity = new HttpEntity<>(updateDto, authHeaders(getAdminToken()));

        ResponseEntity<ApiResponse<BookingDto>> response = restTemplate.exchange(
                baseUrl + "/bookings/" + testBooking.getBookingId(),
                HttpMethod.PUT,
                entity,
                new ParameterizedTypeReference<ApiResponse<BookingDto>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<BookingDto> apiResponse = response.getBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();
        BookingDto updated = apiResponse.getData();
        assertThat(updated).isNotNull();
        assertThat(updated.getCheckIn()).isEqualTo(LocalDate.now().plusDays(8));
    }

    // ========== Role‑based access ==========
    @Test
    void receptionistCanAccessBookings() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getReceptionistToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/bookings",
                HttpMethod.GET,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void managerCanAccessBookings() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getManagerToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/bookings",
                HttpMethod.GET,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}