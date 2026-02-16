package com.vinayakit.hms.repository;

import com.vinayakit.hms.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCustomer_CustomerId(Long customerId);
    List<Booking> findByRoom_RoomId(Long roomId);
    List<Booking> findByStatus(String status);
}
