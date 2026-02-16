package com.vinayakit.hms.service;

import com.vinayakit.hms.dto.RoomDto;
import com.vinayakit.hms.entity.Room;
import com.vinayakit.hms.exception.ResourceNotFoundException;
import com.vinayakit.hms.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public RoomDto createRoom(RoomDto roomDto) {
        if (roomRepository.existsByRoomNumber(roomDto.getRoomNumber())) {
            throw new IllegalArgumentException("Room number already exists");
        }
        Room room = convertToEntity(roomDto);
        Room savedRoom = roomRepository.save(room);
        return convertToDto(savedRoom);
    }

    @Transactional
    public RoomDto updateRoom(Long id, RoomDto roomDto) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "roomId", id));

        if(!room.getRoomNumber().equals(roomDto.getRoomNumber()) &&
            roomRepository.existsByRoomNumber(roomDto.getRoomNumber())) {
            throw new IllegalArgumentException("Room number already exists");
        }

        room.setRoomNumber(roomDto.getRoomNumber());
        room.setRoomType(roomDto.getRoomType());
        room.setPrice(roomDto.getPrice());
        room.setStatus(roomDto.getStatus());

        Room updatedRoom = roomRepository.save(room);
        return convertToDto(updatedRoom);
    }

    @Transactional
    public void deleteRoom(Long id) {
        if(!roomRepository.existsById(id)) {
            throw new ResourceNotFoundException("Room", "roomId", id);
        }
        roomRepository.deleteById(id);
    }

    public RoomDto getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "roomId", id));
        return convertToDto(room);
    }

    public Page<RoomDto> getAllRooms(Pageable pageable) {
        return roomRepository.findAll(pageable).map(this::convertToDto);
    }

    public List<RoomDto> findAvailableRooms(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn.isAfter(checkOut)) {
            throw new IllegalArgumentException("Check-in date must be before check-out date");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Check-in date cannot be in the past");
        }
        List<Room> rooms = roomRepository.findAvailableRooms(checkIn, checkOut);
        return rooms.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private Room convertToEntity(RoomDto roomDto) {
        return modelMapper.map(roomDto, Room.class);
    }

    private RoomDto convertToDto(Room room) {
        return modelMapper.map(room, RoomDto.class);
    }
}
