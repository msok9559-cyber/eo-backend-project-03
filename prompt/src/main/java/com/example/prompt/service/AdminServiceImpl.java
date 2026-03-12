package com.example.prompt.service;

import com.example.prompt.domain.AdminActionLogEntity;
import com.example.prompt.domain.AdminEntity;
import com.example.prompt.domain.PlanEntity;
import com.example.prompt.domain.UserEntity;
import com.example.prompt.dto.admin.*;
import com.example.prompt.repository.*;
import com.example.prompt.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PlanRepository planRepository;
    private final AdminActionLogRepository adminActionLogRepository;

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

    @Transactional
    @Override
    public void lockUser(String adminId, Long userId) {

        log.info("회원 잠금 요청 - adminId={}, userId={}", adminId, userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("회원 잠금 실패 - 존재하지 않는 userId={}", userId);
                    return new IllegalArgumentException("회원을 찾을 수 없습니다. id=" + userId);
                });

        user.setLocked(true);

        saveAdminActionLog(adminId, userId, "LOCK", "회원 계정 잠금");

        log.info("회원 잠금 처리 완료 - adminId={}, userId={}", adminId, userId);
    }

    /**
     * 회원 잠금 해제
     */
    @Transactional
    @Override
    public void unlockUser(String adminId, Long userId) {

        log.info("회원 잠금 해제 요청 - adminId={}, userId={}", adminId, userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("회원 잠금 해제 실패 - 존재하지 않는 userId={}", userId);
                    return new IllegalArgumentException("회원을 찾을 수 없습니다. id=" + userId);
                });

        user.setLocked(false);

        saveAdminActionLog(adminId, userId, "UNLOCK", "회원 계정 잠금 해제");

        log.info("회원 잠금 해제 완료 - adminId={}, userId={}", adminId, userId);
    }

    /**
     * 회원 활성 처리
     */
    @Transactional
    @Override
    public void activateUser(String adminId, Long userId) {

        log.info("회원 활성 요청 - adminId={}, userId={}", adminId, userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("회원 활성 실패 - 존재하지 않는 userId={}", userId);
                    return new IllegalArgumentException("회원을 찾을 수 없습니다. id=" + userId);
                });

        user.setActive(true);

        saveAdminActionLog(adminId, userId, "ACTIVATE", "회원 계정 활성화");

        log.info("회원 활성 처리 완료 - adminId={}, userId={}", adminId, userId);
    }

    /**
     * 회원 비활성 처리
     */
    @Transactional
    @Override
    public void deactivateUser(String adminId, Long userId) {

        log.info("회원 비활성 요청 - adminId={}, userId={}", adminId, userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("회원 비활성 실패 - 존재하지 않는 userId={}", userId);
                    return new IllegalArgumentException("회원을 찾을 수 없습니다. id=" + userId);
                });

        user.setActive(false);

        saveAdminActionLog(adminId, userId, "DEACTIVATE", "회원 계정 비활성화");

        log.info("회원 비활성 처리 완료 - adminId={}, userId={}", adminId, userId);
    }

    /**
     * 관리자 회원 검색 + 페이징 조회
     */
    @Override
    public Page<AdminUserDto> searchUsers(String keyword, Pageable pageable) {

        log.info("관리자 회원 검색 + 페이징 조회 요청 - keyword={}, page={}, size={}",
                keyword, pageable.getPageNumber(), pageable.getPageSize());

        Page<UserEntity> users;

        if (keyword == null || keyword.isBlank()) {
            users = userRepository.findAll(pageable);
        } else {
            users = userRepository.findByUseridContainingOrEmailContaining(
                    keyword, keyword, pageable
            );
        }

        log.info("관리자 회원 검색 + 페이징 조회 성공 - totalElements={}",
                users.getTotalElements());

        return users.map(AdminUserDto::from);
    }

    /**
     * 회원 플랜 변경
     */
    @Transactional
    @Override
    public void changeUserPlan(String adminId, Long userId, AdminDto.ChangePlanRequest request) {

        log.info("회원 플랜 변경 요청 - adminId={}, userId={}, planName={}",
                adminId, userId, request.getPlanName());

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("회원 플랜 변경 실패 - 존재하지 않는 userId={}", userId);
                    return new IllegalArgumentException("회원을 찾을 수 없습니다. id=" + userId);
                });

        String beforePlan = user.getPlan().getPlanName();

        PlanEntity plan = planRepository.findByPlanName(request.getPlanName())
                .orElseThrow(() -> {
                    log.warn("회원 플랜 변경 실패 - 존재하지 않는 planName={}", request.getPlanName());
                    return new IllegalArgumentException("존재하지 않는 플랜입니다. planName=" + request.getPlanName());
                });

        user.setPlan(plan);

        saveAdminActionLog(
                adminId,
                userId,
                "CHANGE_PLAN",
                "플랜 변경: " + beforePlan + " -> " + plan.getPlanName()
        );

        log.info("회원 플랜 변경 완료 - adminId={}, userId={}, changedPlan={}",
                adminId, userId, plan.getPlanName());
    }

    /**
     * 관리자 처리 이력 저장
     */
    private void saveAdminActionLog(String adminId, Long targetUserId, String actionType, String description) {
        AdminActionLogEntity log = AdminActionLogEntity.builder()
                .adminId(adminId)
                .targetUserId(targetUserId)
                .actionType(actionType)
                .description(description)
                .build();

        adminActionLogRepository.save(log);
    }

    /**
     * 관리자 처리 이력 조회
     */
    @Override
    public Page<AdminActionLogDto> getAdminActionLogs(Pageable pageable) {

        log.info("관리자 처리 이력 조회 요청 - page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<AdminActionLogEntity> logs = adminActionLogRepository.findAllByOrderByCreatedAtDesc(pageable);

        log.info("관리자 처리 이력 조회 성공 - totalElements={}", logs.getTotalElements());

        return logs.map(AdminActionLogDto::from);
    }
}
