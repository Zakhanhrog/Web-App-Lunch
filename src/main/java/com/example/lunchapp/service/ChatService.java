package com.example.lunchapp.service;

import com.example.lunchapp.model.dto.ChatMessageDto;
import com.example.lunchapp.model.entity.User;

import java.util.List;

public interface ChatService {
    ChatMessageDto saveMessage(ChatMessageDto chatMessageDto);
    List<ChatMessageDto> getChatHistory(Long userId1, Long userId2);
    List<User> findConversations(Long adminId);
    ChatMessageDto retractMessage(Long messageId, Long currentUserId);
}