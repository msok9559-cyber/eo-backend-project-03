package com.example.prompt.controller.payment;

import com.example.prompt.dto.payment.PaymentDto;
import com.example.prompt.security.CustomUserDetails;
import com.example.prompt.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 결제 검증 및 플랜 업그레이드
     * POST /api/payments/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<PaymentDto> verifyPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PaymentDto dto) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(
                    PaymentDto.builder().success(false).message("로그인이 필요합니다.").build()
            );
        }

        log.info("결제 검증 요청 - userId: {}, impUid: {}", userDetails.getId(), dto.getImpUid());
        PaymentDto result = paymentService.verifyAndSave(userDetails.getId(), dto);
        return ResponseEntity.ok(result);
    }

    /**
     * 내 결제 내역 조회
     * GET /api/payments
     */
    @GetMapping
    public ResponseEntity<List<PaymentDto>> getMyPayments(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(null);
        }

        log.info("결제 내역 조회 - userId: {}", userDetails.getId());
        List<PaymentDto> payments = paymentService.getMyPayments(userDetails.getId());
        return ResponseEntity.ok(payments);
    }
}
