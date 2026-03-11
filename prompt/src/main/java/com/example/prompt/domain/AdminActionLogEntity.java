package com.example.prompt.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "admin_action_logs")
public class AdminActionLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    /**
     * 작업한 관리자 아이디
     */
    @Column(name = "admin_id", nullable = false, length = 100)
    private String adminId;

    /**
     * 대상 회원 ID
     */
    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    /**
     * 처리 유형
     * 예: LOCK, UNLOCK, ACTIVATE, DEACTIVATE, CHANGE_PLAN
     */
    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    /**
     * 처리 상세 내용
     */
    @Column(name = "description", length = 255)
    private String description;

    /**
     * 처리 시각
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
