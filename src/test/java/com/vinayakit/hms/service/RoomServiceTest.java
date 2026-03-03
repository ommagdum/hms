package com.vinayakit.hms.service;

import com.vinayakit.hms.dto.RoomDto;
import com.vinayakit.hms.entity.Room;
import com.vinayakit.hms.exception.ResourceNotFoundException;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private RoomService roomService;

    private Room room;
    private RoomDto roomDto;

    @BeforeEach
    void setUp() {
        room = new Room();
        room.setRoomId(1L);
        room.setRoomNumber("101");
        room.setRoomType("Standard");
        room.setPrice(BigDecimal.valueOf(1000));
        room.setStatus("AVAILABLE");

        roomDto = new RoomDto();
        roomDto.setRoomId(1L);
        roomDto.setRoomNumber("101");
        roomDto.setRoomType("Standard");
        roomDto.setPrice(BigDecimal.valueOf(1000));
        roomDto.setStatus("AVAILABLE");
    }

    // ========== createRoom ==========
    @Test
    void createRoom_Success() {
        when(roomRepository.existsByRoomNumber(roomDto.getRoomNumber())).thenReturn(false);
        when(modelMapper.map(roomDto, Room.class)).thenReturn(room);
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(modelMapper.map(room, RoomDto.class)).thenReturn(roomDto);

        RoomDto result = roomService.createRoom(roomDto);

        assertThat(result).isNotNull();
        assertThat(result.getRoomNumber()).isEqualTo("101");
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    void createRoom_DuplicateRoomNumber() {
        when(roomRepository.existsByRoomNumber(roomDto.getRoomNumber())).thenReturn(true);

        assertThatThrownBy(() -> roomService.createRoom(roomDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Room number already exists");
    }

    // ========== updateRoom ==========
    @Test
    void updateRoom_Success_NoRoomNumberChange() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(modelMapper.map(room, RoomDto.class)).thenReturn(roomDto);

        RoomDto result = roomService.updateRoom(1L, roomDto);

        assertThat(result).isNotNull();
        verify(roomRepository, never()).existsByRoomNumber(anyString());
    }

    @Test
    void updateRoom_Success_RoomNumberChangedAndUnique() {
        RoomDto updatedDto = new RoomDto();
        updatedDto.setRoomNumber("102");
        updatedDto.setRoomType("Deluxe");
        updatedDto.setPrice(BigDecimal.valueOf(2000));
        updatedDto.setStatus("AVAILABLE");

        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomRepository.existsByRoomNumber("102")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(modelMapper.map(room, RoomDto.class)).thenReturn(updatedDto);

        RoomDto result = roomService.updateRoom(1L, updatedDto);

        assertThat(result).isNotNull();
        verify(roomRepository).existsByRoomNumber("102");
    }

    @Test
    void updateRoom_RoomNotFound() {
        when(roomRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.updateRoom(1L, roomDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Room not found with roomId : '1'");
    }

    @Test
    void updateRoom_DuplicateRoomNumber() {
        RoomDto updatedDto = new RoomDto();
        updatedDto.setRoomNumber("102");
        updatedDto.setRoomType("Deluxe");
        updatedDto.setPrice(BigDecimal.valueOf(2000));
        updatedDto.setStatus("AVAILABLE");

        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomRepository.existsByRoomNumber("102")).thenReturn(true);

        assertThatThrownBy(() -> roomService.updateRoom(1L, updatedDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Room number already exists");
    }

    // ========== deleteRoom ==========
    @Test
    void deleteRoom_Success() {
        when(roomRepository.existsById(1L)).thenReturn(true);
        doNothing().when(roomRepository).deleteById(1L);

        roomService.deleteRoom(1L);

        verify(roomRepository).deleteById(1L);
    }

    @Test
    void deleteRoom_NotFound() {
        when(roomRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> roomService.deleteRoom(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Room not found with roomId : '1'");
    }

    // ========== getRoomById ==========
    @Test
    void getRoomById_Success() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(modelMapper.map(room, RoomDto.class)).thenReturn(roomDto);

        RoomDto result = roomService.getRoomById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getRoomId()).isEqualTo(1L);
    }

    @Test
    void getRoomById_NotFound() {
        when(roomRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getRoomById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Room not found with roomId : '1'");
    }

    // ========== getAllRooms ==========
    @Test
    void getAllRooms_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Room> page = new PageImpl<>(List.of(room));
        when(roomRepository.findAll(pageable)).thenReturn(page);
        when(modelMapper.map(room, RoomDto.class)).thenReturn(roomDto);

        Page<RoomDto> result = roomService.getAllRooms(pageable);

        assertThat(result).hasSize(1);
    }

    // ========== findAvailableRooms ==========
    @Test
    void findAvailableRooms_Success() {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);
        List<Room> rooms = List.of(room);
        when(roomRepository.findAvailableRooms(checkIn, checkOut)).thenReturn(rooms);
        when(modelMapper.map(room, RoomDto.class)).thenReturn(roomDto);

        List<RoomDto> result = roomService.findAvailableRooms(checkIn, checkOut);

        assertThat(result).hasSize(1);
    }

    @Test
    void findAvailableRooms_InvalidDate_CheckInAfterCheckOut() {
        LocalDate checkIn = LocalDate.now().plusDays(3);
        LocalDate checkOut = LocalDate.now().plusDays(1);

        assertThatThrownBy(() -> roomService.findAvailableRooms(checkIn, checkOut))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Check-in date must be before check-out date");
    }

    @Test
    void findAvailableRooms_InvalidDate_CheckInInPast() {
        LocalDate checkIn = LocalDate.now().minusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(1);

        assertThatThrownBy(() -> roomService.findAvailableRooms(checkIn, checkOut))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Check-in date cannot be in the past");
    }

    @Test
    void findAvailableRooms_NoRoomsAvailable() {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);
        when(roomRepository.findAvailableRooms(checkIn, checkOut)).thenReturn(List.of());

        List<RoomDto> result = roomService.findAvailableRooms(checkIn, checkOut);

        assertThat(result).isEmpty();
    }
}