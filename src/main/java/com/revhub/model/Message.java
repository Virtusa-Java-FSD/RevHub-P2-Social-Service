package com.revhub.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
@Document(collection = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    @Id
    private String id;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String receiverName;
    private String content;
    private String storyId; // Optional: for story replies
    private String replyToMessageId; // Optional: for replying to specific messages
    private String storyImageUrl; // Optional: story thumbnail URL for story replies
    // Media support for images and videos
    private String mediaUrl; // S3 URL for uploaded media
    private String mediaType; // "IMAGE", "VIDEO", or null
    private Boolean isRead = false;
    // Message reactions: emoji -> list of user IDs
    private java.util.Map<String, java.util.List<Long>> reactions = new java.util.HashMap<>();
    // Edit and Delete tracking
    private Boolean isEdited = false;
    private LocalDateTime editedAt;
    private Boolean isDeleted = false;
    private LocalDateTime deletedAt;
    @CreatedDate
    private LocalDateTime createdAt;
}
