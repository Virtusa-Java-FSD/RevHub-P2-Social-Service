package com.revhub.repository;
import com.revhub.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    Long countByUserIdAndReadFalse(Long userId);
    void deleteByCreatedAtBefore(LocalDateTime date);
    void deleteByUserId(Long userId);
    void deleteByUserIdAndActorIdAndTypeAndRelatedEntityId(
            Long userId,
            Long actorId,
            Notification.NotificationType type,
            String relatedEntityId);
}
