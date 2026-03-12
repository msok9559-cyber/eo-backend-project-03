package com.example.prompt.controller.page;

import com.example.prompt.dto.user.UserDto;
import com.example.prompt.security.CustomUserDetails;
import com.example.prompt.security.CustomOAuth2UserDetails;
import com.example.prompt.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final UserService userService;

    @GetMapping("/")
    public String index(@AuthenticationPrincipal Object principal, Model model) {
        injectUser(principal, model);
        return "index";
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal Object principal, Model model) {
        injectUser(principal, model);
        return "login";
    }

    @GetMapping("/signup")
    public String signup(@AuthenticationPrincipal Object principal, Model model) {
        injectUser(principal, model);
        return "signup";
    }

    @GetMapping("/reset-password")
    public String resetPassword(@AuthenticationPrincipal Object principal, Model model) {
        injectUser(principal, model);
        return "reset-password";
    }

    @GetMapping("/payment")
    public String payment(@AuthenticationPrincipal Object principal, Model model) {
        injectUser(principal, model);
        return "payment";
    }

    // 로그인한 유저 정보 model에 주입
    private void injectUser(Object principal, Model model) {
        if (principal instanceof CustomUserDetails userDetails) {
            UserDto dto = userService.getMyInfo(userDetails.getId());
            model.addAttribute("myInfo", dto);
        } else if (principal instanceof CustomOAuth2UserDetails oauthDetails) {
            UserDto dto = userService.getMyInfo(oauthDetails.getId());
            model.addAttribute("myInfo", dto);
        }
    }
}