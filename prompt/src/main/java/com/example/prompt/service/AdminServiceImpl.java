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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.prompt.dto.common.enums.AdminUserActionType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
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
    private final PlanRepository planRepository;
    private final AdminActionLogRepository adminActionLogRepository;
    private final PasswordEncoder passwordEncoder;

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

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
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
        long withdrawnUsers = userRepository.countByActiveFalse();

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
                .inactiveUsers(withdrawnUsers)
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
    @Transactional(readOnly = true)
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
     * 관리자 회원 검색 + 페이징 조회
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserDto> searchUsers(String keyword, String plan, String status, Pageable pageable) {

        log.info("관리자 회원 검색 + 페이징 조회 요청 - keyword={}, plan={}, status={}, page={}, size={}",
                keyword, plan, status, pageable.getPageNumber(), pageable.getPageSize());

        Page<UserEntity> users = userRepository.searchUsers(
                keyword == null ? "" : keyword,
                plan == null ? "" : plan,
                status == null ? "" : status,
                pageable
        );

        log.info("관리자 회원 검색 + 페이징 조회 성공 - totalElements={}", users.getTotalElements());

        return users.map(AdminUserDto::from);
    }

    /**
     * 회원 플랜 변경
     */
    @Transactional
    @Override
    public void changeUserPlan(String adminId, Long userId, AdminDto.ChangePlanRequest request) {

        log.info("회원 플랜 변경 요청 - adminId={}, userId={}, rawPlanName={}",
                adminId, userId, request.getPlanName());

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("회원 플랜 변경 실패 - 존재하지 않는 userId={}", userId);
                    return new IllegalArgumentException("회원을 찾을 수 없습니다. id=" + userId);
                });

        log.info("회원 조회 성공 - userId={}, currentPlan={}",
                user.getId(),
                user.getPlan() != null ? user.getPlan().getPlanName() : "없음");

        String beforePlan = user.getPlan() != null ? user.getPlan().getPlanName() : "없음";

        String planName = request.getPlanName() != null
                ? request.getPlanName().trim().toUpperCase()
                : null;

        log.info("정규화된 planName={}", planName);

        PlanEntity plan = planRepository.findByPlanName(planName)
                .orElseThrow(() -> {
                    log.warn("회원 플랜 변경 실패 - 존재하지 않는 planName={}", planName);
                    return new IllegalArgumentException("존재하지 않는 플랜입니다. planName=" + planName);
                });

        log.info("플랜 조회 성공 - foundPlan={}", plan.getPlanName());

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
     * 회원 상태 변경
     */
    @Override
    @Transactional
    public void changeUserStatus(String adminId, Long userId, AdminDto.ChangeStatusRequest request) {

        log.info("회원 상태 변경 요청 - adminId={}, userId={}, action={}",
                adminId, userId, request.getAction());

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("회원 상태 변경 실패 - 존재하지 않는 userId={}", userId);
                    return new IllegalArgumentException("회원을 찾을 수 없습니다.");
                });

        AdminUserActionType action = request.getAction();

        switch (action) {
            case LOCK -> user.setLocked(true);

            case UNLOCK -> user.setLocked(false);

            case RESTORE -> {
                user.setActive(true);
                user.setLocked(false);
            }

            case WITHDRAW -> {
                user.setActive(false);
                user.setLocked(false);
            }
        }

        user.setUpdatedAt(LocalDateTime.now());

        adminActionLogRepository.save(
                AdminActionLogEntity.builder()
                        .adminId(adminId)
                        .targetUserId(userId)
                        .actionType(action.name())
                        .description("회원 상태 변경 - " + action.name())
                        .build()
        );

        log.info("회원 상태 변경 완료 - adminId={}, userId={}, action={}",
                adminId, userId, action);
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
    public Page<AdminActionLogDto> getAdminActionLogs(String adminId,
                                                      String actionType,
                                                      String startDate,
                                                      String endDate,
                                                      Pageable pageable) {

        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        if (startDate != null && !startDate.isBlank()) {
            startDateTime = LocalDate.parse(startDate).atStartOfDay();
        }

        if (endDate != null && !endDate.isBlank()) {
            endDateTime = LocalDate.parse(endDate).atTime(LocalTime.MAX);
        }

        Page<AdminActionLogEntity> logs = adminActionLogRepository.searchLogs(
                adminId,
                actionType,
                startDateTime,
                endDateTime,
                pageable
        );

        return logs.map(AdminActionLogDto::from);
    }


    /**
     * 관리자 정책 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<AdminPolicyDto> getPolicies() {

        log.info("관리자 정책 목록 조회 요청");

        List<AdminPolicyDto> policies = planRepository.findAllByOrderByPlanIdAsc()
                .stream()
                .map(AdminPolicyDto::from)
                .toList();

        log.info("관리자 정책 목록 조회 성공 - count={}", policies.size());

        return policies;
    }

    /**
     * 관리자 플랜 정책 수정
     */
    @Transactional
    @Override
    public void updatePolicy(String adminId, Long planId, AdminPolicyUpdateRequest request) {

        log.info("관리자 플랜 정책 수정 요청 - adminId={}, planId={}", adminId, planId);

        PlanEntity plan = planRepository.findById(planId)
                .orElseThrow(() -> {
                    log.warn("플랜 정책 수정 실패 - 존재하지 않는 planId={}", planId);
                    return new IllegalArgumentException("플랜을 찾을 수 없습니다. id=" + planId);
                });

        plan.setDailyChatLimit(request.getDailyChatLimit());
        plan.setImageUploadLimit(request.getImageUploadLimit());
        plan.setFileUploadLimit(request.getFileUploadLimit());
        plan.setFileSizeLimit(request.getFileSizeLimit());
        plan.setTokenLimit(request.getTokenLimit());
        plan.setPrice(request.getPrice());

        saveAdminActionLog(
                adminId,
                null,
                "UPDATE_POLICY",
                "플랜 정책 수정 - " + plan.getPlanName()
        );

        log.info("관리자 플랜 정책 수정 완료 - adminId={}, planId={}, planName={}",
                adminId, planId, plan.getPlanName());
    }

    /**
     * 관리자 통계 페이지 데이터 조회
     */
    @Override
    @Transactional(readOnly = true)
    public AdminStatsDto getStats(String periodType, String startDate, String endDate, String planType, int page) {

        log.info("관리자 통계 조회 요청 - periodType={}, startDate={}, endDate={}, planType={}, page={}",
                periodType, startDate, endDate, planType, page);

        // 기본 날짜 처리
        LocalDate start;
        LocalDate end;

        if (startDate != null && !startDate.isBlank()) {
            start = LocalDate.parse(startDate);
        } else {
            start = LocalDate.now().minusDays(6);
            startDate = start.toString();
        }

        if (endDate != null && !endDate.isBlank()) {
            end = LocalDate.parse(endDate);
        } else {
            end = LocalDate.now();
            endDate = end.toString();
        }

        // 날짜 역전 방지
        if (start.isAfter(end)) {
            LocalDate temp = start;
            start = end;
            end = temp;

            startDate = start.toString();
            endDate = end.toString();
        }

        // 상단 요약 통계
        long totalChatRooms = chatRoomRepository.count();
        long totalChats = 0;
        long totalImages = 0;
        long totalFiles = 0;
        long totalUsedTokens = userRepository.findAll()
                .stream()
                .mapToLong(UserEntity::getUsedToken)
                .sum();

        // 플랜별 통계
        List<AdminStatsDto.PlanStat> planStats = new ArrayList<>();

        if (planType == null || planType.isBlank() || planType.equals("NORMAL")) {
            planStats.add(AdminStatsDto.PlanStat.builder()
                    .planName("NORMAL")
                    .userCount(userRepository.countByPlan_PlanName("NORMAL"))
                    .chatRoomCount(0)
                    .chatCount(0)
                    .imageCount(0)
                    .fileCount(0)
                    .usedTokens(0)
                    .build());
        }

        if (planType == null || planType.isBlank() || planType.equals("PRO")) {
            planStats.add(AdminStatsDto.PlanStat.builder()
                    .planName("PRO")
                    .userCount(userRepository.countByPlan_PlanName("PRO"))
                    .chatRoomCount(0)
                    .chatCount(0)
                    .imageCount(0)
                    .fileCount(0)
                    .usedTokens(0)
                    .build());
        }

        if (planType == null || planType.isBlank() || planType.equals("MAX")) {
            planStats.add(AdminStatsDto.PlanStat.builder()
                    .planName("MAX")
                    .userCount(userRepository.countByPlan_PlanName("MAX"))
                    .chatRoomCount(0)
                    .chatCount(0)
                    .imageCount(0)
                    .fileCount(0)
                    .usedTokens(0)
                    .build());
        }

        // 기간별 통계
        List<AdminStatsDto.PeriodStat> allPeriodStats = new ArrayList<>();

        if ("monthly".equals(periodType)) {
            LocalDate current = start.withDayOfMonth(1);
            LocalDate lastMonth = end.withDayOfMonth(1);

            while (!current.isAfter(lastMonth)) {
                LocalDate monthStart = current;
                LocalDate monthEnd = current.plusMonths(1);

                long signupCount = userRepository.countByCreatedAtBetween(
                        monthStart.atStartOfDay(),
                        monthEnd.atStartOfDay()
                );

                allPeriodStats.add(AdminStatsDto.PeriodStat.builder()
                        .statDate(current.getYear() + "-" + String.format("%02d", current.getMonthValue()))
                        .signupCount(signupCount)
                        .chatRoomCount(0)
                        .chatCount(0)
                        .imageCount(0)
                        .fileCount(0)
                        .usedTokens(0)
                        .build());

                current = current.plusMonths(1);
            }

        } else if ("weekly".equals(periodType)) {
            LocalDate current = start;

            while (!current.isAfter(end)) {
                LocalDate weekStart = current;
                LocalDate weekEnd = current.plusDays(6);
                if (weekEnd.isAfter(end)) {
                    weekEnd = end;
                }

                long signupCount = userRepository.countByCreatedAtBetween(
                        weekStart.atStartOfDay(),
                        weekEnd.plusDays(1).atStartOfDay()
                );

                allPeriodStats.add(AdminStatsDto.PeriodStat.builder()
                        .statDate(weekStart + " ~ " + weekEnd)
                        .signupCount(signupCount)
                        .chatRoomCount(0)
                        .chatCount(0)
                        .imageCount(0)
                        .fileCount(0)
                        .usedTokens(0)
                        .build());

                current = weekEnd.plusDays(1);
            }

        } else {
            LocalDate current = start;

            while (!current.isAfter(end)) {
                long signupCount = userRepository.countByCreatedAtBetween(
                        current.atStartOfDay(),
                        current.plusDays(1).atStartOfDay()
                );

                allPeriodStats.add(AdminStatsDto.PeriodStat.builder()
                        .statDate(current.toString())
                        .signupCount(signupCount)
                        .chatRoomCount(0)
                        .chatCount(0)
                        .imageCount(0)
                        .fileCount(0)
                        .usedTokens(0)
                        .build());

                current = current.plusDays(1);
            }
        }

        // 페이지 처리
        int pageSize = 10;
        Pageable pageable = PageRequest.of(page, pageSize);

        int startIndex = (int) pageable.getOffset();
        int endIndex = Math.min(startIndex + pageable.getPageSize(), allPeriodStats.size());

        List<AdminStatsDto.PeriodStat> pageContent =
                startIndex >= allPeriodStats.size()
                        ? new ArrayList<>()
                        : allPeriodStats.subList(startIndex, endIndex);

        Page<AdminStatsDto.PeriodStat> statsPage =
                new PageImpl<>(pageContent, pageable, allPeriodStats.size());

        AdminStatsDto stats = AdminStatsDto.builder()
                .periodType(periodType)
                .startDate(startDate)
                .endDate(endDate)
                .planType(planType)
                .totalChatRooms(totalChatRooms)
                .totalChats(totalChats)
                .totalImages(totalImages)
                .totalFiles(totalFiles)
                .totalUsedTokens(totalUsedTokens)
                .planStats(planStats)
                .statsPage(statsPage)
                .build();

        log.info("관리자 통계 조회 성공 - totalChatRooms={}, totalUsedTokens={}, periodStatsCount={}",
                totalChatRooms, totalUsedTokens, allPeriodStats.size());

        return stats;
    }

}
