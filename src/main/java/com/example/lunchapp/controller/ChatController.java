package com.example.lunchapp.controller;

import com.example.lunchapp.model.dto.ChatMessageDto;
import com.example.lunchapp.model.dto.NotificationDto;
import com.example.lunchapp.model.dto.UserDto;
import com.example.lunchapp.model.entity.User;
import com.example.lunchapp.service.ChatService;
import com.example.lunchapp.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final UserService userService;

    @Autowired
    public ChatController(SimpMessagingTemplate messagingTemplate, ChatService chatService, UserService userService) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
        this.userService = userService;
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDto chatMessageDto) {
        logger.info("Received message to send: {}", chatMessageDto);
        ChatMessageDto savedMessage = chatService.saveMessage(chatMessageDto);
        logger.info("Message saved, now processing. DTO: {}", savedMessage);

        messagingTemplate.convertAndSendToUser(
                savedMessage.getRecipientUsername(),
                "/queue/messages",
                savedMessage
        );

        messagingTemplate.convertAndSendToUser(
                savedMessage.getSenderUsername(),
                "/queue/messages",
                savedMessage
        );

        NotificationDto notification = new NotificationDto(
                savedMessage.getRecipientId(),
                savedMessage.getSenderId(),
                savedMessage.getSenderUsername(),
                "Bạn có tin nhắn mới từ " + savedMessage.getSenderUsername()
        );

        messagingTemplate.convertAndSendToUser(
                savedMessage.getRecipientUsername(),
                "/queue/notifications",
                notification
        );
        logger.info("Sent notification to {}: {}", savedMessage.getRecipientUsername(), notification);
    }

    @DeleteMapping("/chat/messages/{messageId}")
    @ResponseBody
    public ResponseEntity<Void> retractMessage(@PathVariable Long messageId, HttpSession session) {
        UserDto currentUser = (UserDto) session.getAttribute("loggedInUser");
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            ChatMessageDto retractedMessage = chatService.retractMessage(messageId, currentUser.getId());
            logger.info("Message {} retracted by user {}. Broadcasting update.", messageId, currentUser.getUsername());

            messagingTemplate.convertAndSendToUser(
                    retractedMessage.getRecipientUsername(),
                    "/queue/messages",
                    retractedMessage
            );

            messagingTemplate.convertAndSendToUser(
                    retractedMessage.getSenderUsername(),
                    "/queue/messages",
                    retractedMessage
            );

            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            logger.warn("Unauthorized attempt to retract message {} by user {}", messageId, currentUser.getUsername());
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            logger.error("Error retracting message " + messageId, e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/chat/history/{contactId}")
    public ResponseEntity<List<ChatMessageDto>> getChatHistory(@PathVariable Long contactId, HttpSession session) {
        UserDto currentUser = (UserDto) session.getAttribute("loggedInUser");
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        logger.info("Fetching chat history between user ID {} and contact ID {}", currentUser.getId(), contactId);
        try {
            List<ChatMessageDto> history = chatService.getChatHistory(currentUser.getId(), contactId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Error fetching chat history for user " + currentUser.getId(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/chat/admin-contact")
    public ResponseEntity<User> getAdminContact() {
        try {
            User admin = userService.findAdmin();
            return ResponseEntity.ok(admin);
        } catch (Exception e) {
            logger.error("Could not find admin user", e);
            return ResponseEntity.status(404).build();
        }
    }

    @GetMapping("/chat/conversations")
    public ResponseEntity<List<UserDto>> getConversations(HttpSession session) {
        UserDto adminUser = (UserDto) session.getAttribute("loggedInUser");
        if (adminUser == null || !adminUser.isAdmin()) {
            return ResponseEntity.status(401).build();
        }
        try {
            List<User> users = chatService.findConversations(adminUser.getId());
            List<UserDto> userDtos = users.stream().map(UserDto::new).collect(Collectors.toList());
            return ResponseEntity.ok(userDtos);
        } catch(Exception e) {
            logger.error("Error fetching conversations for admin " + adminUser.getId(), e);
            return ResponseEntity.status(500).build();
        }
    }
}