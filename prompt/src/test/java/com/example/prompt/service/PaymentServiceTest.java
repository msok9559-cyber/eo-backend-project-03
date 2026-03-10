package com.example.prompt.service;

import com.example.prompt.domain.PlanEntity;
import com.example.prompt.domain.UserEntity;
import com.example.prompt.repository.PaymentRepository;
import com.example.prompt.repository.PlanRepository;
import com.example.prompt.repository.UserRepository;
import com.siot.IamportRestClient.IamportClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
@Transactional
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private IamportClient iamportClient;

    private PlanEntity normalPlan;
    private PlanEntity proPlan;
    private UserEntity testUser;

    @BeforeEach
    public void setup() {
        //플랜 저장
        normalPlan = planRepository.save(PlanEntity.builder()
                        .planName("NORMAL")
                        .tokenLimit(10000)
                        .aiUse(0)
                        .price(0)
                        .build()
                );

        proPlan = planRepository.save(PlanEntity.builder()
                        .planName("PRO")
                        .tokenLimit(50000)
                        .aiUse(0)
                        .price(9900)
                        .build()
                );

        // 테스트 유저 저장
        testUser = userRepository.save(UserEntity.builder()
                .userid("payment_user")
                .username("결제테스트유저")
                .email("payment@gmail.com")
                .password("encodedPassword123!")
                .plan(normalPlan)
                .build());

        log.info("setUp - 플랜 및 유저 저장 완료");
    }

    // PaymentService Bean 확인
    @Test
    public void testExists() {
        assertNotNull(paymentService);
        log.info("PaymentService = {}", paymentService);
    }
}