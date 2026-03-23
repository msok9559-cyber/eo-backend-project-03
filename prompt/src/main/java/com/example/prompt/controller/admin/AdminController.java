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
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;


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
            @RequestParam(required = false, defaultValue = "") String plan,
            @RequestParam(required = false, defaultValue = "") String status,
            Pageable pageable
    ) {
        return ApiResponse.ok(adminService.searchUsers(keyword, plan, status, pageable));
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
        String adminId = (String) authentication.getName();
        adminService.lockUser(adminId, userId);
        return ApiResponse.ok(null);
    }

    /**
     * 회원 잠금 해제
     */
    @ResponseBody
    @PatchMapping("/api/users/{userId}/unlock")
    public ApiResponse<Void> unlockUser(Authentication authentication, @PathVariable Long userId) {
        String adminId = (String) authentication.getName();
        adminService.unlockUser(adminId, userId);
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
        String adminId = authentication.getName();
        adminService.changeUserPlan(adminId, userId, request);
        return ApiResponse.ok(null);
    }

    /**
     * 회원 상태 변경
     */
    @ResponseBody
    @PatchMapping("/api/users/{userId}/status")
    public ApiResponse<Void> changeUserStatus(
            Authentication authentication,
            @PathVariable Long userId,
            @Valid @RequestBody AdminDto.ChangeStatusRequest request
    ) {
        String adminId = authentication.getName();
        adminService.changeUserStatus(adminId, userId, request);
        return ApiResponse.ok(null);
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
                        "",
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
            @RequestParam(defaultValue = "") String plan,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        Sort sortOption = sort.equals("oldest")
                ? Sort.by(Sort.Direction.ASC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");

        Pageable pageable = PageRequest.of(page, 10, sortOption);

        Page<AdminUserDto> users = adminService.searchUsers(keyword, plan, status, pageable);

        model.addAttribute("users", users);
        model.addAttribute("keyword", keyword);
        model.addAttribute("plan", plan);
        model.addAttribute("status", status);
        model.addAttribute("sort", sort);

        return "admin/users";
    }
    /**
     * 관리자 처리 이력 페이지
     */
    @GetMapping("/logs")
    public String logs(
            @RequestParam(required = false, defaultValue = "") String adminId,
            @RequestParam(required = false, defaultValue = "") String actionType,
            @RequestParam(required = false, defaultValue = "") String startDate,
            @RequestParam(required = false, defaultValue = "") String endDate,
            @PageableDefault(size = 10) Pageable pageable,
            Model model
    ) {
        Page<AdminActionLogDto> logs =
                adminService.getAdminActionLogs(adminId, actionType, startDate, endDate, pageable);

        model.addAttribute("logs", logs);
        model.addAttribute("adminId", adminId);
        model.addAttribute("actionType", actionType);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "admin/logs";
    }

    /**
     * 관리자 플랜 / 정책 페이지
     */
    @GetMapping("/policies")
    public String policies(Model model) {
        model.addAttribute("plans", adminService.getPolicies());
        return "admin/policies";
    }

    /**
     * 관리자 플랜 / 정책 수정
     */
    @ResponseBody
    @PatchMapping("/api/plans/{planId}")
    public ApiResponse<Void> updatePolicy(
            Authentication authentication,
            @PathVariable Long planId,
            @Valid @RequestBody AdminPolicyUpdateRequest request
    ) {
        String adminId = authentication.getName();
        adminService.updatePolicy(adminId, planId, request);
        return ApiResponse.ok(null);
    }

    /**
     * 통계 페이지
     */
    @GetMapping("/stats")
    public String stats(
            @RequestParam(defaultValue = "daily") String periodType,
            @RequestParam(required = false, defaultValue = "") String startDate,
            @RequestParam(required = false, defaultValue = "") String endDate,
            @RequestParam(required = false, defaultValue = "") String planType,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        AdminStatsDto stats = adminService.getStats(periodType, startDate, endDate, planType, page);

        model.addAttribute("periodType", periodType);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("planType", planType);
        model.addAttribute("stats", stats);

        return "admin/stats";
    }
}