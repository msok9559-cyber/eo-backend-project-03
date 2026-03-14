package com.example.prompt.dto.alan;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YoutubeSubtitleResponse {
        private Summary summary;

        @JsonProperty("token_info")
        private Object tokenInfo;

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Summary {
            private List<SummaryChapter> chapters;

            @JsonProperty("total_summary")
            private List<String> totalSummary;
        }

        @Getter
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
}