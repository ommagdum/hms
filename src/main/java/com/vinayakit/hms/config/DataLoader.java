package com.vinayakit.hms.config;

import com.vinayakit.hms.entity.Staff;
import com.vinayakit.hms.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (staffRepository.count() == 0) {
            log.info("No staff found. Creating default admin user...");

            Staff admin = new Staff();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123")); // change in production!
            admin.setRole("ADMIN");
            admin.setEnabled(true);
            admin.setName("Default Admin");
            admin.setContact("0000000000");
            admin.setSalary(BigDecimal.ZERO);

            staffRepository.save(admin);
            log.info("Default admin user created (username: admin, password: admin123). Please change password after first login.");
        } else {
            log.info("Staff already exists, skipping default admin creation.");
        }
    }
}
