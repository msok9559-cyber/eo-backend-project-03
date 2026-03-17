package com.example.prompt.repository;

import com.example.prompt.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUserid(String userid);

    Optional<UserEntity> findByEmail(String email);

    boolean existsByUserid(String userid);

    boolean existsByEmail(String email);

    Optional<UserEntity> findByProviderAndProviderId(String provider, String providerId);

    // 플랜
    long countByPlan_PlanName(String planName);

    // 활성 유저 수
    long countByActiveTrue();

    // 잠긴 계정 수
    long countByLockedTrue();

    // 비활성 계정 수
    long countByActiveFalse();

    // 오늘 가입자 수
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // 검색 기능 + 페이징 처리
    Page<UserEntity> findByUseridContainingOrEmailContaining(
            String userid,
            String email,
            Pageable pageable
    );

    // 토큰 초기화 스케줄러용: 활성 유저 전체 조회
    List<UserEntity> findByActiveTrue();
}