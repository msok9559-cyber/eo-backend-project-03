package com.example.prompt.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "chat_message")
public class ChatMessageEntity {

    // PK (chat_message 테이블의 chat_id)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatId;

    // 어느 채팅방의 메시지인지 (chatroom.chatroom_id FK)
    @Column(nullable = false)
    private Long chatroomId;

    // 메시지 작성자 구분 ("user" = 사용자, "assistant" = AI)
    @Column(nullable = false, length = 20)
    private String role;

    // 메시지 내용 (TEXT 타입)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 이 메시지에서 사용된 토큰 수
    @Builder.Default
    private int tokensUsed = 0;

    // 생성일 (자동 입력)
    @CreationTimestamp
    private LocalDateTime createdAt;

    // 삭제일
    private LocalDateTime deletedAt;

    // 정적 팩토리 메서드
    public static ChatMessageEntity of(Long chatroomId, String role, String content, int tokensUsed) {
        return ChatMessageEntity.builder()
                .chatroomId(chatroomId)
                .role(role)
                .content(content)
                .tokensUsed(tokensUsed)
                .build();
    }
}