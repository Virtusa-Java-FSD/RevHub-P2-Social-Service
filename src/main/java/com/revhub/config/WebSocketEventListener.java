package com.revhub.config;
import com.revhub.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
    private final UserPresenceService userPresenceService;
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        if (userId != null) {
            System.out.println("🟢 User connected: " + userId);
            userPresenceService.userConnected(userId);
        }
    }
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        if (userId != null) {
            System.out.println("🔴 User disconnected: " + userId);
            userPresenceService.userDisconnected(userId);
        }
    }
}
