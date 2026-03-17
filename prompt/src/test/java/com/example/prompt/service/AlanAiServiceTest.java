package com.example.prompt.service;

import com.example.prompt.dto.alan.AlanAiDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Slf4j
class AlanAiServiceTest {

    @Autowired
    private AlanAiService alanAiService;

    // data.sql 기준 테스트 유저 ID
    private static final Long TEST_USER_ID = 1L;

    /**
     * 페이지 요약 정상 테스트
     * [TEST] AlanAiServiceTest#testSummarizePage
     */
    @Test
    void testSummarizePage() {
        AlanAiDto.PageSummaryRequest request = new AlanAiDto.PageSummaryRequest(
                "[TEST] AlanAiServiceTest#testSummarizePage - " +
                        "인공지능(AI)은 인간의 지능을 모방하는 기술입니다."
        );

        log.info("페이지 요약 서비스 호출 전 - content = {}", request.getContent());
        AlanAiDto.PageSummaryResponse result = alanAiService.summarizePage(request, TEST_USER_ID);
        log.info("페이지 요약 서비스 결과 = {}", result.getSummary());

        assertThat(result).isNotNull();
        assertThat(result.getSummary()).isNotBlank();
    }

    /**
     * 페이지 요약 - 빈 내용 예외 테스트
     * [TEST] AlanAiServiceTest#testSummarizePageBlank
     */
    @Test
    void testSummarizePageBlank() {
        AlanAiDto.PageSummaryRequest request = new AlanAiDto.PageSummaryRequest("");

        log.info("페이지 요약 빈 내용 예외 테스트 시작");

        assertThatThrownBy(() -> alanAiService.summarizePage(request, TEST_USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("요약할 내용을 입력해주세요");

        log.info("페이지 요약 빈 내용 예외 발생 확인 완료");
    }

    /**
     * 페이지 번역 정상 테스트
     * [TEST] AlanAiServiceTest#testTranslatePage
     */
    @Test
    void testTranslatePage() {
        AlanAiDto.PageTranslateRequest request = new AlanAiDto.PageTranslateRequest(
                List.of("Artificial Intelligence is transforming the world.")
        );

        log.info("페이지 번역 서비스 호출 전 - contents = {}", request.getContents());
        AlanAiDto.PageTranslateResponse result = alanAiService.translatePage(request, TEST_USER_ID);
        log.info("페이지 번역 서비스 결과 = {}", result.getTranslated());

        assertThat(result).isNotNull();
        assertThat(result.getTranslated()).isNotBlank();
    }

    /**
     * 페이지 번역 - 빈 목록 예외 테스트
     * [TEST] AlanAiServiceTest#testTranslatePageEmpty
     */
    @Test
    void testTranslatePageEmpty() {
        AlanAiDto.PageTranslateRequest request = new AlanAiDto.PageTranslateRequest(List.of());

        log.info("페이지 번역 빈 목록 예외 테스트 시작");

        assertThatThrownBy(() -> alanAiService.translatePage(request, TEST_USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("번역할 내용을 입력해주세요");

        log.info("페이지 번역 빈 목록 예외 발생 확인 완료");
    }

    /**
     * 유튜브 자막 요약 정상 테스트
     * [TEST] AlanAiServiceTest#testSummarizeYoutube
     */
    @Test
    void testSummarizeYoutube() {
        AlanAiDto.YoutubeSubtitleRequest.SubtitleText t1 =
                new AlanAiDto.YoutubeSubtitleRequest.SubtitleText("0:00", "오늘은 자바 스트림 API에 대해 알아봅니다.");
        AlanAiDto.YoutubeSubtitleRequest.SubtitleText t2 =
                new AlanAiDto.YoutubeSubtitleRequest.SubtitleText("0:15", "filter, map, reduce 등의 연산을 체이닝해서 사용합니다.");

        AlanAiDto.YoutubeSubtitleRequest.Chapter chapter =
                new AlanAiDto.YoutubeSubtitleRequest.Chapter(0, "자바 스트림 API", List.of(t1, t2));

        AlanAiDto.YoutubeSubtitleRequest request =
                new AlanAiDto.YoutubeSubtitleRequest(List.of(chapter));

        log.info("유튜브 자막 요약 서비스 호출 전 - chapters size = {}", request.getSubtitle().size());
        AlanAiDto.YoutubeSubtitleResponse result = alanAiService.summarizeYoutube(request, TEST_USER_ID);
        log.info("유튜브 자막 요약 서비스 결과 = {}", result.getSummary().getChapters());

        assertThat(result).isNotNull();
        assertThat(result.getSummary()).isNotNull();
        assertThat(result.getSummary().getChapters()).isNotEmpty();
    }

    /**
     * 유튜브 자막 요약 - 빈 자막 예외 테스트
     * [TEST] AlanAiServiceTest#testSummarizeYoutubeEmpty
     */
    @Test
    void testSummarizeYoutubeEmpty() {
        AlanAiDto.YoutubeSubtitleRequest request =
                new AlanAiDto.YoutubeSubtitleRequest(List.of());

        log.info("유튜브 자막 빈 목록 예외 테스트 시작");

        assertThatThrownBy(() -> alanAiService.summarizeYoutube(request, TEST_USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("자막 데이터를 입력해주세요");

        log.info("유튜브 자막 빈 목록 예외 발생 확인 완료");
    }

    /**
     * 일반 질문 정상 테스트
     * [TEST] AlanAiServiceTest#testQuestion
     */
    @Test
    void testQuestion() {
        AlanAiDto.QuestionRequest request = new AlanAiDto.QuestionRequest(
                "[TEST] AlanAiServiceTest#testQuestion - 자바의 특징을 설명해주세요."
        );

        log.info("일반 질문 서비스 호출 전 - content = {}", request.getContent());
        AlanAiDto.QuestionResponse result = alanAiService.question(request, TEST_USER_ID);
        log.info("일반 질문 서비스 결과 = {}", result.getAnswer());

        assertThat(result).isNotNull();
        assertThat(result.getAnswer()).isNotBlank();
    }

    /**
     * 일반 질문 - 빈 내용 예외 테스트
     * [TEST] AlanAiServiceTest#testQuestionBlank
     */
    @Test
    void testQuestionBlank() {
        AlanAiDto.QuestionRequest request = new AlanAiDto.QuestionRequest("");

        log.info("일반 질문 빈 내용 예외 테스트 시작");

        assertThatThrownBy(() -> alanAiService.question(request, TEST_USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("질문 내용을 입력해주세요");

        log.info("일반 질문 빈 내용 예외 발생 확인 완료");
    }

    /**
     * plain-streaming 정상 테스트
     * [TEST] AlanAiServiceTest#testPlainStreaming
     */
    @Test
    void testPlainStreaming() {
        AlanAiDto.QuestionRequest request = new AlanAiDto.QuestionRequest(
                "[TEST] AlanAiServiceTest#testPlainStreaming - 스트리밍 질문 테스트입니다."
        );

        log.info("plain-streaming 서비스 호출 전 - content = {}", request.getContent());
        Flux<String> result = alanAiService.plainStreaming(request, TEST_USER_ID);
        log.info("plain-streaming Flux 생성 완료");

        // Flux가 null이 아니고 정상 생성되었는지만 확인 (실제 스트리밍 데이터는 구독 시 처리)
        assertThat(result).isNotNull();

        // 첫 번째 청크가 도착하는지 확인 (최대 5초 대기)
        String firstChunk = result.blockFirst(java.time.Duration.ofSeconds(5));
        log.info("plain-streaming 첫 번째 청크 = {}", firstChunk);

        assertThat(firstChunk).isNotNull();
    }

    /**
     * plain-streaming - 빈 내용 예외 테스트
     * [TEST] AlanAiServiceTest#testPlainStreamingBlank
     */
    @Test
    void testPlainStreamingBlank() {
        AlanAiDto.QuestionRequest request = new AlanAiDto.QuestionRequest("");

        log.info("plain-streaming 빈 내용 예외 테스트 시작");

        assertThatThrownBy(() -> alanAiService.plainStreaming(request, TEST_USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("질문 내용을 입력해주세요");

        log.info("plain-streaming 빈 내용 예외 발생 확인 완료");
    }
}