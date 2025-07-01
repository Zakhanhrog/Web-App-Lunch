package com.example.lunchapp.service;

import com.example.lunchapp.model.dto.AiChatRequest;
import com.example.lunchapp.model.dto.AiChatResponse;

import java.io.IOException;

public interface AiChatService {
    AiChatResponse getChatResponse(AiChatRequest chatRequest) throws IOException;
}