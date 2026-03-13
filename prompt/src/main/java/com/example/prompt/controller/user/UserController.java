package com.example.prompt.controller.user;

import com.example.prompt.dto.user.ResetPasswordDto;
import com.example.prompt.dto.user.UserDto;
import com.example.prompt.security.CustomUserDetails;
import com.example.prompt.service.UserService;
import com.example.prompt.dto.payment.PaymentDto;
import com.example.prompt.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PaymentService paymentService;

    // 세션용 결제 검증
    @PostMapping("/payment/verify")
    public ResponseEntity<Map<String, Object>> verifyPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PaymentDto dto) {
        PaymentDto result = paymentService.verifyAndSave(userDetails.getId(), dto);
        return ResponseEntity.ok(Map.of("success", result.getSuccess(), "message", result.getMessage()));
    }

    // 아이디 체크
    @GetMapping("/api/users/check-id")
    public ResponseEntity<Map<String, Boolean>> checkId(@RequestParam String userid) {
        boolean available = userService.isUseridAvailable(userid);
        return ResponseEntity.ok(Map.of("available", available));
    }

    // 회원 가입
    @PostMapping("/api/users")
    public ResponseEntity<Map<String, Object>> signup(@RequestBody @Valid UserDto dto) {
        userService.signup(dto);
        return ResponseEntity.ok(Map.of("success", true, "message", "회원가입이 완료되었습니다"));
    }

    // 비밀번호 재설정
    @PatchMapping("/api/user/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @RequestParam String email,
            @RequestBody @Valid ResetPasswordDto dto) {
        userService.resetPassword(email, dto);
        return ResponseEntity.ok(Map.of("success", true, "message", "비밀번호가 변경되었습니다"));
    }

    // 내정보 조회
    @GetMapping("/api/mypage")
    public ResponseEntity<UserDto> getMyPage(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserDto dto = userService.getMyInfo(userDetails.getId());
        return ResponseEntity.ok(dto);
    }

    // 비밀번호 변경 (세션 로그인 사용자용)
    @PatchMapping("/mypage/password")
    public ResponseEntity<Map<String, Object>> updatePasswordSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserDto dto,
            HttpServletRequest request) {
        userService.updatePassword(userDetails.getId(), dto);
        // 비밀번호 변경 후 세션 무효화
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "비밀번호가 변경되었습니다"));
    }

    // 비밀번호 변경
    @PatchMapping("/api/mypage/password")
    public ResponseEntity<Map<String, Object>> updatePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserDto dto) {
        userService.updatePassword(userDetails.getId(), dto);
        return ResponseEntity.ok(Map.of("success", true, "message", "비밀번호가 변경되었습니다"));
    }

    // 회원 탈퇴
    @PostMapping("/api/mypage/withdraw")
    public ResponseEntity<Map<String, Object>> withdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.withdraw(userDetails.getId());
        return ResponseEntity.ok(Map.of("success", true, "message", "탈퇴가 완료되었습니다"));
    }

}