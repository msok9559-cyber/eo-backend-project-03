package com.example.prompt.service;

import com.example.prompt.client.AlanAiClient;
import com.example.prompt.domain.UserEntity;
import com.example.prompt.dto.alan.AlanAiDto;
import com.example.prompt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlanAiService {

    private final AlanAiClient alanAiClient;
    private final UserRepository userRepository;

    /**
     * 페이지 요약
     */
    @Transactional
    public AlanAiDto.PageSummaryResponse summarizePage(AlanAiDto.PageSummaryRequest request, Long userId) {
        log.info("페이지 요약 서비스 호출 - userId = {}", userId);

        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("요약할 내용을 입력해주세요.");
        }

        UserEntity user = checkAndGetUser(userId);

        String result = alanAiClient.summarizePage(request.getContent());

        // 입력 + 결과 길이 기준으로 토큰 차감
        int tokensUsed = (request.getContent().length() + (result != null ? result.length() : 0)) / 4;
        deductToken(user, tokensUsed);
        log.info("페이지 요약 토큰 차감 - userId = {}, tokensUsed = {}", userId, tokensUsed);

        return AlanAiDto.PageSummaryResponse.builder()
                .summary(result)
                .build();
    }

    /**
     * 페이지 번역
     */
    @Transactional
    public AlanAiDto.PageTranslateResponse translatePage(AlanAiDto.PageTranslateRequest request, Long userId) {
        log.info("페이지 번역 서비스 호출 - userId = {}, contents size = {}", userId, request.getContents().size());

        if (request.getContents() == null || request.getContents().isEmpty()) {
            throw new IllegalArgumentException("번역할 내용을 입력해주세요.");
        }

        UserEntity user = checkAndGetUser(userId);

        String result = alanAiClient.translatePage(request);

        // 입력 전체 길이 + 결과 길이 기준으로 토큰 차감
        int inputLength = request.getContents().stream().mapToInt(String::length).sum();
        int tokensUsed = (inputLength + (result != null ? result.length() : 0)) / 4;
        deductToken(user, tokensUsed);
        log.info("페이지 번역 토큰 차감 - userId = {}, tokensUsed = {}", userId, tokensUsed);

        return AlanAiDto.PageTranslateResponse.builder()
                .translated(result)
                .build();
    }

    /**
     * 유튜브 자막 요약
     */
    @Transactional
    public AlanAiDto.YoutubeSubtitleResponse summarizeYoutube(AlanAiDto.YoutubeSubtitleRequest request, Long userId) {
        log.info("유튜브 자막 요약 서비스 호출 - userId = {}, chapters size = {}", userId, request.getSubtitle().size());

        if (request.getSubtitle() == null || request.getSubtitle().isEmpty()) {
            throw new IllegalArgumentException("자막 데이터를 입력해주세요.");
        }

        UserEntity user = checkAndGetUser(userId);

        AlanAiDto.YoutubeSubtitleResponse result = alanAiClient.summarizeYoutube(request);

        // 입력 자막 전체 글자 수 기준으로 토큰 차감
        int inputLength = request.getSubtitle().stream()
                .flatMap(chapter -> chapter.getText().stream())
                .mapToInt(text -> text.getContent().length())
                .sum();
        int tokensUsed = inputLength / 4;
        deductToken(user, tokensUsed);
        log.info("유튜브 요약 토큰 차감 - userId = {}, tokensUsed = {}", userId, tokensUsed);

        return result;
    }


    /**
     * 일반 질문 (단순 응답)
     */
    @Transactional
    public AlanAiDto.QuestionResponse question(AlanAiDto.QuestionRequest request, Long userId) {
        log.info("일반 질문 서비스 호출 - userId = {}", userId);

        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("질문 내용을 입력해주세요.");
        }

        UserEntity user = checkAndGetUser(userId);

        String raw = alanAiClient.question(request.getContent());

        // Alan AI 응답 형식: {"action":{...},"content":"실제 답변"}
        // content 필드만 추출해서 순수 텍스트로 반환
        String answer = extractAnswerContent(raw);

        int tokensUsed = (request.getContent().length() + (answer != null ? answer.length() : 0)) / 4;
        deductToken(user, tokensUsed);
        log.info("일반 질문 토큰 차감 - userId = {}, tokensUsed = {}", userId, tokensUsed);

        return AlanAiDto.QuestionResponse.builder()
                .answer(answer)
                .build();
    }

    /**
     * plain-streaming 질문
     */
    @Transactional
    public reactor.core.publisher.Flux<String> plainStreaming(AlanAiDto.QuestionRequest request, Long userId) {
        log.info("plain-streaming 서비스 호출 - userId = {}", userId);

        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("질문 내용을 입력해주세요.");
        }

        // 토큰 한도 체크만 수행 (차감은 스트리밍 완료 후 불가 → 입력 기준으로 선차감)
        UserEntity user = checkAndGetUser(userId);
        int estimatedTokens = request.getContent().length() / 4;
        deductToken(user, estimatedTokens);

        return alanAiClient.plainStreaming(request.getContent());
    }

    /**
     * Alan AI question 응답에서 content 필드 추출
     * 응답 형식: {"action":{"name":"...","speak":"..."},"content":"실제 답변"}
     */
    private String extractAnswerContent(String raw) {
        if (raw == null || raw.isBlank()) return "";
        try {
            int idx = raw.indexOf("\"content\":");
            if (idx == -1) return raw;
            int start = raw.indexOf("\"", idx + 10) + 1;
            if (start <= 0) return raw;
            // 닫는 " 탐색 (이스케이프 처리)
            int end = start;
            while (end < raw.length()) {
                char c = raw.charAt(end);
                if (c == '\\') { end += 2; continue; }
                if (c == '"') break;
                end++;
            }
            return raw.substring(start, end)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        } catch (Exception e) {
            log.warn("question 응답 파싱 실패 - raw = {}", raw);
            return raw;
        }
    }

    /**
     * 토큰 한도 체크 후 유저 반환
     */
    private UserEntity checkAndGetUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        int tokenLimit = user.getPlan().getTokenLimit();
        int usedToken  = user.getUsedToken();

        if (usedToken >= tokenLimit) {
            log.warn("토큰 한도 초과 - userId = {}, usedToken = {}, tokenLimit = {}", userId, usedToken, tokenLimit);
            throw new IllegalArgumentException("토큰 한도를 초과했습니다. 플랜을 업그레이드 해주세요.");
        }

        return user;
    }

    /**
     * 토큰 차감 (한도 초과 방지: 남은 토큰까지만 차감)
     */
    private void deductToken(UserEntity user, int tokensUsed) {
        int tokenLimit  = user.getPlan().getTokenLimit();
        int newUsed     = Math.min(user.getUsedToken() + tokensUsed, tokenLimit);
        user.setUsedToken(newUsed);
        userRepository.save(user);
    }
}