package com.revhub.controller;

import com.revhub.dto.MessageRequest;
import com.revhub.dto.MessageResponse;
import com.revhub.dto.ReactionRequest;
import com.revhub.model.Message;
import com.revhub.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @PostMapping
    public ResponseEntity<Message> sendMessage(Authentication authentication,
            @Valid @RequestBody MessageRequest messageRequest) {
        Message message = messageService.sendMessage(authentication, messageRequest);
        // Broadcast to receiver for real-time update
        messagingTemplate.convertAndSend("/topic/messages/" + message.getReceiverId(), message);
        // Broadcast to sender (for multi-device sync)
        messagingTemplate.convertAndSend("/topic/messages/" + message.getSenderId(), message);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/conversations")
    public ResponseEntity<Map<String, Object>> getAllConversations(Authentication authentication) {
        Map<String, Object> conversations = messageService.getAllConversations(authentication);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/conversation/{userId}")
    public ResponseEntity<List<Message>> getConversation(Authentication authentication, @PathVariable Long userId) {
        List<Message> messages = messageService.getConversation(authentication, userId);
        return ResponseEntity.ok(messages);
    }

    @PutMapping("/{messageId}/read")
    public ResponseEntity<MessageResponse> markAsRead(Authentication authentication, @PathVariable String messageId) {
        Message updatedMessage = messageService.markAsRead(authentication, messageId);
        // Broadcast to sender (so they see the blue ticks)
        messagingTemplate.convertAndSend("/topic/messages/" + updatedMessage.getSenderId(), updatedMessage);
        // Broadcast to receiver (so their other devices update)
        messagingTemplate.convertAndSend("/topic/messages/" + updatedMessage.getReceiverId(), updatedMessage);
        return ResponseEntity.ok(new MessageResponse("Message marked as read"));
    }

    @PutMapping("/{messageId}")
    public ResponseEntity<Message> updateMessage(
            Authentication authentication,
            @PathVariable String messageId,
            @RequestBody Map<String, String> updates) {
        String newContent = updates.get("content");
        Message updatedMessage = messageService.updateMessage(authentication, messageId, newContent);
        return ResponseEntity.ok(updatedMessage);
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Message> deleteMessage(
            Authentication authentication,
            @PathVariable String messageId) {
        Message deletedMessage = messageService.deleteMessage(authentication, messageId);
        return ResponseEntity.ok(deletedMessage);
    }

    @PostMapping("/upload-media")
    public ResponseEntity<Message> uploadMedia(
            Authentication authentication,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("receiverId") Long receiverId) {
        try {
            System.out.println(
                    "📤 Uploading media file: " + file.getOriginalFilename() + " (" + file.getSize() + " bytes)");
            Message message = messageService.createMediaMessage(authentication, file, receiverId);
            System.out.println("✅ Media uploaded successfully: " + message.getMediaUrl());
            // Broadcast to both parties
            messagingTemplate.convertAndSend("/topic/messages/" + message.getReceiverId(), message);
            messagingTemplate.convertAndSend("/topic/messages/" + message.getSenderId(), message);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            System.err.println("❌ Failed to upload media: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{messageId}/react")
    public ResponseEntity<Message> addReaction(
            Authentication authentication,
            @PathVariable String messageId,
            @RequestBody ReactionRequest request) {
        Message message = messageService.addReaction(authentication, messageId, request.getEmoji());
        // Broadcast to both users for real-time reaction updates
        messagingTemplate.convertAndSend("/topic/messages/" + message.getSenderId(), message);
        messagingTemplate.convertAndSend("/topic/messages/" + message.getReceiverId(), message);
        return ResponseEntity.ok(message);
    }

    @DeleteMapping("/{messageId}/react")
    public ResponseEntity<Message> removeReaction(
            Authentication authentication,
            @PathVariable String messageId,
            @RequestParam String emoji) {
        Message message = messageService.removeReaction(authentication, messageId, emoji);
        // Broadcast to both users for real-time reaction updates
        messagingTemplate.convertAndSend("/topic/messages/" + message.getSenderId(), message);
        messagingTemplate.convertAndSend("/topic/messages/" + message.getReceiverId(), message);
        return ResponseEntity.ok(message);
    }

    @DeleteMapping("/conversation/{userId}")
    public ResponseEntity<MessageResponse> clearConversation(
            Authentication authentication,
            @PathVariable Long userId) {
        messageService.clearConversation(authentication, userId);
        return ResponseEntity.ok(new MessageResponse("Conversation cleared successfully"));
    }
}
