package com.example.prompt.controller.user;

import com.example.prompt.domain.PlanEntity;
import com.example.prompt.repository.PlanRepository;
import com.example.prompt.repository.UserRepository;
import com.example.prompt.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Slf4j
@AutoConfigureMockMvc
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @BeforeEach
    public void setUp() {
        planRepository.save(PlanEntity.builder()
                .planName("NORMAL")
                .tokenLimit(10000)
                .aiUse(0)
                .price(0)
                .build());
        log.info("setUp - NORMAL 플랜 저장 완료");
    }

    // MockMvc Bean 확인
    @Test
    public void testExists() {
        assertNotNull(mockMvc);
        log.info("MockMvc = {}", mockMvc);
    }

    // 아이디 중복 확인 API 테스트
    @Test
    public void testCheckId() throws Exception {
        log.info("Testing GET /api/users/check-id");

        mockMvc.perform(get("/api/users/check-id")
                        .param("userid", "brand_new_id"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));

        log.info("checkId test passed");
    }

    // 회원가입 API 테스트
    @Test
    public void testSignup() throws Exception {
        String email = "ctrl_signup@gmail.com";
        String code = emailService.sendVerificationCode(email);
        emailService.verifyCode(email, code);

        Map<String, Object> body = createSignupBody("ctrl_user", "컨트롤러유저", email, "password123!", "password123!");
        log.info("Testing POST /api/users : {}", body);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        log.info("signup API test passed");
    }

    // 회원가입 실패 - 이메일 미인증
    @Test
    public void testSignup_emailNotVerified() throws Exception {
        Map<String, Object> body = createSignupBody("noverify_ctrl", "미인증유저", "noverify_ctrl@gmail.com", "password123!", "password123!");
        log.info("Testing signup without email verification");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        log.info("Email not verified API test passed");
    }

    // 내 정보 조회 API 테스트 - 로그인 없이 접근 시 401
    @Test
    public void testGetMyPage_unauthorized() throws Exception {
        log.info("Testing GET /api/mypage without login");

        mockMvc.perform(get("/api/mypage"))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        log.info("Unauthorized mypage test passed");
    }

    // Helper method
    private Map<String, Object> createSignupBody(String userid, String username, String email,
                                                 String password, String passwordConfirm) {
        Map<String, Object> body = new HashMap<>();
        body.put("userid", userid);
        body.put("username", username);
        body.put("email", email);
        body.put("password", password);
        body.put("passwordConfirm", passwordConfirm);
        body.put("agree", true);
        return body;
    }
}