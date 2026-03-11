package com.example.prompt.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StatsDto {

    // 총 가입자 수
    private long totalUsers;

    // 총 대화 수
    private long totalMessages;

    // 누적 결제 수
    private long totalPayments;
}
