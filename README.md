# 🤖 Doctrio — AI 플랜 기반 채팅 서비스 플랫폼

> 이스트소프트 백엔드 과정 **3차 팀 프로젝트**  
> Spring Boot 기반 구독형 AI 채팅 포털 웹 애플리케이션

---

## 👥 팀 구성

| 역할 | 이름 |
|------|------|
| 팀장 | 정확 🌟 |
| 팀원 | 김재웅 |
| 팀원 | 박민성 |

---

## 🛠 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java |
| Framework | Spring Boot, Spring Security, Spring WebFlux (WebClient) |
| Database | MySQL |
| ORM | JPA / Hibernate |
| Build | Gradle |
| Template Engine | Thymeleaf |
| 암호화 | BCryptPasswordEncoder |
| 이메일 | JavaMailSender |
| 소셜 로그인 | OAuth2 (Google) |
| 인증 | JWT (관리자), Session (일반 유저) |
| AI 연동 | Alan AI API (SSE 스트리밍) |
| 결제 | PortOne (아임포트) 가상 결제 |
| 스케줄러 | Spring Scheduler (토큰 초기화) |

---

## 🎯 프로젝트 목표

플랜(Normal · Pro · Max)에 따라 사용 가능한 AI 모델과 토큰량을 차등 제공하는 구독형 AI 포털 서비스

- 플랜 기반 토큰 차등 시스템 설계 및 구현
- Spring Security + JWT + OAuth2 복합 인증 체계
- Alan AI SSE 스트리밍 실시간 채팅 구현
- 관리자 대시보드 및 통계 기능

---

## 👨‍💻 역할 분담

| 이름 | 담당 업무 |
|------|-----------|
| 정확 | 팀 리드 · AI 채팅 기능 · 채팅 페이지 · AI 도구 페이지 (유튜브 요약 · 페이지 요약/번역) |
| 김재웅 | 관리자 대시보드 · 사용자 관리 · 관리자 처리 이력 · 플랜 정책 관리 |
| 박민성 | 메인 페이지 · 유저 기능 · 로그인/회원가입 · 소셜 로그인 · 마이페이지 · 결제/플랜 소개 페이지 · 도움말 |

---

## 📅 개발 일정

| Phase | 기간 | 내용 |
|-------|------|------|
| Phase 1 | Week 1 | 기획 & 설계 (요구사항 명세서, ERD, Figma 화면 설계, API 명세서) |
| Phase 2 | Week 2~4 | 백엔드 개발 (회원 인증, AI 채팅 API, 결제, 관리자, Spring Security) |
| Phase 3 | Week 3~4 | 프론트엔드 개발 (Thymeleaf 페이지, 채팅 UI, 마이페이지, 반응형) |
| Phase 4 | Week 3~4 | 검토 & 발표 (통합 테스트, 버그 수정, PPT 제작, 최종 코드 리뷰) |

---

## 📁 프로젝트 구조

