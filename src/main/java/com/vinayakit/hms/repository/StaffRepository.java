package com.vinayakit.hms.repository;

import com.vinayakit.hms.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {

    List<Staff> findByRole(String role);

    Optional<Staff> findByUsername(String username);
}
