package com.revhub.controller;
import com.revhub.dto.NotificationDTO;
import com.revhub.model.Notification;
import com.revhub.model.User;
import com.revhub.service.NotificationService;
import com.revhub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final UserService userService;
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User currentUser = userService.getCurrentUser(authentication);
        Page<Notification> notificationsPage = notificationService.getUserNotifications(
                currentUser.getId(), page, size);
        List<NotificationDTO> notifications = notificationsPage.getContent().stream()
                .map(NotificationDTO::fromNotification)
                .collect(Collectors.toList());
        Map<String, Object> response = new HashMap<>();
        response.put("notifications", notifications);
        response.put("currentPage", notificationsPage.getNumber());
        response.put("totalPages", notificationsPage.getTotalPages());
        response.put("totalItems", notificationsPage.getTotalElements());
        return ResponseEntity.ok(response);
    }
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        Long count = notificationService.getUnreadCount(currentUser.getId());
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }
    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable String id) {
        notificationService.markAsRead(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Notification marked as read");
        return ResponseEntity.ok(response);
    }
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        notificationService.markAllAsRead(currentUser.getId());
        Map<String, String> response = new HashMap<>();
        response.put("message", "All notifications marked as read");
        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteNotification(@PathVariable String id) {
        notificationService.deleteNotification(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Notification deleted");
        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, String>> deleteAllNotifications(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        notificationService.deleteAllNotifications(currentUser.getId());
        Map<String, String> response = new HashMap<>();
        response.put("message", "All notifications deleted");
        return ResponseEntity.ok(response);
    }
}
