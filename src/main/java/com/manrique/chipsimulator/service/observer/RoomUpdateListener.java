package com.manrique.chipsimulator.service.observer;

import com.manrique.chipsimulator.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomUpdateListener {

    private final WebSocketNotificationService notificationService;

    @EventListener
    public void handleRoomUpdate(RoomUpdateEvent event) {
        notificationService.notifyRoomUpdate(event.room(), event.message());
    }
}
