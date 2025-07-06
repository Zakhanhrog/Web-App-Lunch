package com.example.lunchapp.service.impl;

import com.example.lunchapp.model.dto.ChatMessageDto;
import com.example.lunchapp.model.entity.ChatMessage;
import com.example.lunchapp.model.entity.MessageType;
import com.example.lunchapp.model.entity.User;
import com.example.lunchapp.repository.ChatMessageRepository;
import com.example.lunchapp.repository.UserRepository;
import com.example.lunchapp.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    @Autowired
    public ChatServiceImpl(ChatMessageRepository chatMessageRepository, UserRepository userRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ChatMessageDto saveMessage(ChatMessageDto chatMessageDto) {
        User sender = userRepository.findById(chatMessageDto.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender not found with id: " + chatMessageDto.getSenderId()));
        User recipient = userRepository.findById(chatMessageDto.getRecipientId())
                .orElseThrow(() -> new RuntimeException("Recipient not found with id: " + chatMessageDto.getRecipientId()));

        ChatMessage chatMessage = ChatMessage.builder()
                .sender(sender)
                .recipient(recipient)
                .content(chatMessageDto.getContent())
                .timestamp(LocalDateTime.now())
                .messageType(chatMessageDto.getMessageType())
                .isRead(false)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        return convertToDto(savedMessage);
    }

    @Override
    @Transactional
    public ChatMessageDto retractMessage(Long messageId, Long currentUserId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found with id: " + messageId));

        if (!Objects.equals(message.getSender().getId(), currentUserId)) {
            throw new SecurityException("User does not have permission to retract this message.");
        }

        message.setMessageType(MessageType.RETRACTED);
        message.setContent("Tin nhắn đã được gỡ");

        ChatMessage retractedMessage = chatMessageRepository.save(message);
        return convertToDto(retractedMessage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getChatHistory(Long userId1, Long userId2) {
        List<ChatMessage> messages = chatMessageRepository.findChatHistory(userId1, userId2);
        return messages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findConversations(Long adminId) {
        List<User> users = userRepository.findUsersWithChatHistoryWithAdmin(adminId);
        users.sort(Comparator.comparing(
                (User u) -> {
                    List<ChatMessage> lastMessages = chatMessageRepository.findLastMessageBetweenUsers(adminId, u.getId());
                    return lastMessages.isEmpty() ? LocalDateTime.MIN : lastMessages.get(0).getTimestamp();
                }
        ).reversed());
        return users;
    }

    private ChatMessageDto convertToDto(ChatMessage message) {
        return ChatMessageDto.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .senderUsername(message.getSender().getUsername())
                .recipientId(message.getRecipient().getId())
                .recipientUsername(message.getRecipient().getUsername())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .messageType(message.getMessageType())
                .isRead(message.isRead())
                .build();
    }
}