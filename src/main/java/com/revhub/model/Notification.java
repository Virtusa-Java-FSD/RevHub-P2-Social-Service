package com.revhub.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    private String id;
    private Long userId; // Recipient user ID
    private NotificationType type;
    private String message;
    private Long actorId; // User who triggered the notification
    private String actorName;
    private String actorProfilePicture;
    private String relatedEntityId; // ID of related post/comment/etc
    private Boolean read = false;
    @CreatedDate
    private LocalDateTime createdAt;

    public enum NotificationType {
        FOLLOWER, // Someone followed you
        LIKE, // Someone liked your post
        COMMENT, // Someone commented on your post
        MENTION, // Someone mentioned you
        STORY_LIKE // Someone liked your story
    }
}
