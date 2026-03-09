package com.example.prompt.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDto {

    // 전체 사용자 수
    private long totalUsers;

    // 플랜별 사용자 수
    private long normalPlanUsers;
    private long proPlanUsers;
    private long maxPlanUsers;

    // 오늘 가입자 수
    private long todaySignups;

    // 활성 / 잠금 / 비활성 상태
    private long activeUsers;
    private long lockedUsers;
    private long inactiveUsers;

    // 전체 채팅 수
    private long totalChats;

    // 전체 메시지 수
    private long totalMessages;

    // 전체 이미지 업로드 수
    private long totalImages;

    // 전체 파일 업로드 수
    private long totalFiles;
}