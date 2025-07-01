package com.example.lunchapp.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AiChatRequest {
    private String newMessage;
    private List<ChatMessage> history;
}