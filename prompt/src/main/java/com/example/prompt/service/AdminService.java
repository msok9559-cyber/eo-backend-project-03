package com.example.prompt.service;

import com.example.prompt.dto.admin.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminService {

    /**
     * 관리자 로그인
     */
    AdminDto.LoginResponse login(AdminDto.LoginRequest request);

    /**
     * 현재 관리자 정보 조회
     */
    AdminDto.MeResponse getMe(String adminId);

    /**
     * 대시보드
     */
    DashboardDto getDashboard();

    /**
     * 관리자 회원 상세 조회
     */
    AdminUserDetailDto getUserDetail(Long userId);

    /**
     * 회원 잠금 처리
     */
    void lockUser(String adminId, Long userId);

    /**
     * 회원 잠금 해제 처리
     */
    void unlockUser(String adminId, Long userId);

    /**
     * 회원 활성 처리
     */
    void activateUser(String adminId, Long userId);

    /**
     * 회원 비활성 처리
     */
    void deactivateUser(String adminId, Long userId);;

    /**
     * 관리자 회원 검색 / 페이징
     */
    Page<AdminUserDto> searchUsers(String keyword, Pageable pageable);

    /**
     * 회원 플랜 변경
     */
    void changeUserPlan(String adminId, Long userId, AdminDto.ChangePlanRequest request);

    /**
     * 관리자 처리 이력
     */
    Page<AdminActionLogDto> getAdminActionLogs(Pageable pageable);
}
