package com.example.prompt.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentDto {

    // 포트원 결제 고유 번호
    private String impUid;

    // 결제할 플랜 이름 (PRO, MAX)
    private String planName;

    // 결제 금액
    private Integer amount;

    // 결제 완료 시간
    private LocalDateTime paidAt;

    // 결제 성공 여부
    private Boolean success;

    // 메세지
    private String message;

}
