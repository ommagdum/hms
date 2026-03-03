package com.vinayakit.hms.service;

import com.vinayakit.hms.dto.CustomerDto;
import com.vinayakit.hms.entity.Customer;
import com.vinayakit.hms.exception.ResourceNotFoundException;
import com.vinayakit.hms.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer;
    private CustomerDto customerDto;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setCustomerId(1L);
        customer.setName("John Doe");
        customer.setEmail("john@example.com");
        customer.setPhone("1234567890");
        customer.setAddress("123 Main St");

        customerDto = new CustomerDto();
        customerDto.setCustomerId(1L);
        customerDto.setName("John Doe");
        customerDto.setEmail("john@example.com");
        customerDto.setPhone("1234567890");
        customerDto.setAddress("123 Main St");
    }

    // ========== createCustomer ==========
    @Test
    void createCustomer_Success() {
        when(customerRepository.existsByEmail(customerDto.getEmail())).thenReturn(false);
        when(customerRepository.existsByPhone(customerDto.getPhone())).thenReturn(false);
        when(modelMapper.map(customerDto, Customer.class)).thenReturn(customer);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        when(modelMapper.map(customer, CustomerDto.class)).thenReturn(customerDto);

        CustomerDto result = customerService.createCustomer(customerDto);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void createCustomer_DuplicateEmail() {
        when(customerRepository.existsByEmail(customerDto.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(customerDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already registered");
    }

    @Test
    void createCustomer_DuplicatePhone() {
        when(customerRepository.existsByEmail(customerDto.getEmail())).thenReturn(false);
        when(customerRepository.existsByPhone(customerDto.getPhone())).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(customerDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Phone number already registered");
    }

    // ========== updateCustomer ==========
    @Test
    void updateCustomer_Success_NoChangeInEmailOrPhone() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        when(modelMapper.map(customer, CustomerDto.class)).thenReturn(customerDto);

        CustomerDto result = customerService.updateCustomer(1L, customerDto);

        assertThat(result).isNotNull();
        verify(customerRepository, never()).existsByEmail(anyString());
        verify(customerRepository, never()).existsByPhone(anyString());
    }

    @Test
    void updateCustomer_Success_EmailChangedButUnique() {
        CustomerDto updatedDto = new CustomerDto();
        updatedDto.setName("John Doe");
        updatedDto.setEmail("newemail@example.com");
        updatedDto.setPhone("1234567890");
        updatedDto.setAddress("123 Main St");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmail("newemail@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        when(modelMapper.map(customer, CustomerDto.class)).thenReturn(updatedDto);

        CustomerDto result = customerService.updateCustomer(1L, updatedDto);

        assertThat(result).isNotNull();
        verify(customerRepository).existsByEmail("newemail@example.com");
    }

    @Test
    void updateCustomer_Success_PhoneChangedButUnique() {
        CustomerDto updatedDto = new CustomerDto();
        updatedDto.setName("John Doe");
        updatedDto.setEmail("john@example.com");
        updatedDto.setPhone("9999999999");
        updatedDto.setAddress("123 Main St");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByPhone("9999999999")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        when(modelMapper.map(customer, CustomerDto.class)).thenReturn(updatedDto);

        CustomerDto result = customerService.updateCustomer(1L, updatedDto);

        assertThat(result).isNotNull();
        verify(customerRepository).existsByPhone("9999999999");
    }

    @Test
    void updateCustomer_CustomerNotFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.updateCustomer(1L, customerDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer not found with customerId : '1'");
    }

    @Test
    void updateCustomer_EmailAlreadyTaken() {
        CustomerDto updatedDto = new CustomerDto();
        updatedDto.setEmail("taken@example.com");
        updatedDto.setPhone("1234567890"); // same phone

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.updateCustomer(1L, updatedDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already registered");
    }

    @Test
    void updateCustomer_PhoneAlreadyTaken() {
        CustomerDto updatedDto = new CustomerDto();
        updatedDto.setEmail("john@example.com"); // same email
        updatedDto.setPhone("takenphone");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByPhone("takenphone")).thenReturn(true);

        assertThatThrownBy(() -> customerService.updateCustomer(1L, updatedDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Phone number already registered");
    }

    // ========== deleteCustomer ==========
    @Test
    void deleteCustomer_Success() {
        when(customerRepository.existsById(1L)).thenReturn(true);
        doNothing().when(customerRepository).deleteById(1L);

        customerService.deleteCustomer(1L);

        verify(customerRepository).deleteById(1L);
    }

    @Test
    void deleteCustomer_NotFound() {
        when(customerRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> customerService.deleteCustomer(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer not found with customerId : '1'");
    }

    // ========== getCustomerById ==========
    @Test
    void getCustomerById_Success() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(modelMapper.map(customer, CustomerDto.class)).thenReturn(customerDto);

        CustomerDto result = customerService.getCustomerById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getCustomerId()).isEqualTo(1L);
    }

    @Test
    void getCustomerById_NotFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer not found with customerId : '1'");
    }

    // ========== getAllCustomers ==========
    @Test
    void getAllCustomers_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Customer> page = new PageImpl<>(List.of(customer));
        when(customerRepository.findAll(pageable)).thenReturn(page);
        when(modelMapper.map(customer, CustomerDto.class)).thenReturn(customerDto);

        Page<CustomerDto> result = customerService.getAllCustomers(pageable);

        assertThat(result).hasSize(1);
    }

    // ========== searchCustomerByName ==========
    @Test
    void searchCustomerByName_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Customer> page = new PageImpl<>(List.of(customer));
        when(customerRepository.findByNameContainingIgnoreCase("John", pageable)).thenReturn(page);
        when(modelMapper.map(customer, CustomerDto.class)).thenReturn(customerDto);

        Page<CustomerDto> result = customerService.searchCustomerByName("John", pageable);

        assertThat(result).hasSize(1);
    }

    // ========== searchCustomerByPhone ==========
    @Test
    void searchCustomerByPhone_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Customer> page = new PageImpl<>(List.of(customer));
        when(customerRepository.findByPhoneContaining("123", pageable)).thenReturn(page);
        when(modelMapper.map(customer, CustomerDto.class)).thenReturn(customerDto);

        Page<CustomerDto> result = customerService.searchCustomerByPhone("123", pageable);

        assertThat(result).hasSize(1);
    }
}