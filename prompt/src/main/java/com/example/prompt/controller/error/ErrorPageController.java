package com.example.prompt.controller.error;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ErrorPageController {
    /**
     * 403
     */
    @GetMapping("/error/403")
    public String accessDeniedPage() {
        return "error/403";
    }
}
