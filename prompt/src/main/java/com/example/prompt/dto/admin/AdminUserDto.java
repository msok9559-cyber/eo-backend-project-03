package com.example.prompt.dto.admin;

import com.example.prompt.domain.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 관리자 페이지에서 회원 목록 조회 시 사용하는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {

    private Long id;
    private String userid;
    private String username;
    private String email;
    private String planName;

    //계정 활성 상태(탈퇴 시 false)
    private Boolean active;

    // 계정 잠금 상태 (잠금 처리한 경우 true)
    private Boolean locked;

    private LocalDateTime createdAt;

    /**
     * USerEntity -> AdminUserDtop 변환
     */
    public static AdminUserDto from(UserEntity user) {
        return AdminUserDto.builder()
                .id(user.getId())
                .userid(user.getUserid())
                .username(user.getUsername())
                .email(user.getEmail())
                .planName(user.getPlan().getPlanName())
                .active(user.isActive())
                .locked(user.isLocked())
                .createdAt(user.getCreatedAt())
                .build();
    }
}