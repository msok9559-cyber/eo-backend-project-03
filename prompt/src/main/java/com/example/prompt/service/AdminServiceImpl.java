package com.example.prompt.service;

import com.example.prompt.domain.AdminEntity;
import com.example.prompt.domain.UserEntity;
import com.example.prompt.dto.admin.AdminDto;
import com.example.prompt.dto.admin.AdminUserDetailDto;
import com.example.prompt.dto.admin.AdminUserDto;
import com.example.prompt.dto.admin.DashboardDto;
import com.example.prompt.repository.*;
import com.example.prompt.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    /**
     * 관리자 로그인
     */
    @Override
    public AdminDto.LoginResponse login(AdminDto.LoginRequest request) {

        log.info("관리자 로그인 시도 - adminId = {}", request.getAdminId());

        AdminEntity admin = adminRepository.findByAdminId(request.getAdminId())
                .orElseThrow(() -> {
                    log.warn("관리자 로그인 실패 - 존재하지 않는 adminId = {}", request.getAdminId());
                    return new IllegalArgumentException("관리자 아이디가 존재하지 않습니다.");
                });

        if (!admin.getPassword().equals(request.getPassword())) {
            log.warn("관리자 로그인 실패 - 비밀번호 불일치, adminId = {}", request.getAdminId());
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        log.info("관리자 로그인 성공 - adminId = {}", admin.getAdminId());

        // 로그인 성공 후 토큰 생성
        String accessToken = jwtProvider.createAdminToken(admin.getAdminId());

        return new AdminDto.LoginResponse(
                accessToken,
                admin.getAdminId(),
                admin.getAdminName()
        );
    }

    /**
     * 현재 관리자 정보 조회
     */
    @Override
    public AdminDto.MeResponse getMe(String adminId) {

        log.info("관리자 정보 조회 요청 - adminId = {}", adminId);

        AdminEntity admin = adminRepository.findByAdminId(adminId)
                .orElseThrow(() -> {
                    log.warn("관리자 정보 조회 실패 - 존재하지 않는 adminId = {}", adminId);
                    return  new IllegalArgumentException("관리자 정보를 찾을 수 없습니다.");
                });

        log.info("관리자 정보 조회 성공 - adminId={}", admin.getAdminId());

        return new AdminDto.MeResponse(admin.getAdminId(), admin.getAdminName());
    }

    /**
     * 대시보드
     */
    @Override
    public DashboardDto getDashboard() {

        log.info("관리자 대시보드 조회 요청");

        long totalUsers = userRepository.count();
        long normalPlanUsers = userRepository.countByPlan_PlanName("NORMAL");
        long proPlanUsers = userRepository.countByPlan_PlanName("PRO");
        long maxPlanUsers = userRepository.countByPlan_PlanName("MAX");

        long activeUsers = userRepository.countByActiveTrue();
        long lockedUsers = userRepository.countByLockedTrue();
        long inactiveUsers = userRepository.countByActiveFalse();

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime startOfNextDay = LocalDate.now().plusDays(1).atStartOfDay();
        long todaySignups = userRepository.countByCreatedAtBetween(startOfDay, startOfNextDay);

        long totalChats = chatRoomRepository.count();
        long totalMessages = chatMessageRepository.count();


        DashboardDto dashboard = DashboardDto.builder()
                .totalUsers(totalUsers)
                .normalPlanUsers(normalPlanUsers)
                .proPlanUsers(proPlanUsers)
                .maxPlanUsers(maxPlanUsers)
                .todaySignups(todaySignups)
                .activeUsers(activeUsers)
                .lockedUsers(lockedUsers)
                .inactiveUsers(inactiveUsers)
                .totalChats(totalChats)
                .totalMessages(totalMessages)
                .totalImages(0)
                .totalFiles(0)
                .build();

        log.info("관리자 대시보드 조회 성공 - totalUsers={}, todaySignups={}", totalUsers, todaySignups);

        return dashboard;
    }

    /**
     * 관리자 회원 목록 조회
     */
    @Override
    public List<AdminUserDto> getUsers() {

        log.info("관리자 회원 목록 조회 요청");

        List<AdminUserDto> users = userRepository.findAll()
                .stream()
                .map(AdminUserDto::from)
                .toList();

        log.info("관리자 회원 목록 조회 성공 - 조회된 회원 수 = {}", users.size());

        return users;
    }

    /**
     * 관리자 회원 상세 조회
     */
    @Override
    public AdminUserDetailDto getUserDetail(Long userId) {

        log.info("관리자 회원 상세 조회 요청 - userId = {}", userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("관리자 회원 상세 조회 실패 - 존재하지 않는 userId = {}", userId);
                    return new IllegalArgumentException("해당 회원을 찾을 수 없습니다. userId=" + userId);
                });


        log.info("관리자 회원 상세 조회 성공 - userId = {}, userid = {}", user.getId(), user.getUserid());
        return AdminUserDetailDto.from(user);
    }
}
