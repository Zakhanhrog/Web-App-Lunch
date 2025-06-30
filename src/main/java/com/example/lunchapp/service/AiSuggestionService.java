package com.example.lunchapp.service;

import com.example.lunchapp.model.dto.AiSuggestionResponse;
import java.io.IOException;

public interface AiSuggestionService {
    AiSuggestionResponse getMealSuggestion(String userPrompt) throws IOException;
}