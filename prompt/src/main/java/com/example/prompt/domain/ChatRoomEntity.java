package com.example.prompt.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "chatroom")
public class ChatRoomEntity {

    //PK (chatroom 테이블의 chatroom_id)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatroomId;

    // users.id FK (팀원 User 엔티티 완성 후 @ManyToOne으로 교체)
    @Column(nullable = false)
    private Long id;

    // 채팅방 제목 (기본값: "새 채팅")
    @Column(nullable = false, length = 100)
    @Builder.Default
    private String chatTitle = "새 채팅";

    // 사용한 AI 모델명 (예시: gpt-4o-mini)
    @Column(nullable = false, length = 100)
    private String model;

    // 생성일 (자동 입력)
    @CreationTimestamp
    private LocalDateTime createdAt;

    // 수정일 (자동 갱신)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 삭제일
    private LocalDateTime deletedAt;

    // 제목 수정
    public ChatRoomEntity updateTitle(String chatTitle) {
        this.chatTitle = chatTitle;
        return this;
    }

    // Soft Delete (삭제 시간만 기록)
    public ChatRoomEntity delete() {
        this.deletedAt = LocalDateTime.now();
        return this;
    }
}