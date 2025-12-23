package com.revhub.service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Service
@RequiredArgsConstructor
public class UserPresenceService {
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<Long, Boolean> onlineUsers = new ConcurrentHashMap<>();
    public void userConnected(Long userId) {
        onlineUsers.put(userId, true);
        broadcastPresence(userId, true);
    }
    public void userDisconnected(Long userId) {
        onlineUsers.remove(userId);
        broadcastPresence(userId, false);
    }
    public boolean isUserOnline(Long userId) {
        return onlineUsers.getOrDefault(userId, false);
    }
    private void broadcastPresence(Long userId, boolean online) {
        Map<String, Object> presence = Map.of(
                "userId", userId,
                "online", online);
        // Broadcast to all users
        messagingTemplate.convertAndSend("/topic/presence", presence);
    }
    public Map<Long, Boolean> getAllOnlineUsers() {
        return new ConcurrentHashMap<>(onlineUsers);
    }
}
