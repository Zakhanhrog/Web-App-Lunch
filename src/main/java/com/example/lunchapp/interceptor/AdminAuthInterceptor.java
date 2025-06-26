package com.example.lunchapp.interceptor;

import com.example.lunchapp.model.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AdminAuthInterceptor.class);
    private static final String LOGGED_IN_USER_SESSION_KEY = "loggedInUser";
    private static final String ADMIN_ROLE_NAME = "ROLE_ADMIN";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        logger.info("======================================================================");
        logger.info("AdminAuthInterceptor: preHandle INVOKED for URI: {}", request.getRequestURI());

        HttpSession session = request.getSession(false);

        if (session == null) {
            logger.warn("AdminAuthInterceptor: Session is NULL. Redirecting to login page. URI: {}", request.getRequestURI());
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return false;
        }
        logger.debug("AdminAuthInterceptor: Session ID: {}", session.getId());

        Object userObjFromSession = session.getAttribute(LOGGED_IN_USER_SESSION_KEY);

        if (userObjFromSession == null) {
            logger.warn("AdminAuthInterceptor: No user found in session (key '{}'). Redirecting to login page. URI: {}", LOGGED_IN_USER_SESSION_KEY, request.getRequestURI());
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return false;
        }

        if (!(userObjFromSession instanceof UserDto)) {
            logger.error("AdminAuthInterceptor: Object in session with key '{}' is NOT an instance of UserDto. Actual type: {}. URI: {}. Forcing logout/redirect.",
                    LOGGED_IN_USER_SESSION_KEY, userObjFromSession.getClass().getName(), request.getRequestURI());
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/auth/login?error=session_corrupted");
            return false;
        }

        UserDto loggedInUser = (UserDto) userObjFromSession;
        logger.debug("AdminAuthInterceptor: User found in session: Username='{}', Roles='{}'", loggedInUser.getUsername(), loggedInUser.getRoles());

        if (loggedInUser.getRoles() == null) {
            logger.warn("AdminAuthInterceptor: User '{}' has NULL roles. Access denied for URI: {}. Redirecting to home.", loggedInUser.getUsername(), request.getRequestURI());
            response.sendRedirect(request.getContextPath() + "/");
            return false;
        }

        if (!loggedInUser.getRoles().contains(ADMIN_ROLE_NAME)) {
            logger.warn("AdminAuthInterceptor: User '{}' does NOT have ADMIN_ROLE ('{}'). Actual roles: {}. Access denied for URI: {}. Redirecting to home.",
                    loggedInUser.getUsername(), ADMIN_ROLE_NAME, loggedInUser.getRoles(), request.getRequestURI());
            response.sendRedirect(request.getContextPath() + "/");
            return false;
        }

        logger.info("AdminAuthInterceptor: Admin user '{}' GRANTED access to admin page. URI: {}", loggedInUser.getUsername(), request.getRequestURI());
        logger.info("======================================================================");
        return true;
    }
}