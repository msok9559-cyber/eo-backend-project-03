package com.example.prompt.controller.payment;

import com.example.prompt.domain.PlanEntity;
import com.example.prompt.domain.UserEntity;
import com.example.prompt.dto.payment.PaymentDto;
import com.example.prompt.repository.PlanRepository;
import com.example.prompt.repository.UserRepository;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
@Transactional
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private IamportClient iamportClient;

    @BeforeEach
    public void setUp() {
        PlanEntity normalPlan = planRepository.save(PlanEntity.builder()
                .planName("NORMAL")
                .tokenLimit(10000)
                .aiUse(0)
                .price(0)
                .build());

        planRepository.save(PlanEntity.builder()
                .planName("PRO")
                .tokenLimit(50000)
                .aiUse(0)
                .price(9900)
                .build());

        userRepository.save(UserEntity.builder()
                .userid("payment_user")
                .username("결제테스트유저")
                .email("payment@gmail.com")
                .password("$2a$10$encodedPasswordHere")
                .plan(normalPlan)
                .build());

        log.info("setUp - 플랜 및 유저 저장 완료");
    }

    // MockMvc Bean 확인
    @Test
    public void testExists() {
        assertNotNull(mockMvc);
        log.info("MockMvc = {}", mockMvc);
    }

    // 비로그인 상태에서 결제 API 접근 - 리다이렉트
    @Test
    public void testVerifyPayment_unauthorized() throws Exception {
        log.info("Testing POST /api/payments/verify - unauthorized");

        mockMvc.perform(post("/api/payments/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                PaymentDto.builder()
                                        .impUid("test")
                                        .planName("PRO")
                                        .amount(9900)
                                        .build()
                        )))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        log.info("Unauthorized payment test passed");
    }

    // 로그인 상태에서 결제 검증 성공
    @Test
    @WithUserDetails(value = "payment_user", userDetailsServiceBeanName = "customUserDetailsService", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void testVerifyPayment_success() throws Exception {
        log.info("Testing POST /api/payments/verify - success");

        Payment mockPayment = mock(Payment.class);
        when(mockPayment.getStatus()).thenReturn("paid");
        when(mockPayment.getAmount()).thenReturn(new BigDecimal(9900));

        IamportResponse<Payment> mockResponse = mock(IamportResponse.class);
        when(mockResponse.getResponse()).thenReturn(mockPayment);

        when(iamportClient.paymentByImpUid(anyString())).thenReturn(mockResponse);

        PaymentDto dto = PaymentDto.builder()
                .impUid("imp_test_ctrl_001")
                .planName("PRO")
                .amount(9900)
                .build();

        mockMvc.perform(post("/api/payments/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.planName").value("PRO"));

        log.info("Payment verify success test passed");
    }

    // 로그인 상태에서 결제 내역 조회
    @Test
    @WithUserDetails(value = "payment_user", userDetailsServiceBeanName = "customUserDetailsService", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void testGetMyPayments() throws Exception {
        log.info("Testing GET /api/payments");

        mockMvc.perform(get("/api/payments"))
                .andDo(print())
                .andExpect(status().isOk());

        log.info("Get my payments test passed");
    }


}