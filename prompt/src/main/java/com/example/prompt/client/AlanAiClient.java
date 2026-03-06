package com.example.prompt.client;

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

    // AlanAiConfig 에서 만든 WebClient Bean 주입
    private final WebClient alanWebClient;

    // application.yml 의 alan.client-id 값 주입 (받은 키 값)
    @Value("${alan.client-id}")
    private String clientId;

    /**
     * SSE 스트리밍 호출 — ChatGPT처럼 글자가 하나씩 오는 방식
     * GET /api/v1/question/sse-streaming?client_id=xxx&content=xxx
     *
     * @param content 사용자가 입력한 메시지
     * @return 글자 조각(chunk)들의 스트림
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
     * 대화 기록 초기화
     * DELETE /api/v1/reset-state  body: {"client_id": "xxx"}
     * → 채팅방 삭제 시 앨런 AI 서버의 대화 기록도 같이 지움
     */
    public void resetState() {
        log.info("앨런 AI 상태 초기화");

        alanWebClient.method(HttpMethod.DELETE)  // ← 이렇게 변경
                .uri("/reset-state")
                .bodyValue(Map.of("client_id", clientId))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}