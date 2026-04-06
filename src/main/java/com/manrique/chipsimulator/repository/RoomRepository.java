package com.manrique.chipsimulator.repository;

import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.model.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    
    Optional<Room> findByCode(String code);
    
    List<Room> findByStatus(RoomStatus status);
    
}
