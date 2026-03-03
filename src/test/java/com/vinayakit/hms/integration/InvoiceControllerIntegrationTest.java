package com.vinayakit.hms.integration;

import com.jayway.jsonpath.JsonPath;
import com.vinayakit.hms.dto.ApiResponse;
import com.vinayakit.hms.entity.Booking;
import com.vinayakit.hms.entity.Customer;
import com.vinayakit.hms.entity.Room;
import com.vinayakit.hms.repository.BookingRepository;
import com.vinayakit.hms.repository.CustomerRepository;
import com.vinayakit.hms.repository.RoomRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvoiceControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @MockitoBean
    private JavaMailSender mailSender;

    private Booking testBooking;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        customerRepository.deleteAll();
        roomRepository.deleteAll();

        Customer customer = new Customer();
        customer.setName("Invoice Test");
        customer.setEmail("invoice@example.com");
        customer.setPhone("1234567890");
        customer.setAddress("123 Test St");
        customer = customerRepository.save(customer);

        Room room = new Room();
        room.setRoomNumber("101");
        room.setRoomType("Standard");
        room.setPrice(BigDecimal.valueOf(1000));
        room.setStatus("AVAILABLE");
        room = roomRepository.save(room);

        testBooking = new Booking();
        testBooking.setCustomer(customer);
        testBooking.setRoom(room);
        testBooking.setCheckIn(LocalDate.now().plusDays(10));
        testBooking.setCheckOut(LocalDate.now().plusDays(12));
        testBooking.setTotalAmount(BigDecimal.valueOf(2000));
        testBooking.setStatus("CONFIRMED");
        testBooking = bookingRepository.save(testBooking);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    // ========== GET /api/invoice/{bookingId} ==========
    @Test
    void getInvoicePdf_Admin_Success() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));

        ResponseEntity<byte[]> response = restTemplate.exchange(
                baseUrl + "/invoice/" + testBooking.getBookingId(),
                HttpMethod.GET,
                entity,
                byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("invoice-" + testBooking.getBookingId() + ".pdf");
        byte[] pdfBytes = response.getBody();
        assertThat(pdfBytes).isNotEmpty();
        // Check PDF header
        assertThat(pdfBytes[0]).isEqualTo((byte) '%');
        assertThat(pdfBytes[1]).isEqualTo((byte) 'P');
        assertThat(pdfBytes[2]).isEqualTo((byte) 'D');
        assertThat(pdfBytes[3]).isEqualTo((byte) 'F');
    }

    @Test
    void getInvoicePdf_Receptionist_Success() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getReceptionistToken()));

        ResponseEntity<byte[]> response = restTemplate.exchange(
                baseUrl + "/invoice/" + testBooking.getBookingId(),
                HttpMethod.GET,
                entity,
                byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
    }

    @Test
    void getInvoicePdf_Manager_Success() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getManagerToken()));

        ResponseEntity<byte[]> response = restTemplate.exchange(
                baseUrl + "/invoice/" + testBooking.getBookingId(),
                HttpMethod.GET,
                entity,
                byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getInvoicePdf_Unauthorized_Forbidden() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/invoice/" + testBooking.getBookingId(),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getInvoicePdf_NotFound() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/invoice/9999",
                HttpMethod.GET,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ========== POST /api/invoice/{bookingId}/send ==========
    @Test
    void sendInvoiceEmail_Admin_Success() {
        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));

        ResponseEntity<ApiResponse<String>> response = restTemplate.exchange(
                baseUrl + "/invoice/" + testBooking.getBookingId() + "/send",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<ApiResponse<String>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<String> apiResponse = response.getBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();
        assertThat(apiResponse.getData()).contains("Invoice sent successfully");

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                verify(mailSender).send(any(MimeMessage.class))
        );
    }

    @Test
    void sendInvoiceEmail_Receptionist_Success() {
        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getReceptionistToken()));

        ResponseEntity<ApiResponse<String>> response = restTemplate.exchange(
                baseUrl + "/invoice/" + testBooking.getBookingId() + "/send",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<ApiResponse<String>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<String> apiResponse = response.getBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                verify(mailSender).send(any(MimeMessage.class))
        );
    }

    @Test
    void sendInvoiceEmail_Manager_Success() {
        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getManagerToken()));

        ResponseEntity<ApiResponse<String>> response = restTemplate.exchange(
                baseUrl + "/invoice/" + testBooking.getBookingId() + "/send",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<ApiResponse<String>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<String> apiResponse = response.getBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                verify(mailSender).send(any(MimeMessage.class))
        );
    }

    @Test
    void sendInvoiceEmail_Unauthorized_Forbidden() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/invoice/" + testBooking.getBookingId() + "/send",
                null,
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void sendInvoiceEmail_NotFound() {
        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/invoice/9999/send",
                HttpMethod.POST,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}