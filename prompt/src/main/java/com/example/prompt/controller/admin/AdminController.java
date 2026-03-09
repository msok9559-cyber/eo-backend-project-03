package com.example.prompt.controller.admin;

import com.example.prompt.dto.admin.AdminDto;
import com.example.prompt.dto.admin.AdminUserDetailDto;
import com.example.prompt.dto.admin.AdminUserDto;
import com.example.prompt.dto.admin.DashboardDto;
import com.example.prompt.dto.common.ApiResponse;
import com.example.prompt.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ApiResponse<List<AdminUserDto>> getUsers() {
        return ApiResponse.ok(adminService.getUsers());
    }

    /**
     * 관리자 회원 상세 조회
     */
    @GetMapping("/users/{userId}")
    public ApiResponse<AdminUserDetailDto> getUserDetail(@PathVariable Long userId) {
        return ApiResponse.ok(adminService.getUserDetail(userId));
    }


}