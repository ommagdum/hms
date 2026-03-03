package com.vinayakit.hms.controller;

import com.vinayakit.hms.dto.ApiResponse;
import com.vinayakit.hms.dto.RoomDto;
import com.vinayakit.hms.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<ApiResponse<RoomDto>> createRoom(@Valid @RequestBody RoomDto roomDTO) {
        RoomDto created = roomService.createRoom(roomDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Room created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RoomDto>>> getAllRooms(
            @PageableDefault(size = 10, sort = "roomNumber", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<RoomDto> rooms = roomService.getAllRooms(pageable);
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomDto>> getRoomById(@PathVariable Long id) {
        RoomDto room = roomService.getRoomById(id);
        return ResponseEntity.ok(ApiResponse.success(room));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomDto>> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomDto roomDTO) {
        RoomDto updated = roomService.updateRoom(id, roomDTO);
        return ResponseEntity.ok(ApiResponse.success(updated, "Room updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Room deleted successfully"));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<RoomDto>>> getAvailableRooms(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        List<RoomDto> availableRooms = roomService.findAvailableRooms(checkIn, checkOut);
        return ResponseEntity.ok(ApiResponse.success(availableRooms));
    }
}
