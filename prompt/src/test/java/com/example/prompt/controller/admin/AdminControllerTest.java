package com.example.prompt.controller.admin;

import com.example.prompt.dto.admin.AdminDto;
import com.example.prompt.dto.admin.AdminUserDetailDto;
import com.example.prompt.dto.admin.AdminUserDto;
import com.example.prompt.dto.admin.DashboardDto;
import com.example.prompt.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminService adminService;

    /**
     * 관리자 로그인 성공 테스트
     */
    @Test
    void login_success() throws Exception {
        AdminDto.LoginRequest request = new AdminDto.LoginRequest("admin1", "1234");
        AdminDto.LoginResponse response = new AdminDto.LoginResponse("test-token", "admin1", "관리자");

        given(adminService.login(any(AdminDto.LoginRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("test-token"))
                .andExpect(jsonPath("$.data.adminId").value("admin1"))
                .andExpect(jsonPath("$.data.name").value("관리자"));
    }

    /**
     * 관리자 로그아웃 성공 테스트
     */
    @Test
    void logout_success() throws Exception {
        mockMvc.perform(post("/api/admin/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    /**
     * 현재 관리자 정보 조회 성공 테스트
     */
    @Test
    void me_success() throws Exception {
        AdminDto.MeResponse response = new AdminDto.MeResponse("admin1", "관리자");

        given(adminService.getMe("admin1")).willReturn(response);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("admin1", null);

        mockMvc.perform(get("/api/admin/auth/me")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.adminId").value("admin1"))
                .andExpect(jsonPath("$.data.name").value("관리자"));
    }

    /**
     * 관리자 대시보드 조회 성공 테스트
     */
    @Test
    void dashboard_success() throws Exception {
        DashboardDto response = DashboardDto.builder()
                .totalUsers(10)
                .normalPlanUsers(5)
                .proPlanUsers(3)
                .maxPlanUsers(2)
                .todaySignups(4)
                .activeUsers(8)
                .lockedUsers(1)
                .inactiveUsers(2)
                .totalChats(20)
                .totalMessages(100)
                .totalImages(0)
                .totalFiles(0)
                .build();

        given(adminService.getDashboard()).willReturn(response);

        mockMvc.perform(get("/api/admin/auth/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").value(10))
                .andExpect(jsonPath("$.data.normalPlanUsers").value(5))
                .andExpect(jsonPath("$.data.proPlanUsers").value(3))
                .andExpect(jsonPath("$.data.maxPlanUsers").value(2))
                .andExpect(jsonPath("$.data.totalChats").value(20))
                .andExpect(jsonPath("$.data.totalMessages").value(100))
                .andExpect(jsonPath("$.data.totalImages").value(0))
                .andExpect(jsonPath("$.data.totalFiles").value(0))
                .andExpect(jsonPath("$.data.todaySignups").value(4))
                .andExpect(jsonPath("$.data.activeUsers").value(8))
                .andExpect(jsonPath("$.data.lockedUsers").value(1))
                .andExpect(jsonPath("$.data.inactiveUsers").value(2));
    }

    /**
     * 관리자 회원 상세 조회 테스트
     */
    @Test
    void get_user_detail_success() throws Exception {

        // 관리자 서비스가 반환할 회원 상세 Mock 데이터 생성
        AdminUserDetailDto userDetail = AdminUserDetailDto.builder()
                .id(1L)
                .userid("user1")
                .username("김땡땡")
                .email("user1@test.com")
                .planId(2L)
                .planName("PRO")
                .usedToken(120)
                .active(true)
                .locked(false)
                .build();

        given(adminService.getUserDetail(1L)).willReturn(userDetail);

        // 관리자 회원 상세 조회 API 호출 후 응답 검증
        mockMvc.perform(get("/api/admin/auth/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.userid").value("user1"))
                .andExpect(jsonPath("$.data.username").value("김땡땡"))
                .andExpect(jsonPath("$.data.email").value("user1@test.com"))
                .andExpect(jsonPath("$.data.planId").value(2))
                .andExpect(jsonPath("$.data.planName").value("PRO"))
                .andExpect(jsonPath("$.data.usedToken").value(120))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.locked").value(false));
    }

    /**
     * 관리자 회원 잠금 처리 테스트
     *
     */
    @Test
    void lock_user_success() throws Exception {
        mockMvc.perform(patch("/api/admin/auth/users/1/lock"))
                .andExpect(status().isOk());
    }

    /**
     * 관리자 회원 잠금 해제 테스트
     */
    @Test
    void unlock_user_success() throws Exception {
        mockMvc.perform(patch("/api/admin/auth/users/1/unlock"))
                .andExpect(status().isOk());
    }

    /**
     * 관리자 회원 활성 처리 테스트
     */
    @Test
    void activate_user_success() throws Exception {
        mockMvc.perform(patch("/api/admin/auth/users/1/activate"))
                .andExpect(status().isOk());
    }

    /**
     * 관리자 회원 비활성 처리 테스트
     */
    @Test
    void deactivate_user_success() throws Exception {
        mockMvc.perform(patch("/api/admin/auth/users/1/deactivate"))
                .andExpect(status().isOk());
    }
}