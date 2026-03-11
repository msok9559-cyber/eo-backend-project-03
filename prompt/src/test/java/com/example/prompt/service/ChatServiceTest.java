package com.example.prompt.service;

import com.example.prompt.client.AlanAiClient;
import com.example.prompt.domain.ChatRoomEntity;
import com.example.prompt.domain.PlanEntity;
import com.example.prompt.domain.UserEntity;
import com.example.prompt.dto.chat.ChatMessageDto;
import com.example.prompt.dto.chat.ChatRoomDto;
import com.example.prompt.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

    @Mock private AlanAiClient alanAiClient;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private UserRepository userRepository;
    @Mock private PlanModelRepository planModelRepository;

    private PlanEntity plan;
    private UserEntity user;
    private ChatRoomEntity chatRoom;

    @BeforeEach
    void setUp() {
        plan = PlanEntity.builder()
                .planId(1L)
                .planName("NORMAL")
                .tokenLimit(1000)
                .price(0)
                .build();

        user = UserEntity.builder()
                .id(1L)
                .userid("[TEST] ChatServiceTest#user")
                .username("테스터")
                .password("encoded_password")
                .email("test@test.com")
                .plan(plan)
                .usedToken(0)
                .build();

        chatRoom = ChatRoomEntity.builder()
                .chatroomId(1L)
                .id(1L)
                .chatTitle("테스트 채팅방")
                .model("alan-4.0")
                .build();
    }

    // createChatRoom

    @Test
    @DisplayName("채팅방 생성 - 성공")
    void testCreateChatRoom() {

        ChatRoomDto.Request request = ChatRoomDto.Request.builder()
                .chatTitle("[TEST] ChatServiceTest#testCreateChatRoom")
                .model("alan-4.0")
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(planModelRepository.existsByPlan_PlanIdAndModelName(1L, "alan-4.0")).willReturn(true);
        given(chatRoomRepository.save(any(ChatRoomEntity.class))).willReturn(chatRoom);

        log.info("채팅방 생성 요청 전 - userId = {}, model = {}", user.getId(), request.getModel());

        ChatRoomDto.Response response = chatService.createChatRoom(1L, request);

        log.info("채팅방 생성 완료 - chatroomId = {}", response.getChatroomId());

        assertThat(response).isNotNull();
        assertThat(response.getChatroomId()).isEqualTo(1L);
        assertThat(response.getModel()).isEqualTo("alan-4.0");
    }

    @Test
    @DisplayName("채팅방 생성 실패 - 사용자 없음")
    void testCreateChatRoom_UserNotFound() {

        ChatRoomDto.Request request = ChatRoomDto.Request.builder()
                .chatTitle("[TEST] ChatServiceTest#testCreateChatRoom_UserNotFound")
                .model("alan-4.0")
                .build();

        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.createChatRoom(99L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("채팅방 생성 실패 - 플랜에서 모델 사용 불가")
    void testCreateChatRoom_ModelNotAvailable() {

        ChatRoomDto.Request request = ChatRoomDto.Request.builder()
                .chatTitle("[TEST] ChatServiceTest#testCreateChatRoom_ModelNotAvailable")
                .model("alan-pro")
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(planModelRepository.existsByPlan_PlanIdAndModelName(1L, "alan-pro")).willReturn(false);

        log.info("모델 검증 요청 - planId = {}, model = {}", plan.getPlanId(), request.getModel());

        assertThatThrownBy(() -> chatService.createChatRoom(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("현재 플랜에서 사용할 수 없는 모델입니다: alan-pro");
    }

    // getChatRooms

    @Test
    @DisplayName("채팅방 목록 조회 - 성공")
    void testGetChatRooms() {

        given(chatRoomRepository.findByIdAndDeletedAtIsNullOrderByCreatedAtDesc(1L))
                .willReturn(List.of(chatRoom));

        log.info("채팅방 목록 조회 전 - userId = {}", 1L);

        List<ChatRoomDto.Response> result = chatService.getChatRooms(1L);

        log.info("채팅방 목록 조회 후 - 조회된 수 = {}", result.size());

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChatroomId()).isEqualTo(1L);
    }

    // getMessages

    @Test
    @DisplayName("메시지 목록 조회 - 성공")
    void testGetMessages() {

        given(chatMessageRepository.findByChatroomIdAndDeletedAtIsNullOrderByCreatedAtAsc(1L))
                .willReturn(List.of());

        List<ChatMessageDto.Response> result = chatService.getMessages(1L);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    // updateTitle

    @Test
    @DisplayName("채팅방 제목 수정 - 성공")
    void testUpdateTitle() {

        given(chatRoomRepository.findById(1L)).willReturn(Optional.of(chatRoom));

        log.info("제목 수정 전 - chatTitle = {}", chatRoom.getChatTitle());

        chatService.updateTitle(1L, 1L, "수정된 제목");

        log.info("제목 수정 후 - chatTitle = {}", chatRoom.getChatTitle());

        assertThat(chatRoom.getChatTitle()).isEqualTo("수정된 제목");
    }

    @Test
    @DisplayName("채팅방 제목 수정 실패 - 소유자 불일치")
    void testUpdateTitle_NotOwner() {

        given(chatRoomRepository.findById(1L)).willReturn(Optional.of(chatRoom));

        // chatRoom.id = 1L, 요청 userId = 99L → 소유자 불일치
        assertThatThrownBy(() -> chatService.updateTitle(1L, 99L, "해킹 시도"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 채팅방에 접근 권한이 없습니다");
    }

    // deleteChatRoom

    @Test
    @DisplayName("채팅방 단건 삭제 - 성공 (Soft Delete)")
    void testDeleteChatRoom() {

        given(chatRoomRepository.findById(1L)).willReturn(Optional.of(chatRoom));

        log.info("채팅방 삭제 전 - deletedAt = {}", chatRoom.getDeletedAt());

        chatService.deleteChatRoom(1L, 1L);

        log.info("채팅방 삭제 후 - deletedAt = {}", chatRoom.getDeletedAt());

        assertThat(chatRoom.getDeletedAt()).isNotNull();
        then(alanAiClient).should().resetState();
    }

    @Test
    @DisplayName("채팅방 삭제 실패 - 채팅방 없음")
    void testDeleteChatRoom_NotFound() {

        given(chatRoomRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.deleteChatRoom(99L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("채팅방을 찾을 수 없습니다");
    }

    // deleteAllChatRooms

    @Test
    @DisplayName("전체 채팅방 삭제 - 성공")
    void testDeleteAllChatRooms() {

        ChatRoomEntity room2 = ChatRoomEntity.builder()
                .chatroomId(2L).id(1L).chatTitle("두번째 방").model("alan-4.0").build();

        given(chatRoomRepository.findByIdAndDeletedAtIsNullOrderByCreatedAtDesc(1L))
                .willReturn(List.of(chatRoom, room2));

        log.info("전체 삭제 전 - 채팅방 수 = 2");

        chatService.deleteAllChatRooms(1L);

        log.info("전체 삭제 후 - deletedAt 설정 여부 확인");

        assertThat(chatRoom.getDeletedAt()).isNotNull();
        assertThat(room2.getDeletedAt()).isNotNull();
        then(alanAiClient).should().resetState();
    }

    // streamMessage - 토큰 한도 초과

    @Test
    @DisplayName("스트리밍 실패 - 토큰 한도 초과")
    void testStreamMessage_TokenLimitExceeded() {
        UserEntity exhaustedUser = UserEntity.builder()
                .id(1L)
                .userid("[TEST] ChatServiceTest#testStreamMessage_TokenLimitExceeded")
                .password("encoded")
                .email("test@test.com")
                .plan(plan)
                .usedToken(1000) // tokenLimit = 1000 → 한도 초과
                .build();

        given(chatRoomRepository.findById(1L)).willReturn(Optional.of(chatRoom));
        given(userRepository.findById(1L)).willReturn(Optional.of(exhaustedUser));

        log.info("토큰 한도 초과 테스트 - usedToken = {}, tokenLimit = {}",
                exhaustedUser.getUsedToken(), plan.getTokenLimit());

        assertThatThrownBy(() -> chatService.streamMessage(1L, 1L, "안녕하세요"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("토큰 한도를 초과했습니다. 플랜을 업그레이드 해주세요");
    }

    @Test
    @DisplayName("스트리밍 실패 - 채팅방 소유자 불일치")
    void testStreamMessage_NotOwner() {
        given(chatRoomRepository.findById(1L)).willReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> chatService.streamMessage(1L, 99L, "안녕하세요"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 채팅방에 접근 권한이 없습니다");
    }
}