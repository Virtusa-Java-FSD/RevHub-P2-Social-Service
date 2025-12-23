package com.revhub.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private String id;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String content;
    private String storyId; // Optional: if this is a story reply
    private String replyToMessageId; // Optional: for message replies
    private String storyImageUrl; // Optional: for story preview
    private String timestamp;
    private MessageType type;
    public enum MessageType {
        CHAT,
        TYPING,
        JOIN,
        LEAVE
    }
}
