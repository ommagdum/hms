package com.vinayakit.hms.integration;

import com.vinayakit.hms.dto.ApiResponse;
import com.vinayakit.hms.dto.LoginRequest;
import com.vinayakit.hms.dto.LoginResponseDto;
import com.vinayakit.hms.entity.Staff;
import com.vinayakit.hms.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected StaffRepository staffRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected String baseUrl;

    @BeforeEach
    public void setUpBase() {
        baseUrl = "http://localhost:" + port + "/api";
        staffRepository.deleteAll();

        // Create admin
        Staff admin = new Staff();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole("ADMIN");
        admin.setEnabled(true);
        admin.setName("Admin User");
        admin.setContact("0000000000");
        admin.setSalary(BigDecimal.ZERO);
        staffRepository.save(admin);

        // Create receptionist
        Staff receptionist = new Staff();
        receptionist.setUsername("reception");
        receptionist.setPassword(passwordEncoder.encode("reception123"));
        receptionist.setRole("RECEPTIONIST");
        receptionist.setEnabled(true);
        receptionist.setName("Reception User");
        receptionist.setContact("1111111111");
        receptionist.setSalary(BigDecimal.ZERO);
        staffRepository.save(receptionist);

        // Create manager
        Staff manager = new Staff();
        manager.setUsername("manager");
        manager.setPassword(passwordEncoder.encode("manager123"));
        manager.setRole("MANAGER");
        manager.setEnabled(true);
        manager.setName("Manager User");
        manager.setContact("2222222222");
        manager.setSalary(BigDecimal.ZERO);
        staffRepository.save(manager);
    }

    protected String getAdminToken() {
        return getToken("admin", "admin123");
    }

    protected String getReceptionistToken() {
        return getToken("reception", "reception123");
    }

    protected String getManagerToken() {
        return getToken("manager", "manager123");
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
}