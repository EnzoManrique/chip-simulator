package com.manrique.chipsimulator.scheduler;

import com.manrique.chipsimulator.model.Room;
import com.manrique.chipsimulator.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RoomCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RoomCleanupScheduler.class);
    private final RoomRepository roomRepository;

    // Ejecutar cada 1 hora
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupInactiveRooms() {
        logger.info("Iniciando tarea programada de limpieza de salas inactivas...");
        
        // Salas que no han tenido actividad por más de 1 hora
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        
        List<Room> inactiveRooms = roomRepository.findByUpdatedAtBefore(cutoff);
        
        if (!inactiveRooms.isEmpty()) {
            logger.info("Se encontraron {} salas inactivas para eliminar.", inactiveRooms.size());
            roomRepository.deleteAll(inactiveRooms);
            logger.info("Limpieza completada con éxito.");
        } else {
            logger.info("No se encontraron salas inactivas para limpiar.");
        }
    }
}
