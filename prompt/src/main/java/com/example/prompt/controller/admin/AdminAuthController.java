package com.example.prompt.controller.admin;

import com.example.prompt.dto.admin.AdminDto;
import com.example.prompt.dto.common.ApiResponse;
import com.example.prompt.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 API Controller
 * 관리자 인증 담당
 */
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

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
}
