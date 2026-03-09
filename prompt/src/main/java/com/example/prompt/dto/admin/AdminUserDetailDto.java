package com.example.prompt.dto.admin;

import com.example.prompt.domain.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 관리자 페이지에서 회원 상세 조회 시 사용하는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailDto {
    private Long id;

    private String userid;

    private String username;

    private String email;

    private Long planId;

    private String planName;

    private Integer usedToken;

    /** 토큰 초기화 시각 */
    private LocalDateTime tokenResetAt;

    /** 계정 활성 상태 */
    private Boolean active;

    /** 계정 잠금 상태 */
    private Boolean locked;

    /** 회원 가입 일시 */
    private LocalDateTime createdAt;

    /** 회원 정보 수정 일시 */
    private LocalDateTime updatedAt;

    /**
     * UserEntity → AdminUserDetailDto 변환
     */
    public static AdminUserDetailDto from(UserEntity user) {
        return AdminUserDetailDto.builder()
                .id(user.getId())
                .userid(user.getUserid())
                .username(user.getUsername())
                .email(user.getEmail())
                .planId(user.getPlan().getPlanId())
                .planName(user.getPlan().getPlanName())
                .usedToken(user.getUsedToken())
                .tokenResetAt(user.getTokenResetAt())
                .active(user.isActive())
                .locked(user.isLocked())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
