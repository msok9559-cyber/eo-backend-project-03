package com.example.prompt.controller.admin;

import com.example.prompt.dto.admin.*;
import com.example.prompt.dto.common.ApiResponse;
import com.example.prompt.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자 API Controller
 * 관리자 인증 담당
 */
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 관리자 로그인
     */
    @PostMapping("/login")
    public ApiResponse<AdminDto.LoginResponse> login(
            @Valid @RequestBody AdminDto.LoginRequest request
    ) {
        return ApiResponse.ok(adminService.login(request));
    }

    /**
     * 관리자 로그아웃
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok(null);
    }

    /**
     * 현재 로그인된 관리자 정보
     */
    @GetMapping("/me")
    public ApiResponse<AdminDto.MeResponse> me(Authentication authentication) {
        String adminId = (String) authentication.getPrincipal();
        return ApiResponse.ok(adminService.getMe(adminId));
    }

    /**
     * 대시보드
     */
    @GetMapping("/dashboard")
    public ApiResponse<DashboardDto> getDashboard() {
        return ApiResponse.ok(adminService.getDashboard());
    }

    /**
     * 관리자 회원 목록 조회
     */
    @GetMapping("/users")
    public ApiResponse<Page<AdminUserDto>> getUsers(
            @RequestParam(required = false, defaultValue = "") String keyword,
            Pageable pageable
    ) {
        return ApiResponse.ok(adminService.searchUsers(keyword, pageable));
    }

    /**
     * 관리자 회원 상세 조회
     */
    @GetMapping("/users/{userId}")
    public ApiResponse<AdminUserDetailDto> getUserDetail(@PathVariable Long userId) {
        return ApiResponse.ok(adminService.getUserDetail(userId));
    }

    /**
     * 회원 잠금 (계정 잠금)
     */
    @PatchMapping("/users/{userId}/lock")
    public ApiResponse<Void> lockUser(Authentication authentication, @PathVariable Long userId) {
        String adminId = (String) authentication.getPrincipal();
        adminService.lockUser(adminId, userId);
        return ApiResponse.ok(null);
    }

    /**
     * 회원 잠금 해제
     */
    @PatchMapping("/users/{userId}/unlock")
    public ApiResponse<Void> unlockUser(Authentication authentication, @PathVariable Long userId) {
        String adminId = (String) authentication.getPrincipal();
        adminService.unlockUser(adminId, userId);
        return ApiResponse.ok(null);
    }

    /**
     * 회원 활성화
     */
    @PatchMapping("/users/{userId}/activate")
    public ApiResponse<Void> activateUser(Authentication authentication, @PathVariable Long userId) {
        String adminId = (String) authentication.getPrincipal();
        adminService.activateUser(adminId, userId);
        return ApiResponse.ok(null);
    }

    /**
     * 회원 비활성화 (탈퇴 처리)
     */
    @PatchMapping("/users/{userId}/deactivate")
    public ApiResponse<Void> deactivateUser(Authentication authentication, @PathVariable Long userId) {
        String adminId = (String) authentication.getPrincipal();
        adminService.deactivateUser(adminId, userId);
        return ApiResponse.ok(null);
    }

    /**
     * 회원 플랜 변경
     */
    @PatchMapping("/users/{userId}/plan")
    public ApiResponse<Void> changeUserPlan(
            Authentication authentication,
            @PathVariable Long userId,
            @Valid @RequestBody AdminDto.ChangePlanRequest request
    ) {
        String adminId = (String) authentication.getPrincipal();
        adminService.changeUserPlan(adminId, userId, request);
        return ApiResponse.ok(null);
    }

    /**
     * 관리자 처리 이력 조회
     */
    @GetMapping("/logs")
    public ApiResponse<Page<AdminActionLogDto>> getAdminActionLogs(Pageable pageable) {
        return ApiResponse.ok(adminService.getAdminActionLogs(pageable));
    }
}