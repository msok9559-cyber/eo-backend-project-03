package com.example.prompt.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "plan_models")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanModelEntity {

    // PK (ERD: model_id)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "model_id")
    private Long modelId;

    // 플랜 FK (plans.plan_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEntity plan;

    // AI 모델명 (예: alan-4.0, alan-4.1)
    @Column(name = "model_name", nullable = false, length = 50)
    private String modelName;

    // 정적 팩토리 메서드
    public static PlanModelEntity of(PlanEntity plan, String modelName) {
        return PlanModelEntity.builder()
                .plan(plan)
                .modelName(modelName)
                .build();
    }
}