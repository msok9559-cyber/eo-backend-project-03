package com.example.prompt.controller.admin;

import com.example.prompt.dto.admin.*;
import com.example.prompt.dto.common.ApiResponse;
import com.example.prompt.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 대시보드
     */
    @ResponseBody
    @GetMapping("/api/dashboard")
    public ApiResponse<DashboardDto> getDashboard() {
        return ApiResponse.ok(adminService.getDashboard());
    }

    /**
     * 관리자 회원 목록 조회
     */
    @ResponseBody
    @GetMapping("/api/users")
    public ApiResponse<Page<AdminUserDto>> getUsers(
            @RequestParam(required = false, defaultValue = "") String keyword,
            Pageable pageable
    ) {
        return ApiResponse.ok(adminService.searchUsers(keyword, pageable));
    }

    /**
     * 관리자 회원 상세 조회
     */
    @ResponseBody
    @GetMapping("/api/users/{userId}")
    public ApiResponse<AdminUserDetailDto> getUserDetail(@PathVariable Long userId) {
        return ApiResponse.ok(adminService.getUserDetail(userId));
    }

    /**
     * 회원 잠금 (계정 잠금)
     */
    @ResponseBody
    @PatchMapping("/api/users/{userId}/lock")
    public ApiResponse<Void> lockUser(Authentication authentication, @PathVariable Long userId) {
        String adminId = (String) authentication.getPrincipal();
        adminService.lockUser(adminId, userId);
        return ApiResponse.ok(null);
    }

    /**
     * 회원 잠금 해제
     */
    @ResponseBody
    @PatchMapping("/api/users/{userId}/unlock")
    public ApiResponse<Void> unlockUser(Authentication authentication, @PathVariable Long userId) {
        String adminId = (String) authentication.getPrincipal();
        adminService.unlockUser(adminId, userId);
        return ApiResponse.ok(null);
    }

    /**
     * 회원 활성화
     */
    @ResponseBody
    @PatchMapping("/api/users/{userId}/activate")
    public ApiResponse<Void> activateUser(Authentication authentication, @PathVariable Long userId) {
        String adminId = (String) authentication.getPrincipal();
        adminService.activateUser(adminId, userId);
        return ApiResponse.ok(null);
    }

    /**
     * 회원 비활성화 (탈퇴 처리)
     */
    @ResponseBody
    @PatchMapping("/api/users/{userId}/deactivate")
    public ApiResponse<Void> deactivateUser(Authentication authentication, @PathVariable Long userId) {
        String adminId = (String) authentication.getPrincipal();
        adminService.deactivateUser(adminId, userId);
        return ApiResponse.ok(null);
    }

    /**
     * 회원 플랜 변경
     */
    @ResponseBody
    @PatchMapping("/api/users/{userId}/plan")
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
    @ResponseBody
    @GetMapping("/api/logs")
    public ApiResponse<Page<AdminActionLogDto>> getAdminActionLogs(Pageable pageable) {
        return ApiResponse.ok(adminService.getAdminActionLogs(pageable));
    }

    /**
     * 관리자 대시보드 페이지
     */
    @GetMapping
    public String dashboard(Model model) {
        DashboardDto dashboard = adminService.getDashboard();

        model.addAttribute("dashboard", dashboard);
        model.addAttribute(
                "recentUsers",
                adminService.searchUsers(
                        "",
                        PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
                ).getContent()
        );

        return "admin/dashboard";
    }

    /**
     * 관리자 로그인 페이지
     */
    @GetMapping("/login")
    public String loginPage() {
        return "admin/admin-login";
    }

    /**
     * 관리자 사용자 관리 페이지
     */
    @GetMapping("/users")
    public String users(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminUserDto> users = adminService.searchUsers(keyword, pageable);

        model.addAttribute("users", users);
        model.addAttribute("keyword", keyword);

        return "admin/users";
    }

    /**
     * 관리자 처리 이력 페이지
     */
    @GetMapping("/logs")
    public String logs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "") String adminId,
            @RequestParam(required = false, defaultValue = "") String actionType,
            Model model
    ) {

        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<AdminActionLogDto> logs = adminService.getAdminActionLogs(pageable);

        model.addAttribute("logs", logs);
        model.addAttribute("adminId", adminId);
        model.addAttribute("actionType", actionType);

        return "admin/logs";
    }

}