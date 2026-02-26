package com.vinayakit.hms.controller;

import com.vinayakit.hms.entity.Staff;
import com.vinayakit.hms.exception.ResourceNotFoundException;
import com.vinayakit.hms.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
@CrossOrigin("*")
public class StaffController {

    private final StaffRepository staffRepository;

    public StaffController(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    @GetMapping
    public ResponseEntity<List<Staff>> getAllStaff() {
        List<Staff> staffList = staffRepository.findAll();
        return ResponseEntity.ok(staffList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Staff> getStaffById(@PathVariable Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "staffId", id));
        return ResponseEntity.ok(staff);
    }

    @PostMapping
    public ResponseEntity<Staff> createStaff(@RequestBody Staff staff) {
        Staff savedStaff = staffRepository.save(staff);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedStaff);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Staff> updateStaff(@PathVariable Long id, @RequestBody Staff staffDetails) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "staffId", id));

        staff.setName(staffDetails.getName());
        staff.setRole(staffDetails.getRole());
        staff.setContact(staffDetails.getContact());
        staff.setSalary(staffDetails.getSalary());

        Staff updatedStaff = staffRepository.save(staff);
        return ResponseEntity.ok(updatedStaff);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "staffId", id));
        staffRepository.delete(staff);
        return ResponseEntity.noContent().build();
    }
}
