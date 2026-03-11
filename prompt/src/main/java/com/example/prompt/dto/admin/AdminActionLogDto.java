package com.example.prompt.dto.admin;

import com.example.prompt.domain.AdminActionLogEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminActionLogDto {

    private Long logId;
    private String adminId;
    private Long targetUserId;
    private String actionType;
    private String description;
    private LocalDateTime createdAt;

    public static AdminActionLogDto from(AdminActionLogEntity log) {
        return AdminActionLogDto.builder()
                .logId(log.getLogId())
                .adminId(log.getAdminId())
                .targetUserId(log.getTargetUserId())
                .actionType(log.getActionType())
                .description(log.getDescription())
                .createdAt(log.getCreatedAt())
                .build();
    }

}
