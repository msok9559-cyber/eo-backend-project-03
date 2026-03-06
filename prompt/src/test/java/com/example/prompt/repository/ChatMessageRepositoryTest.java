package com.example.prompt.repository;

import com.example.prompt.domain.ChatMessageEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Slf4j
@Transactional
class ChatMessageRepositoryTest {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    /**
     * 메시지 저장 테스트
     * - 정적 팩토리 of() 사용 후 PK 자동 생성 여부 확인
     */
    @Test
    @DisplayName("메시지 저장 테스트")
    void testCreate() {
        ChatMessageEntity message = ChatMessageEntity.of(
                1L,
                "user",
                "[TEST] ChatMessageRepositoryTest#testCreate",
                0
        );

        log.info("저장 전 chatId = {}", message.getChatId());
        ChatMessageEntity saved = chatMessageRepository.save(message);
        log.info("저장 후 chatId = {}", saved.getChatId());

        assertThat(saved.getChatId()).isNotNull();
        assertThat(saved.getRole()).isEqualTo("user");
        assertThat(saved.getContent()).isEqualTo("[TEST] ChatMessageRepositoryTest#testCreate");
        assertThat(saved.getTokensUsed()).isEqualTo(0);
    }

    /**
     * 메시지 목록 조회 테스트
     * - 오래된 것부터 순서대로 오는지 확인 (대화 순서)
     * - 삭제된 메시지는 제외되는지 확인
     */
    @Test
    @DisplayName("메시지 목록 조회 - 시간 순서 + 삭제 제외")
    void testFindByChatroomId() {
        // 같은 채팅방에 메시지 3개 저장
        ChatMessageEntity msg1 = chatMessageRepository.save(
                ChatMessageEntity.of(1L, "user",
                        "[TEST] ChatMessageRepositoryTest#testFindByChatroomId - 1번", 0));

        ChatMessageEntity msg2 = chatMessageRepository.save(
                ChatMessageEntity.of(1L, "assistant",
                        "[TEST] ChatMessageRepositoryTest#testFindByChatroomId - 2번", 10));

        ChatMessageEntity msg3 = chatMessageRepository.save(
                ChatMessageEntity.of(1L, "user",
                        "[TEST] ChatMessageRepositoryTest#testFindByChatroomId - 3번 (삭제)", 0));

        // msg3 — ChatMessageEntity 에 delete() 없으므로 Builder 로 처리
        msg3 = ChatMessageEntity.builder()
                .chatId(msg3.getChatId())
                .chatroomId(msg3.getChatroomId())
                .role(msg3.getRole())
                .content(msg3.getContent())
                .tokensUsed(msg3.getTokensUsed())
                .deletedAt(java.time.LocalDateTime.now())
                .build();
        chatMessageRepository.save(msg3);

        log.info("msg1 deletedAt = {}", msg1.getDeletedAt());
        log.info("msg2 deletedAt = {}", msg2.getDeletedAt());
        log.info("msg3 deletedAt = {}", msg3.getDeletedAt());

        List<ChatMessageEntity> result = chatMessageRepository
                .findByChatroomIdAndDeletedAtIsNullOrderByCreatedAtAsc(1L);
        log.info("조회된 메시지 수 = {}", result.size());

        // msg1, msg2만 조회, 시간 순서 확인
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getChatId()).isEqualTo(msg1.getChatId());
        assertThat(result.get(1).getRole()).isEqualTo("assistant");
    }

    /**
     * 다른 채팅방 메시지 미포함 테스트
     * - chatroomId가 다른 메시지는 조회 안 되어야 함
     */
    @Test
    @DisplayName("다른 채팅방 메시지 미포함 테스트")
    void testFindOnlyMyChatroomMessages() {
        // chatroomId 1번과 2번에 각각 메시지 저장
        chatMessageRepository.save(
                ChatMessageEntity.of(1L, "user",
                        "[TEST] ChatMessageRepositoryTest#testFindOnlyMyChatroomMessages - 1번방", 0));

        chatMessageRepository.save(
                ChatMessageEntity.of(2L, "user",
                        "[TEST] ChatMessageRepositoryTest#testFindOnlyMyChatroomMessages - 2번방", 0));

        // 1번 채팅방 메시지만 조회
        List<ChatMessageEntity> result = chatMessageRepository
                .findByChatroomIdAndDeletedAtIsNullOrderByCreatedAtAsc(1L);
        log.info("1번 채팅방 메시지 수 = {}", result.size());

        // 1번 채팅방 메시지만 1개
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChatroomId()).isEqualTo(1L);
    }
}