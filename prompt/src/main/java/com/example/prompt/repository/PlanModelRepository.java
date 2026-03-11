package com.example.prompt.repository;

import com.example.prompt.domain.PlanModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanModelRepository extends JpaRepository<PlanModelEntity, Long> {

    // 특정 플랜에서 사용 가능한 모델 목록 조회
    List<PlanModelEntity> findByPlan_PlanId(Long planId);

    // 특정 플랜에서 해당 모델 사용 가능 여부 확인
    boolean existsByPlan_PlanIdAndModelName(Long planId, String modelName);
}