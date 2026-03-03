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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private BookingService bookingService;

    private Customer customer;
    private Room room;
    private Booking booking;
    private BookingDto bookingDto;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setCustomerId(1L);
        customer.setName("John Doe");
        customer.setEmail("john@example.com");
        customer.setPhone("1234567890");

        room = new Room();
        room.setRoomId(1L);
        room.setRoomNumber("101");
        room.setRoomType("Standard");
        room.setPrice(BigDecimal.valueOf(1000));
        room.setStatus("AVAILABLE");

        booking = new Booking();
        booking.setBookingId(1L);
        booking.setCustomer(customer);
        booking.setRoom(room);
        booking.setCheckIn(LocalDate.now().plusDays(2));
        booking.setCheckOut(LocalDate.now().plusDays(5));
        booking.setTotalAmount(BigDecimal.valueOf(3000));
        booking.setStatus("CONFIRMED");

        bookingDto = new BookingDto();
        bookingDto.setBookingId(1L);
        bookingDto.setCustomerId(1L);
        bookingDto.setRoomId(1L);
        bookingDto.setCheckIn(LocalDate.now().plusDays(2));
        bookingDto.setCheckOut(LocalDate.now().plusDays(5));
        bookingDto.setTotalAmount(BigDecimal.valueOf(3000));
        bookingDto.setStatus("CONFIRMED");
    }

    // ========== createBooking ==========
    @Test
    void createBooking_Success() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.findConflictingBookings(1L, bookingDto.getCheckIn(), bookingDto.getCheckOut()))
                .thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(modelMapper.map(any(Booking.class), eq(BookingDto.class))).thenReturn(bookingDto);

        BookingDto result = bookingService.createBooking(bookingDto);

        assertThat(result).isNotNull();
        assertThat(result.getBookingId()).isEqualTo(1L);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void createBooking_InvalidDate_CheckInAfterCheckOut() {
        bookingDto.setCheckIn(LocalDate.now().plusDays(5));
        bookingDto.setCheckOut(LocalDate.now().plusDays(2));

        assertThatThrownBy(() -> bookingService.createBooking(bookingDto))
                .isInstanceOf(InvalidBookingDateException.class)
                .hasMessage("Check-in date must be before check-out date");
    }

    @Test
    void createBooking_InvalidDate_CheckInEqualsCheckOut() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        bookingDto.setCheckIn(LocalDate.now().plusDays(2));
        bookingDto.setCheckOut(LocalDate.now().plusDays(2)); // same day

        assertThatThrownBy(() -> bookingService.createBooking(bookingDto))
                .isInstanceOf(InvalidBookingDateException.class)
                .hasMessage("Check-out must be after check-in");
    }

    @Test
    void createBooking_InvalidDate_CheckInInPast() {
        bookingDto.setCheckIn(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> bookingService.createBooking(bookingDto))
                .isInstanceOf(InvalidBookingDateException.class)
                .hasMessage("Check-in date cannot be in the past");
    }

    @Test
    void createBooking_CustomerNotFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(bookingDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer not found with customerId : '1'");
    }

    @Test
    void createBooking_RoomNotFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(roomRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(bookingDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Room not found with roomId : '1'");
    }

    @Test
    void createBooking_RoomUnavailable() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.findConflictingBookings(1L, bookingDto.getCheckIn(), bookingDto.getCheckOut()))
                .thenReturn(List.of(new Booking())); // non-empty list

        assertThatThrownBy(() -> bookingService.createBooking(bookingDto))
                .isInstanceOf(RoomUnavailableException.class)
                .hasMessage("Room is not available for the selected dates");
    }

    // ========== cancelBooking ==========
    @Test
    void cancelBooking_Success() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(modelMapper.map(any(Booking.class), eq(BookingDto.class))).thenReturn(bookingDto);

        BookingDto result = bookingService.cancelBooking(1L);

        assertThat(result).isNotNull();
        assertThat(booking.getStatus()).isEqualTo("CANCELLED");
        verify(bookingRepository).save(booking);
    }

    @Test
    void cancelBooking_NotFound() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancelBooking(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Booking not found with bookingId : '1'");
    }

    @Test
    void cancelBooking_AlreadyCancelled() {
        booking.setStatus("CANCELLED");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Booking is already cancelled");
    }

    @Test
    void cancelBooking_CheckInTodayOrPast() {
        booking.setCheckIn(LocalDate.now()); // today
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot cancel on or after the check-in date.");
    }

    // ========== updateBooking ==========
    @Test
    void updateBooking_Success() {
        BookingDto updateDto = new BookingDto();
        updateDto.setCustomerId(1L);
        updateDto.setRoomId(1L);
        updateDto.setCheckIn(LocalDate.now().plusDays(3));
        updateDto.setCheckOut(LocalDate.now().plusDays(6));

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.existsOverlapExcludingSelf(1L, 1L, updateDto.getCheckIn(), updateDto.getCheckOut()))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(modelMapper.map(any(Booking.class), eq(BookingDto.class))).thenReturn(updateDto);

        BookingDto result = bookingService.updateBooking(1L, updateDto);

        assertThat(result).isNotNull();
        verify(bookingRepository).save(booking);
    }

    @Test
    void updateBooking_BookingNotFound() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.updateBooking(1L, bookingDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Booking not found with bookingId : '1'");
    }

    @Test
    void updateBooking_BookingCancelled() {
        booking.setStatus("CANCELLED");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.updateBooking(1L, bookingDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot update a cancelled booking");
    }

    @Test
    void updateBooking_InvalidDates_CheckInAfterCheckOut() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        bookingDto.setCheckIn(LocalDate.now().plusDays(5));
        bookingDto.setCheckOut(LocalDate.now().plusDays(2));

        assertThatThrownBy(() -> bookingService.updateBooking(1L, bookingDto))
                .isInstanceOf(InvalidBookingDateException.class)
                .hasMessage("Check-in date must be before check-out date");
    }

    @Test
    void updateBooking_InvalidDates_CheckInEqualsCheckOut() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        // Need to mock customer/room lookups because they happen before days calculation
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.existsOverlapExcludingSelf(any(), any(), any(), any())).thenReturn(false);

        bookingDto.setCheckIn(LocalDate.now().plusDays(3));
        bookingDto.setCheckOut(LocalDate.now().plusDays(3)); // same day

        assertThatThrownBy(() -> bookingService.updateBooking(1L, bookingDto))
                .isInstanceOf(InvalidBookingDateException.class)
                .hasMessage("Check-out must be after check-in");
    }

    @Test
    void updateBooking_InvalidDates_CheckInInPast() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        bookingDto.setCheckIn(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> bookingService.updateBooking(1L, bookingDto))
                .isInstanceOf(InvalidBookingDateException.class)
                .hasMessage("Check-in date cannot be in the past");
    }

    @Test
    void updateBooking_CustomerNotFound() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.updateBooking(1L, bookingDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer not found with customerId : '1'");
    }

    @Test
    void updateBooking_RoomNotFound() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(roomRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.updateBooking(1L, bookingDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Room not found with roomId : '1'");
    }

    @Test
    void updateBooking_RoomUnavailable() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.existsOverlapExcludingSelf(1L, 1L, bookingDto.getCheckIn(), bookingDto.getCheckOut()))
                .thenReturn(true);

        assertThatThrownBy(() -> bookingService.updateBooking(1L, bookingDto))
                .isInstanceOf(RoomUnavailableException.class)
                .hasMessage("Room is not available for the selected dates");
    }

    // ========== getBookingsByCustomer ==========
    @Test
    void getBookingsByCustomer_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Booking> page = new PageImpl<>(List.of(booking));
        when(customerRepository.existsById(1L)).thenReturn(true);
        when(bookingRepository.findByCustomer_CustomerId(1L, pageable)).thenReturn(page);
        when(modelMapper.map(any(Booking.class), eq(BookingDto.class))).thenReturn(bookingDto);

        Page<BookingDto> result = bookingService.getBookingsByCustomer(1L, pageable);

        assertThat(result).hasSize(1);
    }

    @Test
    void getBookingsByCustomer_CustomerNotFound() {
        when(customerRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> bookingService.getBookingsByCustomer(1L, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer not found with customerId : '1'");
    }

    // ========== getFilteredBookings ==========
    @Test
    void getFilteredBookings_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Booking> page = new PageImpl<>(List.of(booking));
        when(bookingRepository.findFilteredBookings("John", null, null, pageable)).thenReturn(page);
        when(modelMapper.map(any(Booking.class), eq(BookingDto.class))).thenReturn(bookingDto);

        Page<BookingDto> result = bookingService.getFilteredBookings("John", null, null, pageable);

        assertThat(result).hasSize(1);
    }

    // ========== getBookingById ==========
    @Test
    void getBookingById_Success() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(modelMapper.map(booking, BookingDto.class)).thenReturn(bookingDto);

        BookingDto result = bookingService.getBookingById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getBookingId()).isEqualTo(1L);
    }

    @Test
    void getBookingById_NotFound() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBookingById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Booking not found with bookingId : '1'");
    }

    // ========== getBookingsInRange ==========
    @Test
    void getBookingsInRange_Success() {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().plusDays(10);
        when(bookingRepository.findBookingsInRange(start, end)).thenReturn(List.of(booking));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        var result = bookingService.getBookingsInRange(start, end);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRoomId()).isEqualTo(1L);
    }
}