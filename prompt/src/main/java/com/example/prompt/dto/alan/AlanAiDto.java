package com.example.prompt.dto.alan;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

public class AlanAiDto {

    // 페이지 요약
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageSummaryRequest {
        private String content;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageSummaryResponse {
        private String summary;
    }

    // 페이지 번역
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageTranslateRequest {
        private List<String> contents;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageTranslateResponse {
        private String translated;
    }

    // 유튜브 자막 요약
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YoutubeSubtitleRequest {
        private List<Chapter> subtitle;

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Chapter {
            // Alan AI가 snake_case로 요구하므로 직렬화 시 chapter_idx, chapter_title로 변환
            @JsonProperty("chapter_idx")
            private int chapterIdx;

            @JsonProperty("chapter_title")
            private String chapterTitle;

            private List<SubtitleText> text;
        }

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SubtitleText {
            private String timestamp;
            private String content;
        }
    }

    // YoutubeSubtitleResponse: Jackson 역직렬화를 위해 @Setter 필수
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YoutubeSubtitleResponse {
        private Summary summary;

        @JsonProperty("token_info")
        private Object tokenInfo;

        // @Setter 없으면 Jackson이 no-arg 생성자로 만든 뒤 필드를 채우지 못해 500 발생
        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Summary {
            private List<SummaryChapter> chapters;

            @JsonProperty("total_summary")
            private List<String> totalSummary;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SummaryChapter {
            private int index;
            private String title;
            private String timestamp;
            private List<String> detail;
            private List<String> summary;
        }
    }

    // 일반 질문 (단순 응답)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionRequest {
        private String content;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionResponse {
        private String answer;
    }
}