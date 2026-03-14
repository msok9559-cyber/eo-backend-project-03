package com.example.prompt.dto.chat;

import com.example.prompt.domain.ChatRoomEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

public class ChatRoomDto {

    // 요청 DTO
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {

        // 채팅방 제목 (안 보내면 "새 채팅"으로 기본값 설정)
        @Builder.Default
        private String chatTitle = "새 채팅";

        // 사용할 AI 모델명 (필수)
        @NotBlank(message = "Model is required")
        private String model;
    }

    // 응답 DTO
    @Getter
    @Builder
    public static class Response {

        private Long chatroomId;
        private String chatTitle;
        private String model;
        private LocalDateTime createdAt;

        // Entity → DTO 변환 정적 팩토리 메서드
        public static Response from(ChatRoomEntity chatRoom) {
            return Response.builder()
                    .chatroomId(chatRoom.getChatroomId())
                    .chatTitle(chatRoom.getChatTitle())
                    .model(chatRoom.getModel())
                    .createdAt(chatRoom.getCreatedAt())
                    .build();
        }
    }
}