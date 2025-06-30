package com.example.lunchapp.controller;

import com.example.lunchapp.model.dto.AiSuggestionRequest;
import com.example.lunchapp.model.dto.AiSuggestionResponse;
import com.example.lunchapp.service.AiSuggestionService;
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

    private final AiSuggestionService aiSuggestionService;

    @Autowired
    public AiController(AiSuggestionService aiSuggestionService) {
        this.aiSuggestionService = aiSuggestionService;
    }

    @PostMapping("/suggest-menu")
    @ResponseBody
    public ResponseEntity<?> getSuggestion(@RequestBody AiSuggestionRequest request, HttpSession session) {
        if (session.getAttribute("loggedInUser") == null) {
            return ResponseEntity.status(401).body("Vui lòng đăng nhập để sử dụng tính năng này.");
        }

        try {
            AiSuggestionResponse suggestion = aiSuggestionService.getMealSuggestion(request.getUserPrompt());
            return ResponseEntity.ok(suggestion);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi khi kết nối đến AI: " + e.getMessage());
        }
    }
}