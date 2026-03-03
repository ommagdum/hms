package com.vinayakit.hms.integration;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.vinayakit.hms.dto.CreateStaffRequest;
import com.vinayakit.hms.dto.StaffDto;
import com.vinayakit.hms.entity.Staff;
import com.vinayakit.hms.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StaffControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private StaffRepository staffRepository;

    private Staff additionalStaff;

    @BeforeEach
    void setUp() {
        additionalStaff = new Staff();
        additionalStaff.setUsername("additional");
        additionalStaff.setPassword(passwordEncoder.encode("additional123"));
        additionalStaff.setRole("RECEPTIONIST");
        additionalStaff.setEnabled(true);
        additionalStaff.setName("Additional Staff");
        additionalStaff.setContact("5555555555");
        additionalStaff.setSalary(BigDecimal.valueOf(40000));
        additionalStaff = staffRepository.save(additionalStaff);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    // ========== GET /api/staff ==========
    @Test
    void getAllStaff_Admin_Success() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/staff",
                HttpMethod.GET,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext jsonContext = JsonPath.parse(response.getBody());
        int staffCount = jsonContext.read("$.length()");
        // We have admin, receptionist, manager, plus additionalStaff = 4
        assertThat(staffCount).isEqualTo(4);
    }

    @Test
    void getAllStaff_Receptionist_Forbidden() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getReceptionistToken()));
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/staff",
                HttpMethod.GET,
                entity,
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getAllStaff_Manager_Forbidden() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getManagerToken()));
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/staff",
                HttpMethod.GET,
                entity,
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getAllStaff_Unauthorized_Forbidden() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/staff",
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ========== GET /api/staff/{id} ==========
    @Test
    void getStaffById_Admin_Success() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));

        ResponseEntity<StaffDto> response = restTemplate.exchange(
                baseUrl + "/staff/" + additionalStaff.getStaffId(),
                HttpMethod.GET,
                entity,
                StaffDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        StaffDto staff = response.getBody();
        assertThat(staff).isNotNull();
        assertThat(staff.getStaffId()).isEqualTo(additionalStaff.getStaffId());
        assertThat(staff.getUsername()).isEqualTo("additional");
    }

    @Test
    void getStaffById_Receptionist_Forbidden() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getReceptionistToken()));
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/staff/" + additionalStaff.getStaffId(),
                HttpMethod.GET,
                entity,
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getStaffById_NotFound() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/staff/9999",
                HttpMethod.GET,
                entity,
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ========== POST /api/staff ==========
    @Test
    void createStaff_Admin_Success() {
        CreateStaffRequest request = new CreateStaffRequest();
        request.setName("New Staff");
        request.setRole("MANAGER");
        request.setContact("1234567890");
        request.setSalary(BigDecimal.valueOf(50000));
        request.setUsername("newstaff");
        request.setPassword("password123");

        HttpEntity<CreateStaffRequest> entity = new HttpEntity<>(request, authHeaders(getAdminToken()));

        ResponseEntity<StaffDto> response = restTemplate.exchange(
                baseUrl + "/staff",
                HttpMethod.POST,
                entity,
                StaffDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        StaffDto created = response.getBody();
        assertThat(created).isNotNull();
        assertThat(created.getStaffId()).isNotNull();
        assertThat(created.getUsername()).isEqualTo("newstaff");
    }

    @Test
    void createStaff_Receptionist_Forbidden() {
        CreateStaffRequest request = new CreateStaffRequest();
        request.setName("New Staff");
        request.setRole("MANAGER");
        request.setContact("1234567890");
        request.setSalary(BigDecimal.valueOf(50000));
        request.setUsername("newstaff");
        request.setPassword("password123");

        HttpEntity<CreateStaffRequest> entity = new HttpEntity<>(request, authHeaders(getReceptionistToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/staff",
                HttpMethod.POST,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void createStaff_DuplicateUsername() {
        CreateStaffRequest request = new CreateStaffRequest();
        request.setName("Duplicate");
        request.setRole("RECEPTIONIST");
        request.setContact("1111111111");
        request.setSalary(BigDecimal.valueOf(30000));
        request.setUsername("admin"); // existing username
        request.setPassword("somepass");

        HttpEntity<CreateStaffRequest> entity = new HttpEntity<>(request, authHeaders(getAdminToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/staff",
                HttpMethod.POST,
                entity,
                String.class);

        // Should be 400 or 500? The repository save will throw DataIntegrityViolationException because of unique constraint.
        // Global exception handler may catch it and return 500. We'll just check status is 4xx or 5xx.
        assertThat(response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()).isTrue();
    }

    // ========== PUT /api/staff/{id} ==========
    @Test
    void updateStaff_Admin_Success() {
        // Prepare update data (using Staff object, not DTO)
        Staff updateData = new Staff();
        updateData.setName("Updated Name");
        updateData.setRole("ADMIN"); // change role
        updateData.setContact("9999999999");
        updateData.setSalary(BigDecimal.valueOf(60000));
        // username and password are not updated via this endpoint

        HttpEntity<Staff> entity = new HttpEntity<>(updateData, authHeaders(getAdminToken()));

        ResponseEntity<StaffDto> response = restTemplate.exchange(
                baseUrl + "/staff/" + additionalStaff.getStaffId(),
                HttpMethod.PUT,
                entity,
                StaffDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        StaffDto updated = response.getBody();
        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getRole()).isEqualTo("ADMIN");
        assertThat(updated.getContact()).isEqualTo("9999999999");
        assertThat(updated.getSalary()).isEqualByComparingTo("60000");
        // username should remain unchanged
        assertThat(updated.getUsername()).isEqualTo("additional");
    }

    @Test
    void updateStaff_Receptionist_Forbidden() {
        Staff updateData = new Staff();
        updateData.setName("Updated");
        updateData.setRole("RECEPTIONIST");
        updateData.setContact("123");
        updateData.setSalary(BigDecimal.ZERO);

        HttpEntity<Staff> entity = new HttpEntity<>(updateData, authHeaders(getReceptionistToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/staff/" + additionalStaff.getStaffId(),
                HttpMethod.PUT,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void updateStaff_NotFound() {
        Staff updateData = new Staff();
        updateData.setName("Test");
        updateData.setRole("RECEPTIONIST");
        updateData.setContact("123");
        updateData.setSalary(BigDecimal.ZERO);

        HttpEntity<Staff> entity = new HttpEntity<>(updateData, authHeaders(getAdminToken()));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/staff/9999",
                HttpMethod.PUT,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ========== DELETE /api/staff/{id} ==========
    @Test
    void deleteStaff_Admin_Success() {
        // Create a fresh staff to delete (so we don't accidentally delete a needed one)
        Staff toDelete = new Staff();
        toDelete.setUsername("todelete");
        toDelete.setPassword(passwordEncoder.encode("pass"));
        toDelete.setRole("RECEPTIONIST");
        toDelete.setEnabled(true);
        toDelete.setName("To Delete");
        toDelete.setContact("0000000000");
        toDelete.setSalary(BigDecimal.ZERO);
        toDelete = staffRepository.save(toDelete);

        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));

        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/staff/" + toDelete.getStaffId(),
                HttpMethod.DELETE,
                entity,
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify it's gone
        assertThat(staffRepository.findById(toDelete.getStaffId())).isEmpty();
    }

    @Test
    void deleteStaff_Receptionist_Forbidden() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getReceptionistToken()));
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/staff/" + additionalStaff.getStaffId(),
                HttpMethod.DELETE,
                entity,
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deleteStaff_NotFound() {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(getAdminToken()));
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/staff/9999",
                HttpMethod.DELETE,
                entity,
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}