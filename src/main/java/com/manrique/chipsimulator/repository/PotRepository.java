package com.manrique.chipsimulator.repository;

import com.manrique.chipsimulator.model.Pot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PotRepository extends JpaRepository<Pot, Long> {
    
    List<Pot> findByRoomId(Long roomId);
    
}
