package com.example.prompt.scheduler;

import com.example.prompt.domain.UserEntity;
import com.example.prompt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 유저별 토큰 초기화 스케줄러
 * 매일 자정에 실행하여 오늘 초기화 대상인 유저만 선별해서 리셋
 *
 * 초기화 기준:
 * - 최초 가입 후: 가입일(createdAt) 기준 30일 후
 * - 이후: 마지막 초기화일(tokenResetAt) 기준 30일 후
 * - PRO / MAX 결제 시: 결제일 기준 30일이므로 planExpiredAt 주기와 자연스럽게 맞춰짐
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenResetScheduler {

    private final UserRepository userRepository;

    /**
     * 매일 00:00 실행
     * 당일 기준으로 토큰 초기화가 필요한 유저만 골라서 리셋
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void resetTokensForEligibleUsers() {
        final LocalDateTime now = LocalDateTime.now();
        log.info("유저별 토큰 초기화 스케줄러 실행 - {}", now);

        List<UserEntity> allActiveUsers = userRepository.findByActiveTrue();
        int resetCount = 0;

        for (UserEntity user : allActiveUsers) {
            if (isResetDue(user, now)) {
                user.setUsedToken(0);
                user.setTokenResetAt(now);
                resetCount++;
                log.info("토큰 초기화 - userId = {}, userid = {}, plan = {}",
                        user.getId(), user.getUserid(), user.getPlan().getPlanName());
            }
        }

        log.info("토큰 초기화 완료 - 초기화 유저: {} / 전체 활성 유저: {}", resetCount, allActiveUsers.size());
    }

    /**
     * 오늘이 해당 유저의 토큰 초기화 기준일인지 확인
     *
     * 기준일:
     * - tokenResetAt 있으면 → 마지막 초기화일 + 30일
     * - tokenResetAt 없으면 → 가입일(createdAt) + 30일
     */
    private boolean isResetDue(UserEntity user, LocalDateTime now) {
        LocalDateTime baseDate = user.getTokenResetAt() != null
                ? user.getTokenResetAt()
                : user.getCreatedAt();

        LocalDateTime nextResetDate = baseDate.plusDays(30);

        // 다음 초기화 예정일이 오늘 이전이면 초기화 대상
        return !now.isBefore(nextResetDate);
    }
}