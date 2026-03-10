package com.example.prompt.service;

import com.example.prompt.domain.AdminEntity;
import com.example.prompt.domain.PlanEntity;
import com.example.prompt.domain.UserEntity;
import com.example.prompt.dto.admin.AdminDto;
import com.example.prompt.dto.admin.DashboardDto;
import com.example.prompt.repository.*;
import com.example.prompt.security.jwt.JwtProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private AdminActionLogRepository adminActionLogRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    /**
     * 관리자 로그인 성공 테스트
     */
    @Test
    void login_success() {
        AdminDto.LoginRequest request = new AdminDto.LoginRequest("admin1", "1234");

        AdminEntity admin = AdminEntity.builder()
                .adminId("admin1")
                .adminName("관리자")
                .password("1234")
                .build();

        given(adminRepository.findByAdminId("admin1"))
                .willReturn(Optional.of(admin));
        given(jwtProvider.createAdminToken("admin1"))
                .willReturn("test-token");

        AdminDto.LoginResponse response = adminService.login(request);

        assertNotNull(response);
        assertEquals("test-token", response.getAccessToken());
        assertEquals("admin1", response.getAdminId());
        assertEquals("관리자", response.getName());
    }

    /**
     * 로그인 실패(관리자 아이디 없을 때)
     */
    @Test
    void login_fail_admin_notfound() {
        AdminDto.LoginRequest request = new AdminDto.LoginRequest("wrongAdmin", "1234");

        given(adminRepository.findByAdminId("wrongAdmin"))
                .willReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.login(request)
        );

        assertEquals("관리자 아이디가 존재하지 않습니다.", exception.getMessage());
    }

    /**
     * 로그인 실패(비밀번호 불일치)
     */
    @Test
    void login_fail_password_not_match() {
        AdminDto.LoginRequest request = new AdminDto.LoginRequest("admin1", "1111");

        AdminEntity admin = AdminEntity.builder()
                .adminId("admin1")
                .adminName("관리자")
                .password("1234")
                .build();

        given(adminRepository.findByAdminId("admin1"))
                .willReturn(Optional.of(admin));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.login(request)
        );

        assertEquals("비밀번호가 일치하지 않습니다.", exception.getMessage());
    }

    /**
     * 관리자 정보 조회 성공 테스트
     */
    @Test
    void getMe_success() {
        AdminEntity admin = AdminEntity.builder()
                .adminId("admin1")
                .adminName("관리자")
                .password("1234")
                .build();

        given(adminRepository.findByAdminId("admin1"))
                .willReturn(Optional.of(admin));

        AdminDto.MeResponse response = adminService.getMe("admin1");

        assertNotNull(response);
        assertEquals("admin1", response.getAdminId());
        assertEquals("관리자", response.getName());
    }

    /**
     * 관리자 정보 조회 실패 테스트
     */
    @Test
    void getMe_fail_admin_notfound() {
        given(adminRepository.findByAdminId("wrongAdmin"))
                .willReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.getMe("wrongAdmin")
        );

        assertEquals("관리자 정보를 찾을 수 없습니다.", exception.getMessage());
    }

    /**
     * 관리자 대시보드 조회 성공 테스트
     */
    @Test
    void getDashboard_success() {
        given(userRepository.count()).willReturn(10L);
        given(userRepository.countByPlan_PlanName("NORMAL")).willReturn(5L);
        given(userRepository.countByPlan_PlanName("PRO")).willReturn(3L);
        given(userRepository.countByPlan_PlanName("MAX")).willReturn(2L);

        given(userRepository.countByActiveTrue()).willReturn(8L);
        given(userRepository.countByLockedTrue()).willReturn(1L);
        given(userRepository.countByActiveFalse()).willReturn(2L);
        given(userRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(4L);

        given(chatRoomRepository.count()).willReturn(20L);
        given(chatMessageRepository.count()).willReturn(100L);

        DashboardDto result = adminService.getDashboard();

        assertThat(result.getTotalUsers()).isEqualTo(10L);
        assertThat(result.getNormalPlanUsers()).isEqualTo(5L);
        assertThat(result.getProPlanUsers()).isEqualTo(3L);
        assertThat(result.getMaxPlanUsers()).isEqualTo(2L);
        assertThat(result.getTotalChats()).isEqualTo(20L);
        assertThat(result.getTotalMessages()).isEqualTo(100L);
        assertThat(result.getTotalImages()).isEqualTo(0L);
        assertThat(result.getTotalFiles()).isEqualTo(0L);
        assertThat(result.getTodaySignups()).isEqualTo(4L);
        assertThat(result.getActiveUsers()).isEqualTo(8L);
        assertThat(result.getLockedUsers()).isEqualTo(1L);
        assertThat(result.getInactiveUsers()).isEqualTo(2L);
    }

    /**
     * 회원 플랜 변경 성공
     */
    @Test
    void changeUserPlan_success() {
        String adminId = "admin1";
        Long userId = 1L;
        AdminDto.ChangePlanRequest request = new AdminDto.ChangePlanRequest("PRO");

        PlanEntity beforePlan = PlanEntity.builder()
                .planId(1L)
                .planName("NORMAL")
                .tokenLimit(500)
                .aiUse(50)
                .price(0)
                .build();

        UserEntity user = UserEntity.builder()
                .id(userId)
                .userid("user1")
                .plan(beforePlan)
                .build();

        PlanEntity afterPlan = PlanEntity.builder()
                .planId(2L)
                .planName("PRO")
                .tokenLimit(1000)
                .aiUse(100)
                .price(19900)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(planRepository.findByPlanName("PRO")).willReturn(Optional.of(afterPlan));

        adminService.changeUserPlan(adminId, userId, request);

        assertEquals("PRO", user.getPlan().getPlanName());
    }

    /**
     * 회원 플랜 변경 실패 (회원 없음)
     */
    @Test
    void changeUserPlan_fail_userNotFound() {
        String adminId = "admin1";
        Long userId = 999L;
        AdminDto.ChangePlanRequest request = new AdminDto.ChangePlanRequest("PRO");

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.changeUserPlan(adminId, userId, request)
        );

        assertEquals("회원을 찾을 수 없습니다. id=" + userId, exception.getMessage());
    }

    /**
     * 회원 플랜 변경 실패 (플랜 없음)
     */
    @Test
    void changeUserPlan_fail_planNotFound() {
        String adminId = "admin1";
        Long userId = 1L;
        AdminDto.ChangePlanRequest request = new AdminDto.ChangePlanRequest("VIP");

        PlanEntity beforePlan = PlanEntity.builder()
                .planId(1L)
                .planName("NORMAL")
                .tokenLimit(500)
                .aiUse(50)
                .price(0)
                .build();

        UserEntity user = UserEntity.builder()
                .id(userId)
                .userid("user1")
                .plan(beforePlan)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(planRepository.findByPlanName("VIP")).willReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.changeUserPlan(adminId, userId, request)
        );

        assertEquals("존재하지 않는 플랜입니다. planName=VIP", exception.getMessage());
    }
}