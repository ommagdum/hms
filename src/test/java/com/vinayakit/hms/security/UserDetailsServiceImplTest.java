package com.vinayakit.hms.security;

import com.vinayakit.hms.entity.Staff;
import com.vinayakit.hms.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private Staff staff;

    @BeforeEach
    void setUp() {
        staff = new Staff();
        staff.setStaffId(1L);
        staff.setUsername("admin");
        staff.setPassword("encodedPassword");
        staff.setRole("ADMIN");
        staff.setEnabled(true);
        staff.setName("Admin User");
        staff.setContact("1234567890");
        staff.setSalary(new java.math.BigDecimal("50000"));
    }

    @Test
    void loadUserByUsername_Success() {
        when(staffRepository.findByUsername("admin")).thenReturn(Optional.of(staff));

        UserDetails userDetails = userDetailsService.loadUserByUsername("admin");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("admin");
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void loadUserByUsername_UserNotFound() {
        when(staffRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found with username: unknown");
    }
}