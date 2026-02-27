package com.vinayakit.hms.controller;

import com.vinayakit.hms.dto.CreateStaffRequest;
import com.vinayakit.hms.dto.StaffDto;
import com.vinayakit.hms.entity.Staff;
import com.vinayakit.hms.exception.ResourceNotFoundException;
import com.vinayakit.hms.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff")
@CrossOrigin("*")
public class StaffController {

    private final PasswordEncoder passwordEncoder;

    private final StaffRepository staffRepository;

    public StaffController(PasswordEncoder passwordEncoder, StaffRepository staffRepository) {
        this.passwordEncoder = passwordEncoder;
        this.staffRepository = staffRepository;
    }

    @GetMapping
    public ResponseEntity<List<StaffDto>> getAllStaff() {
        List<Staff> staffList = staffRepository.findAll();
        List<StaffDto> staffDtoList = staffList.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(staffDtoList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StaffDto> getStaffById(@PathVariable Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "staffId", id));
        StaffDto dto = convertToDto(staff);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<StaffDto> createStaff(@RequestBody CreateStaffRequest request) {
        Staff staff = new Staff();
        staff.setName(request.getName());
        staff.setRole(request.getRole());
        staff.setContact(request.getContact());
        staff.setSalary(request.getSalary());
        staff.setUsername(request.getUsername());
        staff.setPassword(passwordEncoder.encode(request.getPassword()));
        staff.setEnabled(true);
        Staff saved = staffRepository.save(staff);
        StaffDto dto = convertToDto(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StaffDto> updateStaff(@PathVariable Long id, @RequestBody Staff staffDetails) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "staffId", id));

        staff.setName(staffDetails.getName());
        staff.setRole(staffDetails.getRole());
        staff.setContact(staffDetails.getContact());
        staff.setSalary(staffDetails.getSalary());

        Staff updatedStaff = staffRepository.save(staff);

        StaffDto dto = convertToDto(updatedStaff);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "staffId", id));
        staffRepository.delete(staff);
        return ResponseEntity.noContent().build();
    }

    private StaffDto convertToDto(Staff staff) {
        StaffDto dto = new StaffDto();
        dto.setStaffId(staff.getStaffId());
        dto.setName(staff.getName());
        dto.setRole(staff.getRole());
        dto.setContact(staff.getContact());
        dto.setSalary(staff.getSalary());
        dto.setUsername(staff.getUsername());
        return dto;
    }
}
