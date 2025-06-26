package com.example.lunchapp.controller;

import com.example.lunchapp.model.dto.LoginRequestDto;
import com.example.lunchapp.model.dto.UserDto;
import com.example.lunchapp.model.dto.UserRegistrationDto;
import com.example.lunchapp.model.entity.User;
import com.example.lunchapp.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.Optional;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final String LOGGED_IN_USER_SESSION_KEY = "loggedInUser";
    private static final String REMEMBER_ME_COOKIE_NAME = "rememberMeUser";


    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    // --- Đăng ký ---
    @GetMapping("/register")
    public String showRegistrationForm(Model model, HttpServletRequest request) {
        // Nếu đã đăng nhập thì redirect về trang chủ
        if (request.getSession().getAttribute(LOGGED_IN_USER_SESSION_KEY) != null) {
            return "redirect:/";
        }
        model.addAttribute("userRegistrationDto", new UserRegistrationDto());
        return "auth/register"; // Trả về view register.html trong /templates/auth/
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("userRegistrationDto") UserRegistrationDto registrationDto,
                                      BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            logger.warn("Registration form validation errors: {}", bindingResult.getAllErrors());
            return "auth/register"; // Quay lại form với lỗi
        }
        try {
            userService.registerUser(registrationDto);
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please login.");
            logger.info("User registered successfully: {}", registrationDto.getUsername());
            return "redirect:/auth/login";
        } catch (RuntimeException e) { // Sẽ cải thiện Exception Handling sau
            logger.error("Registration failed for user {}: {}", registrationDto.getUsername(), e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        }
    }

    // --- Đăng nhập ---
    @GetMapping("/login")
    public String showLoginForm(Model model, HttpServletRequest request) {
        // Kiểm tra session trước
        if (request.getSession().getAttribute(LOGGED_IN_USER_SESSION_KEY) != null) {
            return "redirect:/"; // Đã đăng nhập, về trang chủ
        }

        // Kiểm tra cookie "remember me"
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (REMEMBER_ME_COOKIE_NAME.equals(cookie.getName())) {
                    String username = cookie.getValue();
                    Optional<User> userOpt = userService.findByUsername(username);
                    if (userOpt.isPresent()) {
                        // Giả định nếu có cookie là đã "nhớ" và đăng nhập thành công
                        // Trong thực tế, bạn có thể muốn mã hóa giá trị cookie hoặc dùng token phức tạp hơn
                        HttpSession session = request.getSession();
                        session.setAttribute(LOGGED_IN_USER_SESSION_KEY, new UserDto(userOpt.get()));
                        logger.info("User {} auto-logged in via remember-me cookie", username);
                        return "redirect:/"; // Về trang chủ
                    } else {
                        // Cookie không hợp lệ, xóa đi
                        cookie.setMaxAge(0);
                        // response.addCookie(cookie); // Cần HttpServletResponse để addCookie
                    }
                }
            }
        }

        model.addAttribute("loginRequestDto", new LoginRequestDto());
        return "auth/login"; // Trả về view login.html trong /templates/auth/
    }

    @PostMapping("/login")
    public String processLogin(@Valid @ModelAttribute("loginRequestDto") LoginRequestDto loginDto,
                               BindingResult bindingResult, Model model, HttpSession session,
                               HttpServletResponse response, RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            logger.warn("Login form validation errors for user {}: {}", loginDto.getUsername(), bindingResult.getAllErrors());
            return "auth/login";
        }

        Optional<User> userOpt = userService.findByUsername(loginDto.getUsername());

        if (userOpt.isPresent() && passwordEncoder.matches(loginDto.getPassword(), userOpt.get().getPassword())) {
            User user = userOpt.get();
            if (!user.isEnabled()) {
                model.addAttribute("errorMessage", "Your account has been disabled.");
                logger.warn("Login attempt for disabled account: {}", loginDto.getUsername());
                return "auth/login";
            }

            session.setAttribute(LOGGED_IN_USER_SESSION_KEY, new UserDto(user)); // Lưu UserDto vào session
            session.setMaxInactiveInterval(30 * 60); // Session timeout 30 phút

            if (loginDto.isRememberMe()) {
                Cookie rememberMeCookie = new Cookie(REMEMBER_ME_COOKIE_NAME, user.getUsername());
                rememberMeCookie.setMaxAge(7 * 24 * 60 * 60); // 7 ngày
                rememberMeCookie.setPath("/"); // Cookie có hiệu lực trên toàn bộ ứng dụng
                // rememberMeCookie.setHttpOnly(true); // Nên có để tăng bảo mật
                // rememberMeCookie.setSecure(true); // Chỉ gửi qua HTTPS, dùng khi deploy lên HTTPS
                response.addCookie(rememberMeCookie);
                logger.info("Remember-me cookie set for user: {}", user.getUsername());
            } else {
                // Nếu không chọn remember me, xóa cookie (nếu có)
                Cookie rememberMeCookie = new Cookie(REMEMBER_ME_COOKIE_NAME, null);
                rememberMeCookie.setMaxAge(0);
                rememberMeCookie.setPath("/");
                response.addCookie(rememberMeCookie);
            }

            logger.info("User {} logged in successfully.", user.getUsername());
            // Kiểm tra vai trò để redirect
            // Tạm thời redirect về trang chủ cho tất cả
            return "redirect:/";

        } else {
            model.addAttribute("errorMessage", "Invalid username or password.");
            logger.warn("Login failed for user: {}", loginDto.getUsername());
            return "auth/login";
        }
    }

    // --- Đăng xuất ---
    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false); // false: không tạo session mới nếu chưa có
        if (session != null) {
            UserDto loggedInUser = (UserDto) session.getAttribute(LOGGED_IN_USER_SESSION_KEY);
            if (loggedInUser != null) {
                logger.info("User {} logging out.", loggedInUser.getUsername());
            }
            session.invalidate(); // Hủy session
        }

        // Xóa cookie remember-me
        Cookie rememberMeCookie = new Cookie(REMEMBER_ME_COOKIE_NAME, null);
        rememberMeCookie.setMaxAge(0); // Đặt thời gian sống bằng 0 để xóa cookie
        rememberMeCookie.setPath("/");
        response.addCookie(rememberMeCookie);
        logger.info("Remember-me cookie cleared on logout.");

        return "redirect:/auth/login?logout"; // Redirect về trang login với param logout (để hiển thị thông báo)
    }
}