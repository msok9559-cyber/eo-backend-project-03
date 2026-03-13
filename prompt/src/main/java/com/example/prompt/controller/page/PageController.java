package com.example.prompt.controller.page;

import com.example.prompt.domain.PlanEntity;
import com.example.prompt.dto.user.UserDto;
import com.example.prompt.security.CustomUserDetails;
import com.example.prompt.security.CustomOAuth2UserDetails;
import com.example.prompt.service.PlanService;
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
    private final PlanService planService;

    // "http://localhost:8080/" 메인 페이지
    @GetMapping("/")
    public String index(@AuthenticationPrincipal Object principal, Model model) {
        injectUser(principal, model);
        return "index";
    }

    // "http://localhost:8080/chat" 채팅 페이지
    @GetMapping("/chat")
    public String chat(@AuthenticationPrincipal Object principal, Model model) {
        injectUser(principal, model);
        return "ai-chat/chat";
    }

    // "http://localhost:8080/login" 로그인 페이지
    @GetMapping("/login")
    public String login(@AuthenticationPrincipal Object principal, Model model) {
        injectUser(principal, model);
        return "login";
    }

    // "http://localhost:8080/signup" 회원가입 페이지
    @GetMapping("/signup")
    public String signup(@AuthenticationPrincipal Object principal, Model model) {
        injectUser(principal, model);
        return "signup";
    }

    // "http://localhost:8080/mypage" 마이페이지
    @GetMapping("/mypage")
    public String mypage(@AuthenticationPrincipal Object principal, Model model) {
        injectUser(principal, model);
        return "mypage";
    }

    // "http://localhost:8080/payment" 결제 페이지
    @GetMapping("/payment")
    public String payment(@AuthenticationPrincipal Object principal, Model model) {
        injectUser(principal, model);
        List<PlanEntity> plans = planService.getAllPlans();
        model.addAttribute("plans", plans);
        return "payment";
    }

    // "http://localhost:8080/reset-password" 비밀번호 찾기 페이지
    @GetMapping("/reset-password")
    public String resetPassword(@AuthenticationPrincipal Object principal, Model model) {
        injectUser(principal, model);
        return "reset-password";
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