```
com.example.prompt
├── client
│   └── AlanAiClient.java              # Alan AI API 호출 (SSE 스트리밍, 요약, 번역)
│
├── config
│   ├── SecurityConfiguration.java     # JWT 체인(Order 1) / Admin 세션(Order 2) / 일반 세션+OAuth2(Order 3)
│   ├── AlanAiConfiguration.java       # Alan AI WebClient Bean
│   ├── PortOneConfig.java             # IamportClient Bean
│   ├── PasswordEncoderConfig.java     # BCrypt Bean
│   └── SchedulerConfig.java           # @EnableScheduling
│
├── controller
│   ├── admin
│   │   ├── AdminAuthController.java   # 관리자 JWT 인증 API
│   │   └── AdminController.java       # 관리자 페이지 + REST API
│   ├── alan
│   │   └── AlanAiController.java      # AI 요약 · 번역 · 유튜브 · 질문
│   ├── chat
│   │   └── ChatController.java        # 채팅방 CRUD + SSE 스트리밍
│   ├── email
│   │   └── EmailController.java       # 이메일 인증번호 발송/검증
│   ├── page
│   │   └── PageController.java        # Thymeleaf 뷰 라우팅
│   ├── payment
│   │   └── PaymentController.java     # PortOne 결제 검증
│   ├── stats
│   │   └── StatsController.java       # 통계 API
│   ├── user
│   │   └── UserController.java        # 회원 CRUD
│   └── error
│       └── ErrorPageController.java   # 에러 페이지
│
├── domain
│   ├── UserEntity.java
│   ├── AdminEntity.java
│   ├── AdminActionLogEntity.java
│   ├── PlanEntity.java
│   ├── PlanModelEntity.java
│   ├── ChatRoomEntity.java
│   ├── ChatMessageEntity.java
│   └── PaymentEntity.java
│
├── dto
│   ├── admin/                         # Admin 관련 DTO
│   ├── alan/                          # Alan AI 관련 DTO
│   ├── chat/                          # Chat 관련 DTO
│   ├── common/                        # ApiResponse, 공통 Enum
│   ├── payment/                       # 결제 DTO
│   ├── plan/                          # 플랜 DTO
│   ├── stats/                         # 통계 DTO
│   └── user/                          # 유저 DTO
│
├── repository
│   ├── UserRepository.java
│   ├── AdminRepository.java
│   ├── AdminActionLogRepository.java
│   ├── ChatRoomRepository.java
│   ├── ChatMessageRepository.java
│   ├── PaymentRepository.java
│   ├── PlanRepository.java
│   └── PlanModelRepository.java
│
├── scheduler
│   └── TokenResetScheduler.java       # 매일 00:00 토큰 초기화
│
├── security
│   ├── jwt
│   │   ├── JwtProvider.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── JwtProperties.java
│   ├── CustomUserDetails.java
│   ├── CustomUserDetailsService.java
│   ├── CustomOAuth2UserService.java
│   ├── CustomOAuth2UserDetails.java
│   ├── AdminDetailsService.java
│   └── AdminPrincipal.java
│
├── service
│   ├── UserService.java
│   ├── ChatService.java
│   ├── AlanAiService.java
│   ├── PaymentService.java
│   ├── EmailService.java
│   ├── StatsService.java
│   ├── PlanService.java
│   └── AdminService.java (interface) / AdminServiceImpl.java
│
├── util
│   ├── WebPageFetcher.java            # jsoup 웹 페이지 텍스트 추출
│   └── YouTubeSubtitleFetcher.java    # 유튜브 자막 자동 추출
│
└── exception
    └── GlobalExceptionHandler.java    # @Valid 실패, 비즈니스 예외 처리
```

---

## ERD (엔티티 관계)

```
users (회원)
 ├── id (PK)
 ├── plan_id           FK → plans
 ├── admin_id          FK → admin (nullable)
 ├── userid            UNIQUE
 ├── username
 ├── password          BCrypt 암호화
 ├── email             UNIQUE
 ├── used_token        사용 토큰량
 ├── token_reset_at    토큰 초기화 시각
 ├── plan_expired_at   플랜 만료일
 ├── active            탈퇴 시 false (Soft Delete)
 ├── locked            관리자 잠금 처리
 ├── provider          소셜 로그인 (google 등)
 ├── provider_id
 ├── created_at
 └── updated_at

plans (플랜)
 ├── plan_id (PK)
 ├── plan_name         NORMAL / PRO / MAX
 ├── token_limit       월 토큰 한도
 ├── ai_use
 ├── price             월 구독료
 ├── daily_chat_limit  일일 채팅 제한 (-1: 무제한)
 ├── image_upload_limit
 ├── file_upload_limit
 ├── file_size_limit   MB 단위
 ├── created_at
 └── updated_at

plan_models (플랜별 사용 가능 모델)
 ├── model_id (PK)
 ├── plan_id           FK → plans
 └── model_name        alan-4.0 / alan-4.1 / alan-4-turbo

chatroom (채팅방)
 ├── chatroom_id (PK)
 ├── id                FK → users
 ├── chat_title
 ├── model             사용 AI 모델명
 ├── created_at
 ├── updated_at
 └── deleted_at        Soft Delete

chat_message (채팅 메시지)
 ├── chat_id (PK)
 ├── chatroom_id       FK → chatroom
 ├── role              user / assistant
 ├── content           TEXT
 ├── tokens_used       메시지별 토큰 사용량
 ├── created_at
 └── deleted_at

payments (결제)
 ├── payment_id (PK)
 ├── id                FK → users
 ├── plan_id           FK → plans
 ├── imp_uid           포트원 결제 고유번호 UNIQUE
 ├── amount            결제 금액
 └── paid_at           결제 완료 시각

admin (관리자)
 ├── id (PK)
 ├── admin_id          UNIQUE
 ├── admin_name
 ├── password          BCrypt 암호화
 └── created_at

admin_action_logs (관리자 처리 이력)
 ├── log_id (PK)
 ├── admin_id
 ├── target_user_id    FK → users
 ├── action_type       LOCK / UNLOCK / RESTORE / WITHDRAW / PLAN_CHANGE
 ├── description
 └── created_at
```

---

## 🔐 보안 및 인증

### 인증 방식

- **일반 유저 로그인**: `userid` + `password` (Spring Security Form Login + Session)
- **소셜 로그인**: Google OAuth2 (`CustomOAuth2UserService`)
  - 최초 로그인 시 자동 회원가입
  - 이미 등록된 이메일이면 기존 계정으로 연동
