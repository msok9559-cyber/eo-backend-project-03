package com.example.prompt.controller.chat;

import com.example.prompt.domain.PlanEntity;
import com.example.prompt.domain.UserEntity;
import com.example.prompt.dto.chat.ChatMessageDto;
import com.example.prompt.dto.chat.ChatRoomDto;
import com.example.prompt.security.CustomUserDetails;
import com.example.prompt.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ChatService 만 Mock 으로 교체 (나머지는 실제 빈 사용)
    @MockitoBean
    private ChatService chatService;

    // 인증된 사용자 픽스처
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        PlanEntity plan = PlanEntity.builder()
                .planId(1L)
                .planName("NORMAL")
                .tokenLimit(1000)
                .price(0)
                .build();

        UserEntity mockUser = UserEntity.builder()
                .id(1L)
                .userid("[TEST] ChatControllerTest#user")
                .username("테스터")
                .password("encoded_password")
                .email("test@test.com")
                .plan(plan)
                .active(true)
                .locked(false)
                .build();

        userDetails = new CustomUserDetails(mockUser);
        log.info("테스트 사용자 설정 완료 - userId = {}", mockUser.getId());
    }

    // POST /api/chat/rooms - 채팅방 생성

    @Test
    @DisplayName("채팅방 생성 - 200 OK")
    void testCreateRoom() throws Exception {
        ChatRoomDto.Request request = ChatRoomDto.Request.builder()
                .chatTitle("[TEST] ChatControllerTest#testCreateRoom")
                .model("alan-4.0")
                .build();

        ChatRoomDto.Response response = ChatRoomDto.Response.builder()
                .chatroomId(1L)
                .chatTitle("[TEST] ChatControllerTest#testCreateRoom")
                .model("alan-4.0")
                .createdAt(LocalDateTime.now())
                .build();

        given(chatService.createChatRoom(any(), any(ChatRoomDto.Request.class)))
                .willReturn(response);

        log.info("채팅방 생성 요청 - POST /api/chat/rooms");

        mockMvc.perform(post("/api/chat/rooms")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatroomId").value(1L))
                .andExpect(jsonPath("$.model").value("alan-4.0"));

        log.info("채팅방 생성 응답 확인 완료");
    }

    @Test
    @DisplayName("채팅방 생성 실패 - model 없음 (400 Bad Request)")
    void testCreateRoom_ModelBlank() throws Exception {
        // model 미입력
        String requestJson = """
                {
                  "chatTitle": "[TEST] ChatControllerTest#testCreateRoom_ModelBlank",
                  "model": ""
                }
                """;

        log.info("유효성 검증 실패 테스트 - model 미입력");

        mockMvc.perform(post("/api/chat/rooms")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // GET /api/chat/rooms - 채팅방 목록 조회

    @Test
    @DisplayName("채팅방 목록 조회 - 200 OK")
    void testGetRooms() throws Exception {
        List<ChatRoomDto.Response> rooms = List.of(
                ChatRoomDto.Response.builder()
                        .chatroomId(1L).chatTitle("방1").model("alan-4.0")
                        .createdAt(LocalDateTime.now()).build(),
                ChatRoomDto.Response.builder()
                        .chatroomId(2L).chatTitle("방2").model("alan-4.0")
                        .createdAt(LocalDateTime.now()).build()
        );

        given(chatService.getChatRooms(any())).willReturn(rooms);

        log.info("채팅방 목록 조회 요청 - GET /api/chat/rooms");

        mockMvc.perform(get("/api/chat/rooms")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].chatroomId").value(1L))
                .andExpect(jsonPath("$[1].chatroomId").value(2L));

        log.info("채팅방 목록 응답 확인 완료");
    }

    // GET /api/chat/rooms/{chatroomId}/messages - 메시지 목록 조회

    @Test
    @DisplayName("메시지 목록 조회 - 200 OK")
    void testGetMessages() throws Exception {
        List<ChatMessageDto.Response> messages = List.of(
                ChatMessageDto.Response.builder()
                        .chatId(1L).role("user").content("안녕하세요")
                        .tokensUsed(0).createdAt(LocalDateTime.now()).build(),
                ChatMessageDto.Response.builder()
                        .chatId(2L).role("assistant").content("안녕하세요! 무엇을 도와드릴까요?")
                        .tokensUsed(10).createdAt(LocalDateTime.now()).build()
        );

        given(chatService.getMessages(1L)).willReturn(messages);

        log.info("메시지 목록 조회 요청 - GET /api/chat/rooms/1/messages");

        mockMvc.perform(get("/api/chat/rooms/1/messages")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("user"))
                .andExpect(jsonPath("$[1].role").value("assistant"));
    }

    // PATCH /api/chat/rooms/{chatroomId}/title - 제목 수정

    @Test
    @DisplayName("채팅방 제목 수정 - 200 OK")
    void testUpdateTitle() throws Exception {
        willDoNothing().given(chatService).updateTitle(any(), any(), any());

        log.info("제목 수정 요청 - PATCH /api/chat/rooms/1/title?chatTitle=수정된제목");

        mockMvc.perform(patch("/api/chat/rooms/1/title")
                        .with(user(userDetails))
                        .with(csrf())
                        .param("chatTitle", "수정된 제목"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("제목 수정 완료"));
    }

    // DELETE /api/chat/rooms/{chatroomId} - 단건 삭제

    @Test
    @DisplayName("채팅방 단건 삭제 - 200 OK")
    void testDeleteRoom() throws Exception {
        willDoNothing().given(chatService).deleteChatRoom(any(), any());

        log.info("채팅방 삭제 요청 - DELETE /api/chat/rooms/1");

        mockMvc.perform(delete("/api/chat/rooms/1")
                        .with(user(userDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("채팅방 삭제 완료"));
    }

    // DELETE /api/chat/rooms/all - 전체 삭제

    @Test
    @DisplayName("전체 채팅방 삭제 - 200 OK")
    void testDeleteAllRooms() throws Exception {
        willDoNothing().given(chatService).deleteAllChatRooms(any());

        log.info("전체 채팅방 삭제 요청 - DELETE /api/chat/rooms/all");

        mockMvc.perform(delete("/api/chat/rooms/all")
                        .with(user(userDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("전체 삭제 완료"));
    }
}