package com.example.prompt.service;

import com.example.prompt.domain.PlanEntity;
import com.example.prompt.domain.UserEntity;
import com.example.prompt.dto.user.ResetPasswordDto;
import com.example.prompt.dto.user.UserDto;
import com.example.prompt.repository.PlanRepository;
import com.example.prompt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final String DEFAULT_PLAN_NAME = "NORMAL";

    // 아이디 중복 확인
    @Transactional(readOnly = true)
    public boolean isUseridAvailable(String userid){
        return !userRepository.existsByUserid(userid);
    }

    // 회원 가입
    public void signup(UserDto dto){
        if (userRepository.existsByUserid(dto.getUserid())){
            throw new IllegalArgumentException("이미 사용중인 아이디입니다.");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다");
        }
        if (!dto.getPassword().equals(dto.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
        }
        if (!emailService.isVerified(dto.getEmail())) {
            throw new IllegalArgumentException("이메일 인증을 완료해주세요");
        }
        PlanEntity normalPlan = planRepository.findByPlanName(DEFAULT_PLAN_NAME)
                .orElseThrow(() -> new IllegalStateException("기본 플랜이 존재하지 않습니다. 관리자에게 문의하세요."));

        UserEntity user = UserEntity.builder()
                .userid(dto.getUserid())
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .email(dto.getEmail())
                .plan(normalPlan)
                .build();

        userRepository.save(user);
        emailService.clearVerified(dto.getEmail());
    }

    // 비밀번호 재설정
    public void resetPassword(String email, ResetPasswordDto dto) {
        if (!emailService.isVerified(email)) {
            throw new IllegalArgumentException("이메일 인증을 먼저 완료해주세요");
        }
        if (!dto.getPassword().equals(dto.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
        }

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        emailService.clearVerified(email);
    }

    // 내 정보 조회
    @Transactional(readOnly = true)
    public UserDto getMyInfo(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        return UserDto.from(user);
    }

    // 비밀번호 변경 (마이페이지)
    public void updatePassword(Long userId, UserDto dto) {
        if (dto.getCurrentPassword() == null || dto.getCurrentPassword().isBlank()) {
            throw new IllegalArgumentException("현재 비밀번호를 입력해주세요");
        }
        if (!dto.getPassword().equals(dto.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다");
        }

        user.setPassword(passwordEncoder.encode(dto.getPassword()));
    }

    // 회원 탈퇴
    public void withdraw(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        user.setActive(false);
    }
}