package com.vinayakit.hms.integration;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.vinayakit.hms.dto.ApiResponse;
import com.vinayakit.hms.dto.RoomDto;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoomControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Room testRoom;
    private Customer testCustomer;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        roomRepository.deleteAll();
        customerRepository.deleteAll();

        // Create a test room
        testRoom = new Room();
        testRoom.setRoomNumber("101");
        testRoom.setRoomType("Standard");
        testRoom.setPrice(BigDecimal.valueOf(1000));
        testRoom.setStatus("AVAILABLE");
        testRoom = roomRepository.save(testRoom);

        // Create a second room for availability tests
        Room room2 = new Room();
        room2.setRoomNumber("102");
        room2.setRoomType("Deluxe");
        room2.setPrice(BigDecimal.valueOf(2000));
        room2.setStatus("AVAILABLE");
        roomRepository.save(room2);

        // Create a customer and a booking for a room (to make it unavailable)
        testCustomer = new Customer();
        testCustomer.setName("Test Customer");
        testCustomer.setEmail("test@example.com");
        testCustomer.setPhone("1234567890");
        testCustomer.setAddress("Address");
        testCustomer = customerRepository.save(testCustomer);

        testBooking = new Booking();
        testBooking.setCustomer(testCustomer);
        testBooking.setRoom(testRoom);
        testBooking.setCheckIn(LocalDate.now().plusDays(2));
        testBooking.setCheckOut(LocalDate.now().plusDays(5));
        testBooking.setTotalAmount(BigDecimal.valueOf(3000));
        testBooking.setStatus("CONFIRMED");
        testBooking = bookingRepository.save(testBooking);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    // ========== POST /api/rooms ==========
    @Test
    void createRoom_Success() {
        RoomDto newRoom = new RoomDto();
        newRoom.setRoomNumber("201");
        newRoom.setRoomType("Suite");
        newRoom.setPrice(BigDecimal.valueOf(3000));
        newRoom.setStatus("AVAILABLE");

        HttpEntity<RoomDto> entity = new HttpEntity<>(newRoom, authHeaders(getAdminToken()));

        ResponseEntity<ApiResponse<RoomDto>> response = restTemplate.exchange(
                baseUrl + "/rooms",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<ApiResponse<RoomDto>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ApiResponse<RoomDto> apiResponse = response.getBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();
        RoomDto created = apiResponse.getData();
        assertThat(created).isNotNull();
        assertThat(created.getRoomId()).isNotNull();
        assertThat(created.getRoomNumber()).isEqualTo("201");
    }

    @Test
    void createRoom_DuplicateRoomNumber() {
        RoomDto duplicateRoom = new RoomDto();
        duplicateRoom.setRoomNumber(testRoom.getRoomNumber()); // existing number
        duplicateRoom.setRoomType("Deluxe");
        duplicateRoom.setPrice(BigDecimal.valueOf(1500));
        duplicateRoom.setStatus("AVAILABLE");

        HttpEntity<RoomDto> entity = new HttpEntity<>(duplicateRoom, authHeaders(getAdminToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/rooms",
                HttpMethod.POST,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Room number already exists");
    }

    @Test
    void createRoom_ValidationFailure() {
        RoomDto invalidRoom = new RoomDto();
        invalidRoom.setRoomNumber(""); // blank
        invalidRoom.setRoomType("Standard");
        invalidRoom.setPrice(BigDecimal.valueOf(-100)); // negative price
        invalidRoom.setStatus("AVAILABLE");

        HttpEntity<RoomDto> entity = new HttpEntity<>(invalidRoom, authHeaders(getAdminToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/rooms",
                HttpMethod.POST,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ========== GET /api/rooms ==========
    @Test
    void getAllRooms() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/rooms",
                HttpMethod.GET,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext jsonContext = JsonPath.parse(response.getBody());
        int totalElements = jsonContext.read("$.data.totalElements");
        assertThat(totalElements).isGreaterThanOrEqualTo(2); // we have at least two rooms
    }

    // ========== GET /api/rooms/{id} ==========
    @Test
    void getRoomById_Success() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));

        ResponseEntity<ApiResponse<RoomDto>> response = restTemplate.exchange(
                baseUrl + "/rooms/" + testRoom.getRoomId(),
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<ApiResponse<RoomDto>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<RoomDto> apiResponse = response.getBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();
        RoomDto room = apiResponse.getData();
        assertThat(room).isNotNull();
        assertThat(room.getRoomId()).isEqualTo(testRoom.getRoomId());
    }

    @Test
    void getRoomById_NotFound() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/rooms/9999",
                HttpMethod.GET,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ========== PUT /api/rooms/{id} ==========
    @Test
    void updateRoom_Success() {
        RoomDto updateDto = new RoomDto();
        updateDto.setRoomNumber("101-Upd"); // within 10 characters
        updateDto.setRoomType("Deluxe");
        updateDto.setPrice(BigDecimal.valueOf(1500));
        updateDto.setStatus("MAINTENANCE");

        HttpEntity<RoomDto> entity = new HttpEntity<>(updateDto, authHeaders(getAdminToken()));

        ResponseEntity<ApiResponse<RoomDto>> response = restTemplate.exchange(
                baseUrl + "/rooms/" + testRoom.getRoomId(),
                HttpMethod.PUT,
                entity,
                new ParameterizedTypeReference<ApiResponse<RoomDto>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<RoomDto> apiResponse = response.getBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();
        RoomDto updated = apiResponse.getData();
        assertThat(updated).isNotNull();
        assertThat(updated.getRoomNumber()).isEqualTo("101-Upd");
    }

    @Test
    void updateRoom_NotFound() {
        RoomDto updateDto = new RoomDto();
        updateDto.setRoomNumber("Temp");
        updateDto.setRoomType("Temp");
        updateDto.setPrice(BigDecimal.ONE);
        updateDto.setStatus("AVAILABLE");

        HttpEntity<RoomDto> entity = new HttpEntity<>(updateDto, authHeaders(getAdminToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/rooms/9999",
                HttpMethod.PUT,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateRoom_DuplicateRoomNumber() {
        // Create another room
        Room anotherRoom = new Room();
        anotherRoom.setRoomNumber("103");
        anotherRoom.setRoomType("Standard");
        anotherRoom.setPrice(BigDecimal.valueOf(1000));
        anotherRoom.setStatus("AVAILABLE");
        anotherRoom = roomRepository.save(anotherRoom);

        RoomDto updateDto = new RoomDto();
        updateDto.setRoomNumber(anotherRoom.getRoomNumber()); // existing number
        updateDto.setRoomType("Deluxe");
        updateDto.setPrice(BigDecimal.valueOf(1500));
        updateDto.setStatus("AVAILABLE");

        HttpEntity<RoomDto> entity = new HttpEntity<>(updateDto, authHeaders(getAdminToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/rooms/" + testRoom.getRoomId(),
                HttpMethod.PUT,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Room number already exists");
    }

    // ========== DELETE /api/rooms/{id} ==========
    @Test
    void deleteRoom_Success() {
        // Create a room with no bookings for deletion
        Room roomToDelete = new Room();
        roomToDelete.setRoomNumber("104");
        roomToDelete.setRoomType("Standard");
        roomToDelete.setPrice(BigDecimal.valueOf(1000));
        roomToDelete.setStatus("AVAILABLE");
        roomToDelete = roomRepository.save(roomToDelete);

        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));

        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                baseUrl + "/rooms/" + roomToDelete.getRoomId(),
                HttpMethod.DELETE,
                entity,
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<Void> apiResponse = response.getBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();
    }

    @Test
    void deleteRoom_NotFound() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/rooms/9999",
                HttpMethod.DELETE,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ========== GET /api/rooms/available ==========
    @Test
    void getAvailableRooms_Success() {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);

        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));

        ResponseEntity<ApiResponse<List<RoomDto>>> response = restTemplate.exchange(
                baseUrl + "/rooms/available?checkIn=" + checkIn + "&checkOut=" + checkOut,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<ApiResponse<List<RoomDto>>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<List<RoomDto>> apiResponse = response.getBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();
        List<RoomDto> rooms = apiResponse.getData();
        assertThat(rooms).isNotNull();
        assertThat(rooms).isNotEmpty();
    }

    @Test
    void getAvailableRooms_InvalidDates() {
        LocalDate checkIn = LocalDate.now().plusDays(3);
        LocalDate checkOut = LocalDate.now().plusDays(1); // checkIn after checkOut

        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/rooms/available?checkIn=" + checkIn + "&checkOut=" + checkOut,
                HttpMethod.GET,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ========== Role-based access ==========
    @Test
    void adminCanAccessRooms() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/rooms",
                HttpMethod.GET,
                entity,
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void receptionistCanAccessRooms() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getReceptionistToken()));
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/rooms",
                HttpMethod.GET,
                entity,
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void managerCannotAccessRooms() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getManagerToken()));
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/rooms",
                HttpMethod.GET,
                entity,
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void unauthorizedUserCannotAccessRooms() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/rooms",
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}