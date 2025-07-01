package com.example.lunchapp.controller;

import com.example.lunchapp.model.dto.AiChatRequest;
import com.example.lunchapp.model.dto.AiChatResponse;
import com.example.lunchapp.service.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/api/ai")
public class AiController {

    private final AiChatService aiChatService;

    @Autowired
    public AiController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    @ResponseBody
    public ResponseEntity<?> getChatResponse(@RequestBody AiChatRequest request, HttpSession session) {
        if (session.getAttribute("loggedInUser") == null) {
            return ResponseEntity.status(401).body("Vui lòng đăng nhập để sử dụng tính năng này.");
        }

        try {
            AiChatResponse response = aiChatService.getChatResponse(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            AiChatResponse errorResponse = new AiChatResponse();
            errorResponse.setExplanation("Lỗi khi kết nối đến AI: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}