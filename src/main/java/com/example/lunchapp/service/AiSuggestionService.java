package com.example.lunchapp.service;

import com.example.lunchapp.model.dto.AiChatRequest;
import java.io.IOException;

public interface AiSuggestionService {
    AiChatRequest getMealSuggestion(String userPrompt) throws IOException;
}