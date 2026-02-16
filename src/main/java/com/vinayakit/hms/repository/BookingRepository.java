package com.vinayakit.hms.repository;

import com.vinayakit.hms.entity.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCustomer_CustomerId(Long customerId);
    List<Booking> findByRoom_RoomId(Long roomId);
    List<Booking> findByStatus(String status);

    // Custom Query to check overlapping bookings for a room in a data range
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.room.roomId = :roomId " +
            "AND b.status != 'CANCELLED' " +
            "AND b.checkIn < :checkOut " +
            "AND b.checkOut > :checkIn")
    List<Booking> findConflictingBookings(
            @Param("roomId") Long roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    Page<Booking> findByCustomer_CustomerId(Long customerId, Pageable pageable);

    @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM bookings " +
            "WHERE status = 'CONFIRMED' AND check_in >= :startDate AND check_out <= :endDate",
            nativeQuery = true)
    BigDecimal getTotalRevenueBetween(@Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);
}
