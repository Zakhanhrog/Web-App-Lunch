package com.example.lunchapp.controller;

import com.example.lunchapp.model.dto.AiChatRequest;
import com.example.lunchapp.model.dto.AiChatResponse;
import com.example.lunchapp.model.dto.UserDto; // Cần import UserDto
import com.example.lunchapp.service.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;

@Controller
public class AiController {

    private final AiChatService aiChatService;

    @Autowired
    public AiController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @GetMapping("/ai-chat")
    public ModelAndView showAiChatPage(HttpSession session) {
        if (session.getAttribute("loggedInUser") == null) {
            return new ModelAndView("redirect:/auth/login");
        }
        return new ModelAndView("ai-chat");
    }

    @PostMapping("/api/ai/chat")
    @ResponseBody
    public ResponseEntity<?> getChatResponse(@RequestBody AiChatRequest request, HttpSession session) {
        UserDto loggedInUser = (UserDto) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.status(401).body("Vui lòng đăng nhập để sử dụng tính năng này.");
        }

        try {
            // Truyền userId xuống service
            AiChatResponse response = aiChatService.getChatResponse(request, loggedInUser.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            AiChatResponse errorResponse = new AiChatResponse();
            errorResponse.setExplanation("Lỗi khi kết nối đến AI: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}