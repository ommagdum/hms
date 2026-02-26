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
import java.time.LocalDateTime;
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

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status ='CONFIRMED' AND b.checkOut >= CURRENT_DATE")
    Long countActiveBookings();

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b WHERE b.status='CONFIRMED'")
    BigDecimal sumTotalRevenue();

    @Query("SELECT FUNCTION('DATE', b.createdAt) as date, SUM(b.totalAmount) as dailyTotal " +
            "FROM Booking b WHERE b.status = 'CONFIRMED' AND b.createdAt >= :startDate " +
            "GROUP BY FUNCTION('DATE', b.createdAt) ORDER BY date")
    List<Object[]> findDailyRevenueSince(@Param("startDate")LocalDateTime startDate);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b " +
            "WHERE b.status = 'CONFIRMED' AND b.createdAt BETWEEN :start AND :end")
    BigDecimal sumRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT b FROM Booking b WHERE b.checkIn = CURRENT_DATE AND b.status = 'CONFIRMED'")
    List<Booking> findTodayCheckIns();

    @Query("SELECT b FROM Booking b WHERE b.checkOut = CURRENT_DATE AND b.status = 'CONFIRMED'")
    List<Booking> findTodayCheckOuts();

    Long countByCreatedAtBetweenAndStatus(LocalDateTime start, LocalDateTime end, String status);

    @Query("SELECT b FROM Booking b WHERE b.status != 'CANCELLED' " +
            "AND b.checkIn < :endDate AND b.checkOut > :startDate")
    List<Booking> findBookingsInRange(@Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    @Query("SELECT b FROM Booking b WHERE " +
            "(:guestName IS NULL OR LOWER(b.customer.name) LIKE LOWER(CONCAT('%', :guestName, '%'))) AND " +
            "(:roomNumber IS NULL OR b.room.roomNumber = :roomNumber) AND " +
            "(:status IS NULL OR b.status = :status)")
    Page<Booking> findFilteredBookings(@Param("guestName") String guestName,
                                       @Param("roomNumber") String roomNumber,
                                       @Param("status") String status,
                                       Pageable pageable);

}