- **관리자 로그인**: Form Login → JWT 발급 (HS256, 30분 만료)
- **비밀번호 암호화**: `BCryptPasswordEncoder`

### Security Filter Chain (3개 체인)

| Order | 적용 경로 | 방식 | 설명 |
|-------|-----------|------|------|
| 1 | `/api/users/**`, `/api/email/**`, `/api/admin/**`, `/api/payment/**` 등 | JWT Stateless | REST API용 JWT 인증 |
| 2 | `/admin`, `/admin/**` | Session Form Login | 관리자 페이지 세션 인증 |
| 3 | 나머지 전체 | Session + OAuth2 | 일반 유저 세션 인증 |

### 권한 정책

| 기능 | 비로그인 | USER | ADMIN |
|------|:--------:|:----:|:-----:|
| 메인 / 로그인 / 회원가입 | ✅ | ✅ | ✅ |
| AI 채팅 / AI 도구 | ❌ | ✅ | ✅ |
| 마이페이지 | ❌ | ✅ | ✅ |
| 결제 / 플랜 업그레이드 | ❌ | ✅ | ✅ |
| 관리자 페이지 | ❌ | ❌ | ✅ |
| 사용자 잠금 / 플랜 변경 | ❌ | ❌ | ✅ |
| 플랜 정책 수정 | ❌ | ❌ | ✅ |

---

## ✨ 주요 기능 및 API

### 회원

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/users` | 회원가입 |
| GET | `/api/users/check-id` | 아이디 중복 확인 |
| POST | `/login` | 일반 로그인 (Form) |
| GET | `/oauth2/authorization/google` | Google OAuth2 로그인 |
| POST | `/logout` | 로그아웃 |
| GET | `/api/mypage` | 내 정보 조회 |
| PATCH | `/mypage/password` | 비밀번호 변경 |
| POST | `/mypage/withdraw` | 회원 탈퇴 |
| PATCH | `/api/user/reset-password` | 비밀번호 재설정 (이메일 인증 후) |

### 이메일 인증

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/email/send-verification` | 6자리 인증번호 발송 (유효시간 5분) |
| GET | `/api/email/verify-code` | 인증번호 확인 |

### AI 채팅

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/chat/rooms` | 채팅방 생성 (모델 선택 포함) |
| GET | `/api/chat/rooms` | 채팅방 목록 조회 |
| GET | `/api/chat/rooms/{chatroomId}/messages` | 메시지 목록 조회 |
| GET | `/api/chat/rooms/{chatroomId}/stream` | SSE 스트리밍 (AI 실시간 응답) |
| PATCH | `/api/chat/rooms/{chatroomId}/title` | 채팅방 제목 수정 |
| DELETE | `/api/chat/rooms/{chatroomId}` | 채팅방 삭제 |
| DELETE | `/api/chat/rooms/all` | 전체 채팅방 삭제 |

### AI 도구

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/alan/page/summary` | 페이지 요약 (텍스트 직접 입력) |
| POST | `/api/alan/page/summary/url` | 페이지 요약 (URL 자동 추출) |
| POST | `/api/alan/page/translate` | 페이지 번역 (텍스트 직접 입력) |
| POST | `/api/alan/page/translate/url` | 페이지 번역 (URL 자동 추출) |
| POST | `/api/alan/youtube/summary` | 유튜브 자막 요약 (자막 직접 입력) |
| POST | `/api/alan/youtube/url` | 유튜브 자막 요약 (URL 자동 추출) |
| GET | `/api/alan/question` | 일반 AI 질문 |

### 결제

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/payments/verify` | PortOne 결제 검증 및 플랜 업그레이드 |
| GET | `/api/payments` | 내 결제 내역 조회 |

### 관리자

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/admin/auth/login` | 관리자 로그인 (JWT 발급) |
| GET | `/admin/api/dashboard` | 대시보드 통계 |
| GET | `/admin/api/users` | 사용자 목록 조회 (페이징 · 검색 · 필터) |
| GET | `/admin/api/users/{userId}` | 사용자 상세 조회 |
| PATCH | `/admin/api/users/{userId}/lock` | 계정 잠금 |
| PATCH | `/admin/api/users/{userId}/unlock` | 잠금 해제 |
| PATCH | `/admin/api/users/{userId}/plan` | 플랜 변경 |
| PATCH | `/admin/api/users/{userId}/status` | 계정 상태 변경 |
| GET | `/admin/api/plans` | 플랜 정책 목록 |
| PATCH | `/admin/api/plans/{planId}` | 플랜 정책 수정 |
| GET | `/api/stats` | 서비스 통계 (totalUsers, totalMessages, totalPayments) |

---

## 🔑 플랜 정책

