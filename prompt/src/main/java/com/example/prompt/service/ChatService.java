package com.example.prompt.service;

import com.example.prompt.client.AlanAiClient;
import com.example.prompt.domain.ChatMessageEntity;
import com.example.prompt.domain.ChatRoomEntity;
import com.example.prompt.domain.UserEntity;
import com.example.prompt.dto.chat.ChatMessageDto;
import com.example.prompt.dto.chat.ChatRoomDto;
import com.example.prompt.repository.ChatMessageRepository;
import com.example.prompt.repository.ChatRoomRepository;
import com.example.prompt.repository.PlanModelRepository;
import com.example.prompt.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final PlanModelRepository planModelRepository;

    /**
     * 채팅방 생성
     * - 사용자의 플랜에서 해당 모델 사용 가능 여부 검증 후 생성
     */
    public ChatRoomDto.Response createChatRoom(Long userId, ChatRoomDto.Request dto) {
        // 사용자 조회
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 플랜에서 해당 모델 사용 가능 여부 검증
        checkModelAvailable(user, dto.getModel());

        ChatRoomEntity chatRoom = ChatRoomEntity.builder()
                .id(userId)
                .chatTitle(dto.getChatTitle())
                .model(dto.getModel())
                .build();

        log.info("채팅방 생성 userId = {}, model = {}", userId, dto.getModel());
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
     * 1. 토큰 한도 초과 여부 확인
     * 2. 사용자 메시지 DB 저장
     * 3. 앨런 AI 에 SSE 스트리밍 요청
     * 4. 글자 chunk 올 때마다 → 브라우저로 실시간 전송
     * 5. 스트리밍 완료 시 → AI 응답 전체 DB 저장 + 토큰 차감
     */
    public SseEmitter streamMessage(Long chatroomId, Long userId, String content) {
        // 3분 타임아웃 (앨런 AI 응답이 늦어질 경우 대비)
        SseEmitter emitter = new SseEmitter(180_000L);

        // 채팅방 존재 여부 확인
        ChatRoomEntity chatRoom = chatRoomRepository.findById(chatroomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다"));

        // 채팅방 소유자 검증
        checkChatRoomOwner(chatRoom, userId);

        // 사용자 조회 및 토큰 한도 확인
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        checkTokenLimit(user);

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

                            // 5. 사용자 토큰 차감
                            user.setUsedToken(user.getUsedToken() + tokensUsed);
                            userRepository.save(user);

                            log.info("스트리밍 완료 chatroomId = {}, tokensUsed = {}", chatroomId, tokensUsed);
                            emitter.complete();
                        }
                );

        return emitter;
    }

    /**
     * 채팅방 제목 수정
     */
    public void updateTitle(Long chatroomId, Long userId, String chatTitle) {
        ChatRoomEntity room = chatRoomRepository.findById(chatroomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다"));

        checkChatRoomOwner(room, userId);
        room.updateTitle(chatTitle);
        log.info("제목 수정 chatroomId = {}, chatTitle = {}", chatroomId, chatTitle);
    }

    /**
     * 채팅방 단건 삭제 (Soft Delete)
     */
    public void deleteChatRoom(Long chatroomId, Long userId) {
        ChatRoomEntity room = chatRoomRepository.findById(chatroomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다"));

        checkChatRoomOwner(room, userId);
        room.delete();
        alanAiClient.resetState();

        log.info("채팅방 삭제 chatroomId = {}", chatroomId);
    }

    /**
     * 전체 채팅방 삭제 (Soft Delete)
     */
    public void deleteAllChatRooms(Long userId) {
        List<ChatRoomEntity> rooms = chatRoomRepository
                .findByIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);

        rooms.forEach(ChatRoomEntity::delete);
        alanAiClient.resetState();

        log.info("전체 채팅방 삭제 userId = {}", userId);
    }

    /**
     * 플랜에서 해당 모델 사용 가능 여부 검증
     */
    private void checkModelAvailable(UserEntity user, String modelName) {
        Long planId = user.getPlan().getPlanId();
        boolean available = planModelRepository.existsByPlan_PlanIdAndModelName(planId, modelName);
        if (!available) {
            log.warn("모델 사용 불가 - planId = {}, modelName = {}", planId, modelName);
            throw new IllegalArgumentException("현재 플랜에서 사용할 수 없는 모델입니다: " + modelName);
        }
        log.info("모델 사용 가능 확인 - planId = {}, modelName = {}", planId, modelName);
    }

    /**
     * 채팅방 소유자 검증
     */
    private void checkChatRoomOwner(ChatRoomEntity chatRoom, Long userId) {
        if (!chatRoom.getId().equals(userId)) {
            log.warn("채팅방 소유자 불일치 - chatroomId = {}, userId = {}", chatRoom.getChatroomId(), userId);
            throw new IllegalArgumentException("해당 채팅방에 접근 권한이 없습니다");
        }
    }

    /**
     * 토큰 한도 초과 여부 확인
     */
    private void checkTokenLimit(UserEntity user) {
        int tokenLimit = user.getPlan().getTokenLimit();
        int usedToken = user.getUsedToken();
        if (usedToken >= tokenLimit) {
            log.warn("토큰 한도 초과 - userId = {}, usedToken = {}, tokenLimit = {}", user.getId(), usedToken, tokenLimit);
            throw new IllegalArgumentException("토큰 한도를 초과했습니다. 플랜을 업그레이드 해주세요");
        }
    }
}