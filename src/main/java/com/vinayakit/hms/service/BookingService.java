package com.vinayakit.hms.service;

import com.vinayakit.hms.dto.BookingDto;
import com.vinayakit.hms.entity.Booking;
import com.vinayakit.hms.entity.Customer;
import com.vinayakit.hms.entity.Room;
import com.vinayakit.hms.exception.InvalidBookingDateException;
import com.vinayakit.hms.exception.ResourceNotFoundException;
import com.vinayakit.hms.exception.RoomUnavailableException;
import com.vinayakit.hms.repository.BookingRepository;
import com.vinayakit.hms.repository.CustomerRepository;
import com.vinayakit.hms.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final RoomRepository roomRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public BookingDto createBooking(BookingDto bookingDto) {

        // Validate dates
        if (bookingDto.getCheckIn().isAfter(bookingDto.getCheckOut())) {
            throw new InvalidBookingDateException("Check-in date must be before check-out date");
        }
        if (bookingDto.getCheckIn().isBefore(LocalDate.now())) {
            throw new InvalidBookingDateException("Check-in date cannot be in the past");
        }

        // Fetch Customer and room
        Customer customer = customerRepository.findById(bookingDto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "customerId", bookingDto.getCustomerId()));

        Room room = roomRepository.findById(bookingDto.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", "roomId", bookingDto.getRoomId()));

        // Check room availability
        List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(
                room.getRoomId(), bookingDto.getCheckIn(), bookingDto.getCheckOut()
        );
        if (!conflictingBookings.isEmpty()) {
            throw new RoomUnavailableException("Room is not available for the selected dates");
        }

        // Calculate total amount
        long days = ChronoUnit.DAYS.between(bookingDto.getCheckIn(), bookingDto.getCheckOut());
        if (days <= 0) {
            throw new InvalidBookingDateException("Check-out must be after check-in");
        }
        BigDecimal totalAmount = room.getPrice().multiply(BigDecimal.valueOf(days));

        // Create and save booking
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setRoom(room);
        booking.setCheckIn(bookingDto.getCheckIn());
        booking.setCheckOut(bookingDto.getCheckOut());
        booking.setTotalAmount(totalAmount);
        booking.setStatus("CONFIRMED");

        Booking savedBooking = bookingRepository.save(booking);
        return convertToDto(savedBooking);
    }

    @Transactional
    public BookingDto cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "bookingId", bookingId));

        if ("CANCELLED".equals(booking.getStatus())) {
            throw new IllegalArgumentException("Booking is already cancelled");
        }

        if (!booking.getCheckIn().isAfter(LocalDate.now())) {
            throw new IllegalStateException("Cannot cancel on or after the check-in date.");
        }

        booking.setStatus("CANCELLED");
        Booking cancelledBooking = bookingRepository.save(booking);
        return convertToDto(cancelledBooking);
    }

    public Page<BookingDto> getBookingsByCustomer(Long customerId, Pageable pageable) {

        // Verify if customer exits
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer", "customerId", customerId);
        }

        Page<Booking> bookingsPage = bookingRepository.findByCustomer_CustomerId(customerId, pageable);
        return bookingsPage.map(this::convertToDto);
    }

    public Page<BookingDto> getAllBookings(Pageable pageable) {
        return bookingRepository.findAll(pageable).map(this::convertToDto);
    }

    public BookingDto getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "bookingId", id));
        return convertToDto(booking);
    }

    private BookingDto convertToDto(Booking booking) {
        BookingDto dto = modelMapper.map(booking, BookingDto.class);
        dto.setCustomerId(booking.getCustomer().getCustomerId());
        dto.setRoomId(booking.getRoom().getRoomId());
        return dto;
    }


}
