package com.example.lunchapp.controller;

import com.example.lunchapp.model.dto.UserDto;
import com.example.lunchapp.service.PushNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;
import java.util.Map;

@Controller
@RequestMapping("/api/push")
public class PushController {

    private final PushNotificationService pushNotificationService;

    public PushController(PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
    }

    @PostMapping("/subscribe")
    @ResponseBody
    public ResponseEntity<?> subscribe(@RequestBody Map<String, String> payload, HttpSession session) {
        UserDto currentUserDto = (UserDto) session.getAttribute("loggedInUser");
        if (currentUserDto == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "error", "message", "User not logged in"));
        }

        String token = payload.get("token");
        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Token is missing"));
        }

        try {
            pushNotificationService.subscribe(currentUserDto.getId(), token);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    @GetMapping("/test-send-all")
    @ResponseBody
    public ResponseEntity<?> testSendNotificationToAll(HttpSession session) {
        UserDto currentUserDto = (UserDto) session.getAttribute("loggedInUser");
        if (currentUserDto == null || !currentUserDto.getRoles().contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Chỉ admin mới có quyền test.");
        }

        String title = "Thông Báo Test";
        String body = "Đây là thông báo được gửi lúc " + java.time.LocalTime.now().toString();

        try {
            pushNotificationService.sendNotificationToAll(title, body);
            return ResponseEntity.ok("Đã kích hoạt gửi thông báo test. Hãy kiểm tra thiết bị của bạn.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi gửi thông báo: " + e.getMessage());
        }
    }
}