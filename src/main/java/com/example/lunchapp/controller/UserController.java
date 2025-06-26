package com.example.lunchapp.controller;

import com.example.lunchapp.model.dto.ChangePasswordDto;
import com.example.lunchapp.model.dto.UserDto;
import com.example.lunchapp.model.entity.Order;
import com.example.lunchapp.model.entity.User;
import com.example.lunchapp.service.OrderService;
import com.example.lunchapp.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private static final String LOGGED_IN_USER_SESSION_KEY = "loggedInUser";

    private final UserService userService;
    private final OrderService orderService;

    @Autowired
    public UserController(UserService userService, OrderService orderService) {
        this.userService = userService;
        this.orderService = orderService;
    }

    @GetMapping("/order-history")
    public String showOrderHistory(Model model, HttpSession session) {
        UserDto currentUser = (UserDto) session.getAttribute(LOGGED_IN_USER_SESSION_KEY);
        if (currentUser == null) {
            return "redirect:/auth/login";
        }
        List<Order> orders = orderService.getOrderHistory(currentUser.getId());
        model.addAttribute("orders", orders);
        return "user/order-history";
    }

    @GetMapping("/profile")
    public String showProfilePage(Model model, HttpSession session) {
        UserDto currentUser = (UserDto) session.getAttribute(LOGGED_IN_USER_SESSION_KEY);
        if (currentUser == null) {
            return "redirect:/auth/login";
        }
        model.addAttribute("changePasswordDto", new ChangePasswordDto());
        return "user/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute("changePasswordDto") ChangePasswordDto changePasswordDto,
                                 BindingResult bindingResult,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        UserDto currentUser = (UserDto) session.getAttribute(LOGGED_IN_USER_SESSION_KEY);
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        if (!changePasswordDto.getNewPassword().equals(changePasswordDto.getConfirmNewPassword())) {
            bindingResult.rejectValue("confirmNewPassword", "error.changePasswordDto", "New passwords do not match.");
        }

        if (bindingResult.hasErrors()) {
            logger.warn("Change password form validation errors for user {}: {}", currentUser.getUsername(), bindingResult.getAllErrors());
            return "user/profile";
        }

        try {
            userService.changePassword(currentUser.getId(), changePasswordDto.getNewPassword());
            redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully.");
            logger.info("Password changed successfully for user {}", currentUser.getUsername());
        } catch (RuntimeException e) {
            logger.error("Error changing password for user {}: {}",currentUser.getUsername(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error changing password: " + e.getMessage());
        }
        return "redirect:/user/profile";
    }

    @GetMapping("/deposit")
    public String showDepositForm(Model model, HttpSession session) {
        UserDto currentUser = (UserDto) session.getAttribute(LOGGED_IN_USER_SESSION_KEY);
        if (currentUser == null) {
            return "redirect:/auth/login";
        }
        return "user/deposit-money";
    }

    @PostMapping("/deposit")
    public String processDeposit(@RequestParam("amount") BigDecimal amount,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        UserDto currentUser = (UserDto) session.getAttribute(LOGGED_IN_USER_SESSION_KEY);
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Deposit amount must be a positive number.");
            return "redirect:/user/deposit";
        }

        try {
            User updatedUserEntity = userService.depositMoney(currentUser.getId(), amount);
            UserDto updatedUserDto = new UserDto(updatedUserEntity);
            session.setAttribute(LOGGED_IN_USER_SESSION_KEY, updatedUserDto);

            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Successfully deposited %.2f VND. Your new balance is %.2f VND.",
                            amount, updatedUserDto.getBalance()));
            logger.info("User {} deposited {}. New balance: {}", currentUser.getUsername(), amount, updatedUserDto.getBalance());
        } catch (RuntimeException e) {
            logger.error("Error depositing money for user {}: {}",currentUser.getUsername(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error depositing money: " + e.getMessage());
        }
        return "redirect:/user/deposit";
    }
}