package com.example.prompt.repository;

import com.example.prompt.domain.ChatRoomEntity;
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
class ChatRoomRepositoryTest {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    /**
     * 채팅방 저장 테스트
     * - save() 후 PK 자동 생성 여부 확인
     */
    @Test
    @DisplayName("채팅방 저장 테스트")
    void testCreate() {
        ChatRoomEntity chatRoom = ChatRoomEntity.builder()
                .id(1L)
                .chatTitle("[TEST] ChatRoomRepositoryTest#testCreate")
                .model("alan-4.0")
                .build();

        log.info("저장 전 chatroomId = {}", chatRoom.getChatroomId());
        ChatRoomEntity saved = chatRoomRepository.save(chatRoom);
        log.info("저장 후 chatroomId = {}", saved.getChatroomId());

        assertThat(saved.getChatroomId()).isNotNull();
        assertThat(saved.getChatTitle()).isEqualTo("[TEST] ChatRoomRepositoryTest#testCreate");
        assertThat(saved.getModel()).isEqualTo("alan-4.0");
        assertThat(saved.getDeletedAt()).isNull();
    }

    /**
     * 채팅방 목록 조회 테스트
     * - 삭제된 채팅방은 제외되는지 확인
     */
    @Test
    @DisplayName("채팅방 목록 조회 - 삭제된 것 제외")
    void testFindByIdAndDeletedAtIsNull() {
        // 2개 저장 후 1개 Soft Delete
        ChatRoomEntity room1 = chatRoomRepository.save(ChatRoomEntity.builder()
                .id(1L)
                .chatTitle("[TEST] ChatRoomRepositoryTest#testFindByIdAndDeletedAtIsNull - 유지")
                .model("alan-4.0")
                .build());

        ChatRoomEntity room2 = chatRoomRepository.save(ChatRoomEntity.builder()
                .id(1L)
                .chatTitle("[TEST] ChatRoomRepositoryTest#testFindByIdAndDeletedAtIsNull - 삭제")
                .model("alan-4.0")
                .build());

        log.info("삭제 전 room2 deletedAt = {}", room2.getDeletedAt());
        room2.delete();
        chatRoomRepository.save(room2);
        log.info("삭제 후 room2 deletedAt = {}", room2.getDeletedAt());

        List<ChatRoomEntity> result = chatRoomRepository
                .findByIdAndDeletedAtIsNullOrderByCreatedAtDesc(1L);
        log.info("조회된 채팅방 수 = {}", result.size());

        // room1만 조회되어야 함
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChatroomId()).isEqualTo(room1.getChatroomId());
    }

    /**
     * 채팅방 제목 수정 테스트
     * - updateTitle() 후 DB에 반영되는지 확인
     */
    @Test
    @DisplayName("채팅방 제목 수정 테스트")
    void testUpdateTitle() {
        ChatRoomEntity chatRoom = chatRoomRepository.save(ChatRoomEntity.builder()
                .id(1L)
                .chatTitle("[TEST] ChatRoomRepositoryTest#testUpdateTitle - 수정 전")
                .model("alan-4.0")
                .build());

        log.info("수정 전 chatTitle = {}", chatRoom.getChatTitle());
        chatRoom.updateTitle("[TEST] ChatRoomRepositoryTest#testUpdateTitle - 수정 후");
        chatRoomRepository.save(chatRoom);
        log.info("수정 후 chatTitle = {}", chatRoom.getChatTitle());

        chatRoomRepository.findById(chatRoom.getChatroomId())
                .ifPresentOrElse(
                        found -> assertThat(found.getChatTitle())
                                .isEqualTo("[TEST] ChatRoomRepositoryTest#testUpdateTitle - 수정 후"),
                        () -> { throw new RuntimeException("채팅방이 없습니다"); }
                );
    }

    /**
     * 채팅방 Soft Delete 테스트
     * - delete() 후 deletedAt 이 채워지는지 확인
     * - DB 에서 실제로 삭제되지 않는지 확인
     */
    @Test
    @DisplayName("채팅방 Soft Delete 테스트")
    void testSoftDelete() {
        ChatRoomEntity chatRoom = chatRoomRepository.save(ChatRoomEntity.builder()
                .id(1L)
                .chatTitle("[TEST] ChatRoomRepositoryTest#testSoftDelete")
                .model("alan-4.0")
                .build());

        log.info("삭제 전 deletedAt = {}", chatRoom.getDeletedAt());
        chatRoom.delete();
        chatRoomRepository.save(chatRoom);
        log.info("삭제 후 deletedAt = {}", chatRoom.getDeletedAt());

        // 실제로 삭제 X, deletedAt 만 채워짐
        chatRoomRepository.findById(chatRoom.getChatroomId())
                .ifPresentOrElse(
                        found -> assertThat(found.getDeletedAt()).isNotNull(),
                        () -> { throw new RuntimeException("삭제되면 안됩니다"); }
                );
    }
}