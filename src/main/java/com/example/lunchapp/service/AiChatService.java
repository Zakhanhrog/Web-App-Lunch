package com.example.lunchapp.service;

import com.example.lunchapp.model.dto.AiChatRequest;
import com.example.lunchapp.model.dto.AiChatResponse;

import java.io.IOException;

public interface AiChatService {
    // Phương thức cũ, có thể giữ lại hoặc xóa đi nếu không cần
    AiChatResponse getChatResponse(AiChatRequest chatRequest) throws IOException;

    // Phương thức mới để cá nhân hóa
    AiChatResponse getChatResponse(AiChatRequest chatRequest, Long userId) throws IOException;
}