package com.revhub.service;
import com.revhub.model.Notification;
import com.revhub.model.Notification.NotificationType;
import com.revhub.model.User;
import com.revhub.repository.NotificationRepository;
import com.revhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    public Notification createNotification(
            NotificationType type,
            Long userId,
            Long actorId,
            String message,
            String relatedEntityId) {
        // Don't create notification if user is notifying themselves
        if (userId.equals(actorId)) {
            return null;
        }
        // Get actor details
        User actor = userRepository.findById(actorId).orElse(null);
        if (actor == null) {
            log.warn("Actor not found for notification: {}", actorId);
            return null;
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setActorId(actorId);
        notification.setActorName(actor.getFirstName() + " " + actor.getLastName());
        notification.setActorProfilePicture(actor.getProfilePicture());
        notification.setRelatedEntityId(relatedEntityId);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        Notification saved = notificationRepository.save(notification);
        log.info("Saved notification with ID: {} for user: {}", saved.getId(), userId);
        // Send real-time notification via WebSocket
        sendRealTimeNotification(userId, saved);
        log.info("Created notification: {} for user: {}", type, userId);
        return saved;
    }
    public Page<Notification> getUserNotifications(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    public List<Notification> getAllUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }
    @Transactional
    public void markAsRead(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
            log.info("Marked notification as read: {}", notificationId);
        });
    }
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        notifications.forEach(notification -> {
            if (!notification.getRead()) {
                notification.setRead(true);
            }
        });
        notificationRepository.saveAll(notifications);
        log.info("Marked all notifications as read for user: {}", userId);
    }
    public void deleteNotification(String notificationId) {
        notificationRepository.deleteById(notificationId);
        log.info("Deleted notification: {}", notificationId);
    }
    @Transactional
    public void deleteAllNotifications(Long userId) {
        notificationRepository.deleteByUserId(userId);
        log.info("Deleted all notifications for user: {}", userId);
    }
    @Transactional
    public void deleteLikeNotification(Long postAuthorId, Long likerId, String postId) {
        notificationRepository.deleteByUserIdAndActorIdAndTypeAndRelatedEntityId(
                postAuthorId,
                likerId,
                NotificationType.LIKE,
                postId);
        log.info("Deleted like notification for post: {} from user: {} to user: {}",
                postId, likerId, postAuthorId);
    }
    @Transactional
    public void cleanupOldNotifications(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        notificationRepository.deleteByCreatedAtBefore(cutoffDate);
        log.info("Cleaned up notifications older than {} days", daysOld);
    }
    private void sendRealTimeNotification(Long userId, Notification notification) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/notifications/" + userId,
                    notification);
            log.debug("Sent real-time notification to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send real-time notification", e);
        }
    }
}
