package com.example.lunchapp.controller;

import com.example.lunchapp.model.dto.UserDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

@Controller
public class HomeController {

    private static final String LOGGED_IN_USER_SESSION_KEY = "loggedInUser";

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        UserDto loggedInUser = (UserDto) session.getAttribute(LOGGED_IN_USER_SESSION_KEY);

        if (loggedInUser != null) {
            model.addAttribute("loggedInUser", loggedInUser);
            model.addAttribute("message", "Welcome back, " + loggedInUser.getUsername() + "!");
            // Kiểm tra vai trò để hiển thị nội dung phù hợp
            if (loggedInUser.getRoles().contains("ROLE_ADMIN")) {
                model.addAttribute("isAdmin", true);
            } else {
                model.addAttribute("isUser", true);
            }
        } else {
            model.addAttribute("message", "Welcome to CG LUNCH! Please login or register.");
        }
        return "home";
    }
}