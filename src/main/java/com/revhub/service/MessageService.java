package com.revhub.service;

import com.revhub.dto.MessageRequest;
import com.revhub.model.Message;
import com.revhub.model.User;
import com.revhub.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final UserService userService;
    private final BucketService bucketService;
    private final SimpMessagingTemplate messagingTemplate;
    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public Message sendMessage(Authentication authentication, MessageRequest messageRequest) {
        User sender = userService.getCurrentUser(authentication);
        User receiver = userService.getUserById(messageRequest.getReceiverId());
        Message message = new Message();
        message.setSenderId(sender.getId());
        message.setSenderName(sender.getFirstName() + " " + sender.getLastName());
        message.setReceiverId(receiver.getId());
        message.setReceiverName(receiver.getFirstName() + " " + receiver.getLastName());
        message.setContent(messageRequest.getContent());
        message.setIsRead(false);
        message.setCreatedAt(LocalDateTime.now());
        return messageRepository.save(message);
    }

    public List<Message> getConversation(Authentication authentication, Long otherUserId) {
        User currentUser = userService.getCurrentUser(authentication);
        userService.getUserById(otherUserId); // Verify other user exists
        return messageRepository.findConversation(currentUser.getId(), otherUserId,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC,
                        "createdAt"));
    }

    public Map<String, Object> getAllConversations(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        List<Message> allMessages = messageRepository.findAllUserMessages(currentUser.getId());
        // Group messages by conversation partner
        Map<Long, List<Message>> conversationMap = new HashMap<>();
        for (Message message : allMessages) {
            Long partnerId = message.getSenderId().equals(currentUser.getId())
                    ? message.getReceiverId()
                    : message.getSenderId();
            conversationMap.computeIfAbsent(partnerId, k -> new ArrayList<>()).add(message);
        }
        // Get last message for each conversation
        List<Map<String, Object>> conversations = conversationMap.entrySet().stream()
                .map(entry -> {
                    List<Message> messages = entry.getValue();
                    Message lastMessage = messages.stream()
                            .max(Comparator.comparing(Message::getCreatedAt))
                            .orElse(null);
                    Map<String, Object> conversation = new HashMap<>();
                    conversation.put("partnerId", entry.getKey());
                    // Get partner's full name and profile picture with error handling
                    try {
                        User partner = userService.getUserById(entry.getKey());
                        String fullName = partner.getFirstName() + " " + partner.getLastName();
                        conversation.put("partnerName", fullName);
                        conversation.put("partnerProfilePicture", partner.getProfilePicture());
                        System.out.println("✅ Loaded partner: ID=" + entry.getKey() + ", Name=" + fullName);
                    } catch (Exception e) {
                        System.err.println("❌ Failed to load user ID: " + entry.getKey());
                        conversation.put("partnerName", "User #" + entry.getKey());
                        conversation.put("partnerProfilePicture", null);
                    }
                    conversation.put("lastMessage", lastMessage);
                    conversation.put("messageCount", messages.size());
                    // Calculate unread count for current user
                    long unreadCount = messages.stream()
                            .filter(m -> m.getReceiverId().equals(currentUser.getId())
                                    && !Boolean.TRUE.equals(m.getIsRead()))
                            .count();
                    conversation.put("unreadCount", unreadCount);
                    return conversation;
                })
                .sorted((c1, c2) -> {
                    Message m1 = (Message) c1.get("lastMessage");
                    Message m2 = (Message) c2.get("lastMessage");
                    return m2.getCreatedAt().compareTo(m1.getCreatedAt());
                })
                .collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("conversations", conversations);
        return result;
    }

    @Transactional
    public Message markAsRead(Authentication authentication, String messageId) {
        User currentUser = userService.getCurrentUser(authentication);
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        if (!message.getReceiverId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only mark messages sent to you as read");
        }
        message.setIsRead(true);
        Message updatedMessage = messageRepository.save(message);

        // Broadcast read status to sender (for double tick effect)
        messagingTemplate.convertAndSend("/topic/messages/" + message.getSenderId(), updatedMessage);

        // Broadcast to receiver as well (to update other tabs/devices)
        messagingTemplate.convertAndSend("/topic/messages/" + message.getReceiverId(), updatedMessage);

        return updatedMessage;
    }

    // Overloaded method for WebSocket usage
    public Message sendMessage(Long senderId, Long receiverId, String content, String storyId,
            String replyToMessageId, String storyImageUrl) {
        User sender = userService.getUserById(senderId);
        User receiver = userService.getUserById(receiverId);
        Message message = new Message();
        message.setSenderId(sender.getId());
        message.setSenderName(sender.getFirstName() + " " + sender.getLastName());
        message.setReceiverId(receiver.getId());
        message.setReceiverName(receiver.getFirstName() + " " + receiver.getLastName());
        message.setContent(content);
        message.setStoryId(storyId);
        message.setReplyToMessageId(replyToMessageId); // ✅ Set reply field
        message.setStoryImageUrl(storyImageUrl); // ✅ Set story image URL
        message.setIsRead(false);
        message.setCreatedAt(LocalDateTime.now());
        return messageRepository.save(message);
    }

    public Message updateMessage(Authentication authentication, String messageId, String newContent) {
        User currentUser = userService.getCurrentUser(authentication);
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        // Verify user owns the message
        if (!message.getSenderId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only edit your own messages");
        }
        // Don't allow editing deleted messages
        if (Boolean.TRUE.equals(message.getIsDeleted())) {
            throw new RuntimeException("Cannot edit a deleted message");
        }
        // Check if message is older than 2 hours
        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
        if (message.getCreatedAt().isBefore(twoHoursAgo)) {
            throw new RuntimeException("Cannot edit messages older than 2 hours");
        }
        message.setContent(newContent);
        message.setIsEdited(true);
        message.setEditedAt(LocalDateTime.now());
        return messageRepository.save(message);
    }

    public Message deleteMessage(Authentication authentication, String messageId) {
        User currentUser = userService.getCurrentUser(authentication);
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        // Verify user owns the message
        if (!message.getSenderId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only delete your own messages");
        }
        // Soft delete - mark as deleted
        message.setIsDeleted(true);
        message.setDeletedAt(LocalDateTime.now());
        message.setContent("This message was deleted");
        return messageRepository.save(message);
    }

    public Message createMediaMessage(Authentication authentication,
            org.springframework.web.multipart.MultipartFile file, Long receiverId) throws Exception {
        User sender = userService.getCurrentUser(authentication);
        User receiver = userService.getUserById(receiverId);
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        // Validate file size (10MB max)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }
        // Validate file type
        String contentType = file.getContentType();
        String mediaType;
        if (contentType != null && contentType.startsWith("image/")) {
            mediaType = "IMAGE";
        } else if (contentType != null && contentType.startsWith("video/")) {
            mediaType = "VIDEO";
        } else {
            throw new IllegalArgumentException("Unsupported file type. Only images and videos are allowed.");
        }
        // Upload to S3
        String mediaUrl = bucketService.uploadFile(file, bucketName);
        // Create message
        Message message = new Message();
        message.setSenderId(sender.getId());
        message.setSenderName(sender.getFirstName() + " " + sender.getLastName());
        message.setReceiverId(receiver.getId());
        message.setReceiverName(receiver.getFirstName() + " " + receiver.getLastName());
        message.setMediaUrl(mediaUrl);
        message.setMediaType(mediaType);
        message.setContent(""); // Empty content for media messages
        message.setIsRead(false);
        message.setCreatedAt(LocalDateTime.now());
        return messageRepository.save(message);
    }

    @Transactional
    public Message addReaction(Authentication authentication, String messageId, String emoji) {
        User currentUser = userService.getCurrentUser(authentication);
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        // Initialize reactions map if null
        if (message.getReactions() == null) {
            message.setReactions(new HashMap<>());
        }
        // Get or create the list of users who reacted with this emoji
        List<Long> userIds = message.getReactions().computeIfAbsent(emoji, k -> new ArrayList<>());
        // Add user ID if not already present
        if (!userIds.contains(currentUser.getId())) {
            userIds.add(currentUser.getId());
        }
        return messageRepository.save(message);
    }

    @Transactional
    public Message removeReaction(Authentication authentication, String messageId, String emoji) {
        User currentUser = userService.getCurrentUser(authentication);
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        if (message.getReactions() != null && message.getReactions().containsKey(emoji)) {
            List<Long> userIds = message.getReactions().get(emoji);
            userIds.remove(currentUser.getId());
            // Remove emoji key if no users left
            if (userIds.isEmpty()) {
                message.getReactions().remove(emoji);
            }
        }
        return messageRepository.save(message);
    }

    @Transactional
    public void clearConversation(Authentication authentication, Long otherUserId) {
        User currentUser = userService.getCurrentUser(authentication);
        userService.getUserById(otherUserId); // Verify other user exists

        // Delete all messages between these two users
        messageRepository.deleteConversation(currentUser.getId(), otherUserId);
    }
}
