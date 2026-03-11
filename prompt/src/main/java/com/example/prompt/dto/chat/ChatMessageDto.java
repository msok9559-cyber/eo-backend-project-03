package com.example.prompt.dto.chat;

import com.example.prompt.domain.ChatMessageEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class ChatMessageDto {

    // 요청 DTO
    @Getter
    @NoArgsConstructor
    public static class Request {

        // 사용자가 입력한 메시지 내용 필수
        @NotBlank(message = "Content is required")
        private String content;
    }

    // 응답 DTO
    @Getter
    @Builder
    public static class Response {

        private Long chatId;
        private String role;
        private String content;
        private int tokensUsed;
        private LocalDateTime createdAt;

        // Entity → DTO 변환 정적 팩토리 메서드
        public static Response from(ChatMessageEntity msg) {
            return Response.builder()
                    .chatId(msg.getChatId())
                    .role(msg.getRole())
                    .content(msg.getContent())
                    .tokensUsed(msg.getTokensUsed())
                    .createdAt(msg.getCreatedAt())
                    .build();
        }
    }
}