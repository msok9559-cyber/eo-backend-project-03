package com.example.prompt.service;

import com.example.prompt.dto.stats.StatsDto;
import com.example.prompt.repository.ChatMessageRepository;
import com.example.prompt.repository.PaymentRepository;
import com.example.prompt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {

    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public StatsDto getStats() {
        long totalUsers = userRepository.count();
        long totalMessages = chatMessageRepository.count();
        long totalPayments = paymentRepository.count();

        log.info("통계 조회 - 가입자: {}, 대화수: {}, 결제수: {}", totalUsers, totalMessages, totalPayments);

        return StatsDto.builder()
                .totalUsers(totalUsers)
                .totalMessages(totalMessages)
                .totalPayments(totalPayments)
                .build();
    }

}
