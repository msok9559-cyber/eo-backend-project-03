package com.example.prompt.service;

import com.example.prompt.client.AlanAiClient;
import com.example.prompt.domain.ChatMessageEntity;
import com.example.prompt.domain.ChatRoomEntity;
import com.example.prompt.dto.chat.ChatMessageDto;
import com.example.prompt.dto.chat.ChatRoomDto;
import com.example.prompt.repository.ChatMessageRepository;
import com.example.prompt.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final AlanAiClient alanAiClient;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    /**
     * 채팅방 생성
     * - 사용자 ID + 제목 + 모델명으로 chatroom 테이블에 INSERT
     */
    public ChatRoomDto.Response createChatRoom(Long userId, ChatRoomDto.Request dto) {
        ChatRoomEntity chatRoom = ChatRoomEntity.builder()
                .id(userId)
                .chatTitle(dto.getChatTitle())
                .model(dto.getModel())
                .build();

        log.info("채팅방 생성 userId = {}", userId);
        return ChatRoomDto.Response.from(chatRoomRepository.save(chatRoom));
    }

    /**
     * 채팅방 목록 조회
     * - 로그인한 사용자의 삭제 안 된 채팅방만, 최신순으로 반환
     */
    @Transactional(readOnly = true)
    public List<ChatRoomDto.Response> getChatRooms(Long userId) {
        return chatRoomRepository
                .findByIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .map(ChatRoomDto.Response::from)
                .collect(Collectors.toList());
    }

    /**
     * 메시지 목록 조회
     * - 특정 채팅방의 메시지를 시간 순서대로 반환 (대화 내역)
     */
    @Transactional(readOnly = true)
    public List<ChatMessageDto.Response> getMessages(Long chatroomId) {
        return chatMessageRepository
                .findByChatroomIdAndDeletedAtIsNullOrderByCreatedAtAsc(chatroomId)
                .stream()
                .map(ChatMessageDto.Response::from)
                .collect(Collectors.toList());
    }

    /**
     * SSE 스트리밍 메시지 전송 (핵심 기능!)
     * 1. 사용자 메시지 DB 저장
     * 2. 앨런 AI 에 SSE 스트리밍 요청
     * 3. 글자 chunk 올 때마다 → 브라우저로 실시간 전송
     * 4. 스트리밍 완료 시 → AI 응답 전체를 DB 저장
     */
    public SseEmitter streamMessage(Long chatroomId, String content) {
        // 3분 타임아웃 (앨런 AI 응답이 늦어질 경우 대비)
        SseEmitter emitter = new SseEmitter(180_000L);

        // 채팅방 존재 여부 확인
        chatRoomRepository.findById(chatroomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다"));

        // 1. 사용자 메시지 DB 저장 (role = "user")
        chatMessageRepository.save(
                ChatMessageEntity.of(chatroomId, "user", content, 0));

        // AI 응답 전체를 모으기 위한 StringBuilder
        StringBuilder fullResponse = new StringBuilder();

        // 2. 앨런 AI 스트리밍 호출
        alanAiClient.streamChat(content)
                .subscribe(
                        // chunk 도착할 때마다 실행
                        chunk -> {
                            try {
                                fullResponse.append(chunk);
                                // 3. 브라우저로 실시간 전송
                                emitter.send(SseEmitter.event().data(chunk));
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        // 에러 발생 시
                        error -> {
                            log.info("streaming error = {}", error.getMessage());
                            emitter.completeWithError(error);
                        },
                        // 스트리밍 완료 시
                        () -> {
                            String aiMessage = fullResponse.toString();
                            // 토큰 수: 앨런 AI 미제공 → 글자 수 / 4 로 추정
                            int tokensUsed = aiMessage.length() / 4;

                            // 4. AI 응답 전체 DB 저장 (role = "assistant")
                            chatMessageRepository.save(
                                    ChatMessageEntity.of(chatroomId, "assistant", aiMessage, tokensUsed));

                            log.info("스트리밍 완료 chatroomId = {}", chatroomId);
                            emitter.complete();
                        }
                );

        return emitter;
    }

    /**
     * 채팅방 제목 수정
     * - chatTitle 만 변경 (return this 체이닝)
     */
    public void updateTitle(Long chatroomId, String chatTitle) {
        ChatRoomEntity room = chatRoomRepository.findById(chatroomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다"));

        room.updateTitle(chatTitle);
        log.info("제목 수정 chatroomId = {}, chatTitle = {}", chatroomId, chatTitle);
    }

    /**
     * 채팅방 단건 삭제 (Soft Delete)
     * - DB에서 실제 삭제 X, deleted_at 에 현재 시간만 기록
     * - 앨런 AI 대화 기록도 같이 초기화
     */
    public void deleteChatRoom(Long chatroomId) {
        ChatRoomEntity room = chatRoomRepository.findById(chatroomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다"));

        room.delete();
        alanAiClient.resetState();

        log.info("채팅방 삭제 chatroomId = {}", chatroomId);
    }

    /**
     * 전체 채팅방 삭제 (Soft Delete)
     * - 해당 사용자의 모든 채팅방 삭제
     */
    public void deleteAllChatRooms(Long userId) {
        List<ChatRoomEntity> rooms = chatRoomRepository
                .findByIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);

        rooms.forEach(ChatRoomEntity::delete);
        alanAiClient.resetState();

        log.info("전체 채팅방 삭제 userId = {}", userId);
    }
}