package com.revhub.dto;
import com.revhub.model.Notification;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private String id;
    private Long userId;
    private String type;
    private String message;
    private Long actorId;
    private String actorName;
    private String actorProfilePicture;
    private String relatedEntityId;
    private Boolean read;
    private LocalDateTime createdAt;
    public static NotificationDTO fromNotification(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUserId());
        dto.setType(notification.getType().name());
        dto.setMessage(notification.getMessage());
        dto.setActorId(notification.getActorId());
        dto.setActorName(notification.getActorName());
        dto.setActorProfilePicture(notification.getActorProfilePicture());
        dto.setRelatedEntityId(notification.getRelatedEntityId());
        dto.setRead(notification.getRead());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }
}
