package com.example.prompt.service;

import com.example.prompt.domain.PlanEntity;
import com.example.prompt.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {

    private final PlanRepository planRepository;

    // 전체 플랜 조회
    public List<PlanEntity> getAllPlans() {
        return planRepository.findAll();
    }

    // 플랜 이름으로 단건 조회
    public PlanEntity getPlanByName(String planName) {
        return planRepository.findByPlanName(planName)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 플랜입니다: " + planName));
    }
}