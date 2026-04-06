package com.manrique.chipsimulator.repository;

import com.manrique.chipsimulator.model.RoomPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomPlayerRepository extends JpaRepository<RoomPlayer, Long> {
    
    List<RoomPlayer> findByRoomIdOrderBySeatNumberAsc(Long roomId);
    
}
