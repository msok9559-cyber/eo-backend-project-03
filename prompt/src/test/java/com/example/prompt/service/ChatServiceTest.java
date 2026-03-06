package com.example.prompt.service;

import com.example.prompt.client.AlanAiClient;
import com.example.prompt.domain.ChatRoomEntity;
import com.example.prompt.dto.chat.ChatRoomDto;
import com.example.prompt.repository.ChatRoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@Transactional
@Slf4j
class ChatServiceTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @MockitoBean
    private AlanAiClient alanAiClient;

    private static final Long TEST_USER_ID = 1L;

    // 테스트용 Request 생성 헬퍼
    private ChatRoomDto.Request buildRequest(String title) {
        return ChatRoomDto.Request.builder()
                .chatTitle(title)
                .model("alan-4.0")
                .build();
    }

    @Test
    void testCreateChatRoom() {
        ChatRoomDto.Request request = buildRequest("[TEST] ChatServiceTest#testCreateChatRoom");

        ChatRoomDto.Response response = chatService.createChatRoom(TEST_USER_ID, request);
        log.info("생성된 chatroomId = {}", response.getChatroomId());

        assertThat(response).isNotNull();
        assertThat(response.getChatTitle()).isEqualTo("[TEST] ChatServiceTest#testCreateChatRoom");
        assertThat(response.getModel()).isEqualTo("alan-4.0");
    }

    @Test
    void testGetChatRooms() {
        chatService.createChatRoom(TEST_USER_ID, buildRequest("[TEST] ChatServiceTest#testGetChatRooms-1"));
        chatService.createChatRoom(TEST_USER_ID, buildRequest("[TEST] ChatServiceTest#testGetChatRooms-2"));

        List<ChatRoomDto.Response> list = chatService.getChatRooms(TEST_USER_ID);
        log.info("채팅방 수 = {}", list.size());

        assertThat(list).hasSize(2);
    }

    @Test
    void testUpdateTitle() {
        ChatRoomDto.Response created = chatService.createChatRoom(TEST_USER_ID,
                buildRequest("[TEST] ChatServiceTest#testUpdateTitle"));

        chatService.updateTitle(created.getChatroomId(), "수정된 제목");

        ChatRoomEntity updated = chatRoomRepository.findById(created.getChatroomId()).orElseThrow();
        log.info("수정 후 제목 = {}", updated.getChatTitle());

        assertThat(updated.getChatTitle()).isEqualTo("수정된 제목");
    }

    @Test
    void testDeleteChatRoom() {
        ChatRoomDto.Response created = chatService.createChatRoom(TEST_USER_ID,
                buildRequest("[TEST] ChatServiceTest#testDeleteChatRoom"));

        doNothing().when(alanAiClient).resetState();
        chatService.deleteChatRoom(created.getChatroomId());

        List<ChatRoomDto.Response> list = chatService.getChatRooms(TEST_USER_ID);
        log.info("삭제 후 채팅방 수 = {}", list.size());

        assertThat(list).isEmpty();
    }

    @Test
    void testDeleteAllChatRooms() {
        chatService.createChatRoom(TEST_USER_ID, buildRequest("[TEST] ChatServiceTest#testDeleteAllChatRooms-1"));
        chatService.createChatRoom(TEST_USER_ID, buildRequest("[TEST] ChatServiceTest#testDeleteAllChatRooms-2"));

        doNothing().when(alanAiClient).resetState();
        chatService.deleteAllChatRooms(TEST_USER_ID);

        List<ChatRoomDto.Response> list = chatService.getChatRooms(TEST_USER_ID);
        log.info("전체 삭제 후 채팅방 수 = {}", list.size());

        assertThat(list).isEmpty();
    }
}