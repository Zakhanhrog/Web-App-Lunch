package com.example.lunchapp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            try {
                int statusCode = Integer.parseInt(status.toString());

                if (statusCode == HttpStatus.NOT_FOUND.value()) {
                    return "error/404";
                } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                    return "error/500";
                }
            } catch (NumberFormatException ex) {
                return "error/500";
            }
        }

        return "error/500";
    }
}