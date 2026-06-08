package com.manrique.chipsimulator.config;

import com.manrique.chipsimulator.service.GameOrchestratorService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class WebSocketSessionListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketSessionListener.class);

    private final GameOrchestratorService orchestratorService;

    // Mapeo thread-safe de sessionId -> SessionInfo
    private final Map<String, SessionInfo> sessionTracker = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String username = headerAccessor.getFirstNativeHeader("username");
        String roomCode = headerAccessor.getFirstNativeHeader("roomCode");

        if (username != null && roomCode != null) {
            sessionTracker.put(sessionId, new SessionInfo(username, roomCode));
            logger.info("Cliente {} conectado a la sala {} con sessionId {}", username, roomCode, sessionId);
            orchestratorService.setUserConnectionStatus(roomCode, username, true);
        }
    }

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        String username = headerAccessor.getFirstNativeHeader("username");

        // Si se suscribe a /topic/room/{roomCode} y envía la cabecera 'username'
        if (destination != null && destination.startsWith("/topic/room/") && username != null) {
            String roomCode = destination.substring("/topic/room/".length());
            sessionTracker.put(sessionId, new SessionInfo(username, roomCode));
            logger.info("Cliente {} suscrito a la sala {} con sessionId {}", username, roomCode, sessionId);
            orchestratorService.setUserConnectionStatus(roomCode, username, true);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        SessionInfo sessionInfo = sessionTracker.remove(sessionId);

        if (sessionInfo != null) {
            String username = sessionInfo.username();
            String roomCode = sessionInfo.roomCode();
            logger.info("Cliente {} desconectado de la sala {} (sessionId {})", username, roomCode, sessionId);
            orchestratorService.setUserConnectionStatus(roomCode, username, false);
        }
    }

    public record SessionInfo(String username, String roomCode) {}
}
