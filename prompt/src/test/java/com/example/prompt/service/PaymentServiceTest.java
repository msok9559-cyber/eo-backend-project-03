package com.example.prompt.service;

import com.example.prompt.domain.PlanEntity;
import com.example.prompt.domain.UserEntity;
import com.example.prompt.dto.payment.PaymentDto;
import com.example.prompt.repository.PaymentRepository;
import com.example.prompt.repository.PlanRepository;
import com.example.prompt.repository.UserRepository;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    // 결제 검증 성공 테스트
    @Test
    public void testVerifyAndSave_success() throws IamportResponseException, IOException {
        log.info("Testing verifyAndSave - success");

        // 포트원 응답 Mock 설정
        Payment mockPayment = mock(Payment.class);
        when(mockPayment.getStatus()).thenReturn("paid");
        when(mockPayment.getAmount()).thenReturn(new BigDecimal(9900));

        IamportResponse<Payment> mockResponse = mock(IamportResponse.class);
        when(mockResponse.getResponse()).thenReturn(mockPayment);

        when(iamportClient.paymentByImpUid(anyString())).thenReturn(mockResponse);

        PaymentDto dto = PaymentDto.builder()
                .impUid("imp_test_001")
                .planName("PRO")
                .amount(9900)
                .build();

        PaymentDto result = paymentService.verifyAndSave(testUser.getId(), dto);

        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertThat(result.getPlanName()).isEqualTo("PRO");
        assertThat(result.getAmount()).isEqualTo(9900);
        log.info("결제 검증 성공 result = {}", result);
    }

    // 중복 결제 테스트
    @Test
    public void testVerifyAndSave_duplicatePayment() throws IamportResponseException, IOException {
        log.info("Testing verifyAndSave - duplicate payment");

        Payment mockPayment = mock(Payment.class);
        when(mockPayment.getStatus()).thenReturn("paid");
        when(mockPayment.getAmount()).thenReturn(new BigDecimal(9900));

        IamportResponse<Payment> mockResponse = mock(IamportResponse.class);
        when(mockResponse.getResponse()).thenReturn(mockPayment);

        when(iamportClient.paymentByImpUid(anyString())).thenReturn(mockResponse);

        PaymentDto dto = PaymentDto.builder()
                .impUid("imp_test_duplicate")
                .planName("PRO")
                .amount(9900)
                .build();

        paymentService.verifyAndSave(testUser.getId(), dto);

        assertThrows(IllegalArgumentException.class,
                () -> paymentService.verifyAndSave(testUser.getId(), dto));

        log.info("중복 결제 테스트 passed");
    }

    // 금액 불일치 테스트
    @Test
    public void testVerifyAndSave_amountMismatch() throws IamportResponseException, IOException {
        log.info("Testing verifyAndSave - amount mismatch");

        Payment mockPayment = mock(Payment.class);
        when(mockPayment.getStatus()).thenReturn("paid");
        when(mockPayment.getAmount()).thenReturn(new BigDecimal(1000));

        IamportResponse<Payment> mockResponse = mock(IamportResponse.class);
        when(mockResponse.getResponse()).thenReturn(mockPayment);

        when(iamportClient.paymentByImpUid(anyString())).thenReturn(mockResponse);

        PaymentDto dto = PaymentDto.builder()
                .impUid("imp_test_mismatch")
                .planName("PRO")
                .amount(1000)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> paymentService.verifyAndSave(testUser.getId(), dto));

        log.info("금액 불일치 테스트 passed");
    }

    // 결제 내역 조회
    @Test
    public void testGetMyPayments() throws IamportResponseException, IOException {
        log.info("Testing getMyPayments");

        Payment mockPayment = mock(Payment.class);
        when(mockPayment.getStatus()).thenReturn("paid");
        when(mockPayment.getAmount()).thenReturn(new BigDecimal(9900));

        IamportResponse<Payment> mockResponse = mock(IamportResponse.class);
        when(mockResponse.getResponse()).thenReturn(mockPayment);

        when(iamportClient.paymentByImpUid(anyString())).thenReturn(mockResponse);

        paymentService.verifyAndSave(testUser.getId(), PaymentDto.builder()
                .impUid("imp_test_history")
                .planName("PRO")
                .amount(9900)
                .build());

        var payments = paymentService.getMyPayments(testUser.getId());

        assertNotNull(payments);
        assertThat(payments.size()).isGreaterThan(0);
        assertThat(payments.get(0).getPlanName()).isEqualTo("PRO");
        log.info("결제 내역 조회 result = {}", payments);
    }

}