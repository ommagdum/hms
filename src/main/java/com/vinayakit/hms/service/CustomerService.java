package com.vinayakit.hms.service;

import com.vinayakit.hms.dto.CustomerDto;
import com.vinayakit.hms.entity.Customer;
import com.vinayakit.hms.exception.ResourceNotFoundException;
import com.vinayakit.hms.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ModelMapper modelMapper;
    private final BookingService bookingService;

    @Transactional
    public CustomerDto createCustomer(CustomerDto customerDto) {

        if (customerRepository.existsByEmail(customerDto.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        if (customerRepository.existsByPhone(customerDto.getPhone())) {
            throw new IllegalArgumentException("Phone number already registered");
        }

        Customer customer = convertToEntity(customerDto);
        Customer savedCustomer = customerRepository.save(customer);
        return convertToDto(savedCustomer);
    }

    @Transactional
    public CustomerDto updateCustomer(Long id, CustomerDto customerDto) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "customerId", id));

        // Check email uniqueness if changed
        if(!customer.getEmail().equalsIgnoreCase(customerDto.getEmail())
            && customerRepository.existsByEmail(customerDto.getEmail())
            ) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Check phone uniqueness if changed
        if(!customer.getPhone().equals(customerDto.getPhone())
                && customerRepository.existsByPhone(customerDto.getPhone())
        ) {
            throw new IllegalArgumentException("Phone number already registered");
        }

        // Update fields
        customer.setName(customerDto.getName());
        customer.setEmail(customerDto.getEmail());
        customer.setPhone(customerDto.getPhone());
        customer.setAddress(customerDto.getAddress());

        Customer updatedCustomer = customerRepository.save(customer);
        return convertToDto(updatedCustomer);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Customer", "customerId", id);
        }
        customerRepository.deleteById(id);
    }

    public CustomerDto getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "customerId", id));
        return convertToDto(customer);
    }

    public Page<CustomerDto> getAllCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable).map(this::convertToDto);
    }

    public Page<CustomerDto> searchCustomerByName(String name, Pageable pageable) {
        return customerRepository.findByNameContainingIgnoreCase(name, pageable)
                .map(this::convertToDto);
    }

    public Page<CustomerDto> searchCustomerByPhone(String phone, Pageable pageable) {
        return customerRepository.findByPhoneContaining(phone, pageable)
                .map(this::convertToDto);
    }

    private CustomerDto convertToDto(Customer customer) {
        return modelMapper.map(customer, CustomerDto.class);
    }

    private Customer convertToEntity(CustomerDto dto) {
        return modelMapper.map(dto, Customer.class);
    }
}