| 항목 | NORMAL | PRO | MAX |
|------|:------:|:---:|:---:|
| 가격 | 무료 | 9,900원/월 | 29,900원/월 |
| 월 토큰 한도 | 10,000 | 100,000 | 500,000 |
| 일일 채팅 제한 | 20회 | 100회 | 무제한 |
| 이미지 업로드 | 5개 | 20개 | 50개 |
| 파일 업로드 | 3개 | 10개 | 30개 |
| 파일 최대 용량 | 10MB | 50MB | 100MB |
| 사용 가능 모델 | alan-4.0 | alan-4.0, alan-4.1 | alan-4.0, alan-4.1, alan-4-turbo |

> 모델별 토큰 배율: `alan-4.0` × 1.0 / `alan-4.1` × 1.5 / `alan-4-turbo` × 2.0

---

## ⚙️ 핵심 서비스 로직

**UserService**
- 회원가입 시 userid / email 중복 체크 후 BCrypt 암호화 저장
- 이메일 인증 완료 여부 검증 후 가입 처리
- 가입 시 NORMAL 플랜 자동 부여

**ChatService**
- 채팅방 생성 시 플랜에서 해당 모델 사용 가능 여부 검증
- Alan AI SSE 스트리밍 → 청크 수신 시 브라우저로 실시간 전송
- 스트리밍 완료 후 `self.saveStreamResult()` (Spring 프록시로 `@Transactional` 적용)
- 토큰 차감 = `(질문 길이 + 응답 길이) / 4 × 모델 배율`
- 한도 도달 시 `token-exhausted` SSE 이벤트 전송
- Soft Delete 방식 (채팅방/메시지 모두)

**AlanAiService**
- 페이지 요약/번역: 텍스트 직접 입력 또는 URL → `WebPageFetcher`로 본문 추출 후 Alan AI 호출
- 유튜브 요약: URL → `YouTubeSubtitleFetcher`로 자막 자동 추출 (한국어 우선, 없으면 영어)
- 모든 기능 실행 후 토큰 차감

**PaymentService**
- `imp_uid` 중복 결제 방지
- 결제 금액 ↔ 플랜 가격 일치 검증
- 결제 성공 시 플랜 업그레이드 + `plan_expired_at` = 결제일 + 30일

**TokenResetScheduler**
- 매일 00:00 실행
- `tokenResetAt` 있으면 마지막 초기화일 + 30일, 없으면 `createdAt` + 30일 기준 초기화

**EmailService**
- 6자리 난수 인증번호 생성
- `ConcurrentHashMap` 인메모리 관리, 유효시간 5분
- 재발송 시 기존 인증 상태 초기화

**AdminService**
- 처리 이력(LOCK / UNLOCK / RESTORE / WITHDRAW / PLAN_CHANGE) 자동 기록
- 플랜 정책 수정 시 즉시 전체 사용자에 반영

---

## 📂 화면 구성

| 페이지 | 설명 |
|--------|------|
| 메인 (`/`) | 서비스 소개, 통계 카운팅 애니메이션, 플랜 소개 진입 |
| 로그인 (`/login`) | Form 로그인 + Google OAuth2 소셜 로그인 |
| 회원가입 (`/signup`) | 아이디 중복 확인, 이메일 인증, 약관 동의 |
| 비밀번호 찾기 (`/reset-password`) | 이메일 인증 3단계 → 비밀번호 재설정 |
| AI 채팅 (`/chat`) | 사이드바 채팅방 목록, SSE 실시간 스트리밍, 모델 선택 |
| AI 도구 (`/ai-tools`) | 페이지 요약/번역, 유튜브 요약, AI 질문 (탭 구성) |
| 플랜 안내 (`/payment`) | NORMAL / PRO / MAX 플랜 비교, 현재 플랜 표시 |
| 결제 (`/payment/checkout`) | PortOne 카카오페이 결제 |
| 마이페이지 | 팝업 형태, 내 정보 / 비밀번호 변경 / 회원 탈퇴 (OAuth2 유저는 비밀번호 탭 숨김) |
| 도움말 (`/guide`) | 서비스 소개, 플랜 안내, 사용법, FAQ 아코디언 |
| 관리자 로그인 (`/admin/login`) | 관리자 전용 Form 로그인 |
| 관리자 대시보드 (`/admin`) | 전체 회원/플랜/사용량 요약 카드 |
| 사용자 관리 (`/admin/users`) | 목록 페이징, 검색, 잠금/해제/플랜 변경, 상세 모달 |
| 처리 이력 (`/admin/logs`) | 관리자 액션 로그 검색/필터/페이징 |
| 플랜 정책 (`/admin/policies`) | 플랜별 정책 조회 및 수정 모달 |
| 통계 (`/admin/stats`) | 플랜별/기간별 통계 테이블 + 페이징 |
