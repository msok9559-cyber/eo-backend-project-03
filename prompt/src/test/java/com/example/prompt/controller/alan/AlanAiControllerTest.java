package com.example.prompt.controller.alan;

import com.example.prompt.dto.alan.AlanAiDto;
import com.example.prompt.security.CustomUserDetails;
import com.example.prompt.service.AlanAiService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Slf4j
class AlanAiControllerTest {

    @Mock
    private AlanAiService alanAiService;

    @InjectMocks
    private AlanAiController alanAiController;

    // 컨트롤러에 주입할 mock UserDetails
    private CustomUserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        mockUserDetails = mock(CustomUserDetails.class);
        when(mockUserDetails.getId()).thenReturn(1L);
    }

    /**
     * 페이지 요약 컨트롤러 단위 테스트
     * [TEST] AlanAiControllerTest#testSummarizePage
     */
    @Test
    void testSummarizePage() {
        AlanAiDto.PageSummaryRequest request = new AlanAiDto.PageSummaryRequest(
                "[TEST] AlanAiControllerTest#testSummarizePage - 테스트 내용"
        );
        AlanAiDto.PageSummaryResponse mockResponse = AlanAiDto.PageSummaryResponse.builder()
                .summary("테스트 요약 결과")
                .build();

        given(alanAiService.summarizePage(any(), anyLong())).willReturn(mockResponse);

        log.info("페이지 요약 컨트롤러 테스트 시작");
        var result = alanAiController.summarizePage(mockUserDetails, request);

        log.info("페이지 요약 컨트롤러 결과 = {}", result.getData().getSummary());
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getSummary()).isEqualTo("테스트 요약 결과");
    }

    /**
     * 페이지 번역 컨트롤러 단위 테스트
     * [TEST] AlanAiControllerTest#testTranslatePage
     */
    @Test
    void testTranslatePage() {
        AlanAiDto.PageTranslateRequest request = new AlanAiDto.PageTranslateRequest(
                List.of("Hello World")
        );
        AlanAiDto.PageTranslateResponse mockResponse = AlanAiDto.PageTranslateResponse.builder()
                .translated("안녕 세계")
                .build();

        given(alanAiService.translatePage(any(), anyLong())).willReturn(mockResponse);

        log.info("페이지 번역 컨트롤러 테스트 시작");
        var result = alanAiController.translatePage(mockUserDetails, request);

        log.info("페이지 번역 컨트롤러 결과 = {}", result.getData().getTranslated());
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getTranslated()).isEqualTo("안녕 세계");
    }

    /**
     * 유튜브 자막 요약 컨트롤러 단위 테스트
     * [TEST] AlanAiControllerTest#testSummarizeYoutube
     */
    @Test
    void testSummarizeYoutube() {
        AlanAiDto.YoutubeSubtitleRequest.SubtitleText text =
                new AlanAiDto.YoutubeSubtitleRequest.SubtitleText("0:00", "테스트 자막");
        AlanAiDto.YoutubeSubtitleRequest.Chapter chapter =
                new AlanAiDto.YoutubeSubtitleRequest.Chapter(0, "테스트 챕터", List.of(text));
        AlanAiDto.YoutubeSubtitleRequest request =
                new AlanAiDto.YoutubeSubtitleRequest(List.of(chapter));

        AlanAiDto.YoutubeSubtitleResponse.SummaryChapter summaryChapter =
                new AlanAiDto.YoutubeSubtitleResponse.SummaryChapter(
                        0, "테스트 챕터", "0:00", List.of("상세"), List.of("요약")
                );
        AlanAiDto.YoutubeSubtitleResponse.Summary summary =
                new AlanAiDto.YoutubeSubtitleResponse.Summary(List.of(summaryChapter), List.of("전체 요약"));
        AlanAiDto.YoutubeSubtitleResponse mockResponse =
                AlanAiDto.YoutubeSubtitleResponse.builder().summary(summary).build();

        given(alanAiService.summarizeYoutube(any(), anyLong())).willReturn(mockResponse);

        log.info("유튜브 자막 요약 컨트롤러 테스트 시작");
        var result = alanAiController.summarizeYoutube(mockUserDetails, request);

        log.info("유튜브 자막 요약 컨트롤러 결과 = {}", result.getData().getSummary().getChapters());
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getSummary().getChapters()).isNotEmpty();
        assertThat(result.getData().getSummary().getChapters().get(0).getTitle()).isEqualTo("테스트 챕터");
    }

    /**
     * 일반 질문 컨트롤러 단위 테스트
     * [TEST] AlanAiControllerTest#testQuestion
     */
    @Test
    void testQuestion() {
        AlanAiDto.QuestionResponse mockResponse = AlanAiDto.QuestionResponse.builder()
                .answer("테스트 답변입니다.")
                .build();

        given(alanAiService.question(any(), anyLong())).willReturn(mockResponse);

        log.info("일반 질문 컨트롤러 테스트 시작");
        var result = alanAiController.question(
                mockUserDetails,
                "[TEST] AlanAiControllerTest#testQuestion - 테스트 질문"
        );

        log.info("일반 질문 컨트롤러 결과 = {}", result.getData().getAnswer());
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getAnswer()).isEqualTo("테스트 답변입니다.");
    }

    /**
     * plain-streaming 컨트롤러 단위 테스트
     * [TEST] AlanAiControllerTest#testPlainStreaming
     */
    @Test
    void testPlainStreaming() {
        Flux<String> mockFlux = Flux.just("안녕", "하세", "요");

        given(alanAiService.plainStreaming(any(), anyLong())).willReturn(mockFlux);

        log.info("plain-streaming 컨트롤러 테스트 시작");
        Flux<String> result = alanAiController.plainStreaming(
                mockUserDetails,
                "[TEST] AlanAiControllerTest#testPlainStreaming - 테스트 질문"
        );

        // Flux를 블로킹으로 수집해서 검증
        List<String> chunks = result.collectList().block();

        log.info("plain-streaming 컨트롤러 결과 = {}", chunks);
        assertThat(chunks).isNotNull();
        assertThat(chunks).isNotEmpty();
        assertThat(String.join("", chunks)).isEqualTo("안녕하세요");
    }
}