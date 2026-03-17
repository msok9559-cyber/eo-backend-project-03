package com.example.prompt.client;

import com.example.prompt.dto.alan.AlanAiDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Map;

// 앨런 AI API 를 실제로 호출하는 클래스
@Component
@RequiredArgsConstructor
@Slf4j
public class AlanAiClient {

    private final WebClient alanWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${alan.client-id}")
    private String clientId;

    /**
     * SSE 스트리밍 호출
     * GET /api/v1/question/sse-streaming?client_id=xxx&content=xxx
     */
    public Flux<String> streamChat(String content) {
        log.info("content = {}", content);

        return alanWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/question/sse-streaming")
                        .queryParam("client_id", clientId)
                        .queryParam("content", content)
                        .build()
                )
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnError(e -> log.info("streamChat error = {}", e.getMessage()));
    }

    /**
     * 일반 질문 (단순 응답, 스트리밍 없음)
     * GET /api/v1/question?client_id=xxx&content=xxx
     */
    public String question(String content) {
        log.info("일반 질문 요청 - content length = {}", content.length());

        return alanWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/question")
                        .queryParam("client_id", clientId)
                        .queryParam("content", content)
                        .build()
                )
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> log.error("question 오류 = {}", e.getMessage()))
                .block();
    }

    /**
     * 일반 스트리밍 질문 (plain text streaming)
     * GET /api/v1/question/plain-streaming?client_id=xxx&content=xxx
     */
    public Flux<String> plainStreaming(String content) {
        log.info("plain-streaming 요청 - content length = {}", content.length());

        return alanWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/question/plain-streaming")
                        .queryParam("client_id", clientId)
                        .queryParam("content", content)
                        .build()
                )
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnError(e -> log.error("plainStreaming 오류 = {}", e.getMessage()));
    }

    /**
     * 대화 기록 초기화
     * DELETE /api/v1/reset-state  body: {"client_id": "xxx"}
     */
    public void resetState() {
        log.info("앨런 AI 상태 초기화");

        alanWebClient.method(HttpMethod.DELETE)
                .uri("/reset-state")
                .bodyValue(Map.of("client_id", clientId))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /**
     * 페이지 요약
     * POST /api/v1/chrome/page/summary
     * body: { "content": "페이지 내용" }
     */
    public String summarizePage(String content) {
        log.info("페이지 요약 요청 - content length = {}", content.length());

        return alanWebClient.post()
                .uri("/chrome/page/summary")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("content", content))
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> log.error("페이지 요약 오류 = {}", e.getMessage()))
                .block();
    }

    /**
     * 페이지 번역
     * POST /api/v1/chrome/page/translate
     * body: { "contents": ["텍스트1", "텍스트2"] }
     */
    public String translatePage(AlanAiDto.PageTranslateRequest request) {
        log.info("페이지 번역 요청 - contents size = {}", request.getContents().size());

        return alanWebClient.post()
                .uri("/chrome/page/translate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("contents", request.getContents()))
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> log.error("페이지 번역 오류 = {}", e.getMessage()))
                .block();
    }

    /**
     * 유튜브 자막 요약
     * POST /api/v1/summary-youtube
     * body: { "subtitle": [...] }
     *
     * WebFlux 코덱 역직렬화 문제를 피하기 위해 String으로 받아서 ObjectMapper로 직접 파싱
     */
    public AlanAiDto.YoutubeSubtitleResponse summarizeYoutube(AlanAiDto.YoutubeSubtitleRequest request) {
        log.info("유튜브 자막 요약 요청 - chapters size = {}", request.getSubtitle().size());

        String raw = alanWebClient.post()
                .uri("/summary-youtube")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("subtitle", request.getSubtitle()))
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> log.error("유튜브 요약 오류 = {}", e.getMessage()))
                .block();

        try {
            log.info("유튜브 요약 raw 응답 = {}", raw);
            return objectMapper.readValue(raw, AlanAiDto.YoutubeSubtitleResponse.class);
        } catch (Exception e) {
            log.error("유튜브 요약 응답 파싱 실패 - raw = {}, error = {}", raw, e.getMessage());
            throw new IllegalStateException("유튜브 요약 응답 파싱 실패: " + e.getMessage());
        }
    }
}