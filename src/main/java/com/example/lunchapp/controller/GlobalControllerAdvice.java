package com.example.lunchapp.controller;

import com.example.lunchapp.model.dto.UserDto;
import com.example.lunchapp.model.entity.User;
import com.example.lunchapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@ControllerAdvice(basePackages = "com.example.lunchapp.controller")
public class GlobalControllerAdvice {

    private final UserService userService;

    @Autowired
    public GlobalControllerAdvice(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("loggedInUser")
    public UserDto loggedInUser(HttpSession session) {
        return (UserDto) session.getAttribute("loggedInUser");
    }

    @ModelAttribute("chatAdminUser")
    public UserDto chatAdminUser() {
        try {
            User admin = userService.findAdmin();
            if (admin != null) {
                return new UserDto(admin);
            }
        } catch (Exception e) {
            System.out.println("Error fetching admin user: " + e.getMessage());
        }
        return null;
    }

    @ModelAttribute("isUserPage")
    public boolean isUserPage(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String uri = request.getRequestURI();
        return (uri != null && !uri.startsWith("/admin"));
    }
}