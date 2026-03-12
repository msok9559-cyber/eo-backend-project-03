package com.example.prompt.dto.user;

import com.example.prompt.domain.PlanEntity;
import com.example.prompt.domain.UserEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {

    private Long id;
    // 플랜 (ERD: plan_id FK)
    private Long planId;
    private String planName;

    @NotBlank(message = "아이디를 입력해주세요")
    @Size(min = 4, max = 50, message = "아이디는 4~50자 이내로 입력해주세요")
    private String userid;

    @NotBlank(message = "이름을 입력해주세요")
    @Size(max = 50)
    private String username;

    @NotBlank(message = "비밀번호를 입력해주세요")
    @Size(min = 8, message = "비밀번호는 8자 이상 입력해주세요")
    private String password;

    // 현재 비밀번호 (비밀번호 변경 시 사용)
    private String currentPassword;

    // 비밀번호 확인 (회원가입/비번재설정 시 사용)
    private String passwordConfirm;

    @NotBlank(message = "이메일을 입력해주세요")
    @Email(message = "이메일 형식을 다시 확인해주세요")
    private String email;

    // 사용 토큰량 (ERD: used_token)
    @Builder.Default
    private Integer usedToken = 0;

    // 토큰 초기화 일자 (ERD: token_reset_at)
    private LocalDateTime tokenResetAt;

    // 활성여부 - 탈퇴 시 false (ERD: active)
    @Builder.Default
    private Boolean active = true;

    // 계정 잠금 - 관리자 잠금 처리 (ERD: locked)
    @Builder.Default
    private Boolean locked = false;

    // 약관 동의 (회원가입 시 사용)
    private Boolean agree;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity → DTO 변환
     */
    public static UserDto from(UserEntity userEntity) {
        if (userEntity == null) {
            throw new IllegalArgumentException("UserEntity cannot be null");
        }

        PlanEntity plan = userEntity.getPlan();

        return UserDto.builder()
                .id(userEntity.getId())
                .planId(plan != null ? plan.getPlanId() : null)
                .planName(plan != null ? plan.getPlanName() : null)
                .userid(userEntity.getUserid())
                .username(userEntity.getUsername())
                .password(userEntity.getPassword())
                .email(userEntity.getEmail())
                .usedToken(userEntity.getUsedToken())
                .tokenResetAt(userEntity.getTokenResetAt())
                .active(userEntity.isActive())
                .locked(userEntity.isLocked())
                .createdAt(userEntity.getCreatedAt())
                .updatedAt(userEntity.getUpdatedAt())
                .build();
    }
}