package com.vinayakit.hms.repository;

import com.vinayakit.hms.entity.Room;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByRoomNumber(String roomNumber);

    List<Room> findByStatus(String status);

    @Query("SELECT r FROM Room r WHERE r.status = 'AVAILABLE' " +
            "AND NOT EXISTS (SELECT b FROM Booking b WHERE b.room = r " +
            "AND b.status != 'CANCELLED' " +
            "AND b.checkIn < :checkOut AND b.checkOut > :checkIn)")
    List<Room> findAvailableRooms(@Param("checkIn") LocalDate checkIn, @Param("checkOut") LocalDate checkOut);

    Page<Room> findByStatus(String status, Pageable pageable);

    boolean existsByRoomNumber(@NotBlank(message = "Room number is required") String roomNumber);
}
