package com.example.prompt.repository;

import com.example.prompt.domain.UserEntity;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

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
}
