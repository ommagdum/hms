package com.vinayakit.hms.integration;

import com.vinayakit.hms.dto.ApiResponse;
import com.vinayakit.hms.dto.LoginRequest;
import com.vinayakit.hms.dto.LoginResponseDto;
import com.vinayakit.hms.entity.Staff;
import com.vinayakit.hms.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AuthIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";
        staffRepository.deleteAll();

        // Create admin user
        Staff admin = new Staff();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole("ADMIN");
        admin.setEnabled(true);
        admin.setName("Admin User");
        admin.setContact("0000000000");
        admin.setSalary(BigDecimal.ZERO);
        staffRepository.save(admin);

        // Create receptionist user
        Staff receptionist = new Staff();
        receptionist.setUsername("reception");
        receptionist.setPassword(passwordEncoder.encode("reception123"));
        receptionist.setRole("RECEPTIONIST");
        receptionist.setEnabled(true);
        receptionist.setName("Reception User");
        receptionist.setContact("1111111111");
        receptionist.setSalary(BigDecimal.ZERO);
        staffRepository.save(receptionist);
    }

    private String getToken(String username, String password) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        ResponseEntity<ApiResponse<LoginResponseDto>> response = restTemplate.exchange(
                baseUrl + "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<ApiResponse<LoginResponseDto>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<LoginResponseDto> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.isSuccess()).isTrue();
        assertThat(body.getData()).isNotNull();
        return body.getData().getToken();
    }

    @Test
    void testLoginSuccess() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("admin123");

        ResponseEntity<ApiResponse<LoginResponseDto>> response = restTemplate.exchange(
                baseUrl + "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<ApiResponse<LoginResponseDto>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<LoginResponseDto> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.isSuccess()).isTrue();
        assertThat(body.getData()).isNotNull();
        assertThat(body.getData().getToken()).isNotBlank();
        assertThat(body.getData().getUsername()).isEqualTo("admin");
        assertThat(body.getData().getRole()).isEqualTo("ADMIN");
    }

    @Test
    void testLoginFailure() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("wrongpassword");

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/auth/login",
                loginRequest,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testAccessSecuredEndpointWithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/bookings",
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void testAccessSecuredEndpointWithValidToken() {
        String token = getToken("admin", "admin123");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/bookings",
                HttpMethod.GET,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testAdminAccessStaffEndpoint() {
        String token = getToken("admin", "admin123");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/staff",
                HttpMethod.GET,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testReceptionistCannotAccessStaffEndpoint() {
        String token = getToken("reception", "reception123");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/staff",
                HttpMethod.GET,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void testReceptionistCanAccessBookingsEndpoint() {
        String token = getToken("reception", "reception123");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/bookings",
                HttpMethod.GET,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}