package com.example.prompt.controller.alan;

import com.example.prompt.dto.alan.AlanAiDto;
import com.example.prompt.dto.common.ApiResponse;
import com.example.prompt.security.CustomUserDetails;
import com.example.prompt.service.AlanAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alan")
@RequiredArgsConstructor
public class AlanAiController {

    private final AlanAiService alanAiService;

    /**
     * 페이지 요약
     * POST "http://localhost:8080/api/alan/page/summary"
     * Body: { "content": "요약할 페이지 내용" }
     */
    @PostMapping("/page/summary")
    public ApiResponse<AlanAiDto.PageSummaryResponse> summarizePage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody AlanAiDto.PageSummaryRequest request
    ) {
        return ApiResponse.ok(alanAiService.summarizePage(request, userDetails.getId()));
    }

    /**
     * 페이지 번역
     * POST "http://localhost:8080/api/alan/page/translate"
     * Body: { "contents": ["번역할 텍스트1", "번역할 텍스트2"] }
     */
    @PostMapping("/page/translate")
    public ApiResponse<AlanAiDto.PageTranslateResponse> translatePage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody AlanAiDto.PageTranslateRequest request
    ) {
        return ApiResponse.ok(alanAiService.translatePage(request, userDetails.getId()));
    }

    /**
     * 유튜브 자막 요약
     * POST "http://localhost:8080/api/alan/youtube/summary"
     * Body: { "subtitle": [{ "chapterIdx": 0, "chapterTitle": "제목", "text": [{ "timestamp": "0:00", "content": "내용" }] }] }
     */
    @PostMapping("/youtube/summary")
    public ApiResponse<AlanAiDto.YoutubeSubtitleResponse> summarizeYoutube(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody AlanAiDto.YoutubeSubtitleRequest request
    ) {
        return ApiResponse.ok(alanAiService.summarizeYoutube(request, userDetails.getId()));
    }

    /**
     * 일반 질문 (단순 응답)
     * GET "http://localhost:8080/api/alan/question?content=질문내용"
     */
    @GetMapping("/question")
    public ApiResponse<AlanAiDto.QuestionResponse> question(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String content
    ) {
        AlanAiDto.QuestionRequest request = new AlanAiDto.QuestionRequest(content);
        return ApiResponse.ok(alanAiService.question(request, userDetails.getId()));
    }

    /**
     * plain-streaming 질문
     * GET "http://localhost:8080/api/alan/question/plain-streaming?content=질문내용"
     */
    @GetMapping(value = "/question/plain-streaming", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public reactor.core.publisher.Flux<String> plainStreaming(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String content
    ) {
        AlanAiDto.QuestionRequest request = new AlanAiDto.QuestionRequest(content);
        return alanAiService.plainStreaming(request, userDetails.getId());
    }

}