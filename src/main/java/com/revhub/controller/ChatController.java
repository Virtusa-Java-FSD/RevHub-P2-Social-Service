package com.revhub.controller;

import com.revhub.dto.ChatMessage;
import com.revhub.model.Message;
import com.revhub.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatController {
        private final SimpMessagingTemplate messagingTemplate;
        private final MessageService messageService;

        @MessageMapping("/chat.send")
        public void sendMessage(@Payload ChatMessage chatMessage, Authentication authentication) {
                System.out.println("🔵 WebSocket Message Received:");
                System.out.println("   SenderId: " + chatMessage.getSenderId());
                System.out.println("   SenderName: " + chatMessage.getSenderName());
                System.out.println("   ReceiverId: " + chatMessage.getReceiverId());
                System.out.println("   Content: " + chatMessage.getContent());
                System.out.println("   ReplyToMessageId: " + chatMessage.getReplyToMessageId());
                System.out.println("   StoryImageUrl: " + chatMessage.getStoryImageUrl());
                // Save message to database
                Message savedMessage = messageService.sendMessage(
                                chatMessage.getSenderId(),
                                chatMessage.getReceiverId(),
                                chatMessage.getContent(),
                                chatMessage.getStoryId(),
                                chatMessage.getReplyToMessageId(),
                                chatMessage.getStoryImageUrl());
                System.out.println(
                                "✅ Message saved with ID: " + savedMessage.getId() + ", SenderId: "
                                                + savedMessage.getSenderId());
                // Update chat message with saved data
                chatMessage.setId(savedMessage.getId());
                chatMessage.setTimestamp(savedMessage.getCreatedAt().toString());
                System.out.println("📤 Sending to receiver: /topic/messages/" + savedMessage.getReceiverId());
                // Send to receiver's topic
                messagingTemplate.convertAndSend(
                                "/topic/messages/" + savedMessage.getReceiverId(),
                                savedMessage);
                System.out.println("📤 Sending to sender: /topic/messages/" + savedMessage.getSenderId());
                // Send to sender's topic
                messagingTemplate.convertAndSend(
                                "/topic/messages/" + savedMessage.getSenderId(),
                                savedMessage);
                System.out.println("✅ Messages sent via WebSocket to both users");
        }

        @MessageMapping("/chat.typing")
        public void sendTypingIndicator(@Payload com.revhub.dto.TypingIndicator indicator) {
                System.out.println("⌨️ Typing indicator: receiverId=" + indicator.getReceiverId() + ", isTyping="
                                + indicator.isTyping());
                // Send typing indicator to receiver
                messagingTemplate.convertAndSend(
                                "/topic/typing/" + indicator.getReceiverId(),
                                indicator);
        }

        @MessageMapping("/chat.join")
        @SendTo("/topic/public")
        public ChatMessage addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
                // Add username in web socket session
                headerAccessor.getSessionAttributes().put("username", chatMessage.getSenderName());
                return chatMessage;
        }

        @MessageMapping("/chat.edit")
        public void editMessage(@Payload Message message) {
                System.out.println("🔵 WebSocket Edit Received for message ID: " + message.getId());
                // Broadcast to both sender and receiver
                messagingTemplate.convertAndSend(
                                "/topic/messages/" + message.getReceiverId(),
                                message);
                messagingTemplate.convertAndSend(
                                "/topic/messages/" + message.getSenderId(),
                                message);
                System.out.println("✅ Edit broadcast to both users");
        }

        @MessageMapping("/chat.delete")
        public void deleteMessage(@Payload Message message) {
                System.out.println("🔵 WebSocket Delete Received for message ID: " + message.getId());
                // Broadcast to both sender and receiver
                messagingTemplate.convertAndSend(
                                "/topic/messages/" + message.getReceiverId(),
                                message);
                messagingTemplate.convertAndSend(
                                "/topic/messages/" + message.getSenderId(),
                                message);
                System.out.println("✅ Delete broadcast to both users");
        }

        @MessageMapping("/user.connect")
        public void userConnect(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
                Long userId = ((Number) payload.get("userId")).longValue();
                headerAccessor.getSessionAttributes().put("userId", userId);
                System.out.println("👤 User " + userId + " stored in session");
        }
}
