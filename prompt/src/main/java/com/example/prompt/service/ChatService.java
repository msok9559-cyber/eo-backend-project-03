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
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final AlanAiClient alanAiClient;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final PlanModelRepository planModelRepository;

    // Alan AI continue 청크 형식: {'type': 'continue', 'data': {'content': '텍스트'}}
    private static final String CONTENT_MARKER = "'content': '";

    /**
     * 채팅방 생성
     * - 사용자의 플랜에서 해당 모델 사용 가능 여부 검증 후 생성
     */
    @Transactional
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
     * SSE 스트리밍 메시지 전송
     * 1. 토큰 한도 초과 여부 확인
     * 2. 사용자 메시지 DB 저장
     * 3. 앨런 AI 에 SSE 스트리밍 요청
     * 4. chunk 도착마다 브라우저로 실시간 전송
     * 5. 완료 시 순수 텍스트만 DB 저장 + 토큰 차감
     * 6. 차감 후 한도 도달 시 token-exhausted 이벤트 전송
     */
    @Transactional
    public SseEmitter streamMessage(Long chatroomId, Long userId, String content) {
        // 3분 타임아웃 (앨런 AI 응답이 늦어질 경우 대비)
        SseEmitter emitter = new SseEmitter(180_000L);

        // 채팅방 존재 여부 + 소유자 검증
        ChatRoomEntity chatRoom = chatRoomRepository.findById(chatroomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다"));

        // 채팅방 소유자 검증
        checkChatRoomOwner(chatRoom, userId);

        // 사용자 조회 + 토큰 한도 확인
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        checkTokenLimit(user);

        // 사용자 메시지 DB 저장
        chatMessageRepository.save(ChatMessageEntity.of(chatroomId, "user", content, 0));

        // 트랜잭션 안에서 필요한 값 미리 추출 (메서드 리턴 후 세션 종료 대비)
        String model = chatRoom.getModel();
        int tokenLimit = user.getPlan().getTokenLimit();

        log.info("스트리밍 시작 - chatroomId = {}, userId = {}, model = {}", chatroomId, userId, model);

        // cleanResponse: continue 청크에서 추출한 순수 텍스트 누적 → DB 저장용
        StringBuilder cleanResponse = new StringBuilder();

        // 앨런 AI 스트리밍 호출
        alanAiClient.streamChat(content)
                .subscribe(
                        // chunk 도착할 때마다 실행
                        chunk -> {
                            try {
                                // 브라우저에는 raw chunk 그대로 전송
                                emitter.send(SseEmitter.event().data(chunk));
                                // DB 저장용으로는 순수 텍스트만 추출해서 누적
                                String text = extractContinueContent(chunk);
                                if (text != null) cleanResponse.append(text);
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
                            String aiMessage = cleanResponse.toString();
                            double multiplier = getTokenMultiplier(model);
                            int tokensUsed = (int) ((content.length() + aiMessage.length()) / 4 * multiplier);

                            boolean exhausted = saveStreamResult(chatroomId, userId, aiMessage, tokensUsed, tokenLimit);

                            log.info("스트리밍 완료 chatroomId = {}, tokensUsed = {}", chatroomId, tokensUsed);

                            if (exhausted) {
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("token-exhausted")
                                            .data("토큰 한도에 도달했습니다. 플랜을 업그레이드 해주세요."));
                                    log.warn("토큰 한도 도달 - userId = {}", userId);
                                } catch (IOException e) {
                                    log.warn("token-exhausted 이벤트 전송 실패 = {}", e.getMessage());
                                }
                            }

                            emitter.complete();
                        }
                );

        return emitter;
    }

    /**
     * 채팅방 제목 수정
     */
    @Transactional
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
    @Transactional
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
    @Transactional
    public void deleteAllChatRooms(Long userId) {
        List<ChatRoomEntity> rooms = chatRoomRepository
                .findByIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);

        rooms.forEach(ChatRoomEntity::delete);
        alanAiClient.resetState();

        log.info("전체 채팅방 삭제 userId = {}", userId);
    }

    /**
     * Alan AI continue 청크에서 순수 텍스트 추출
     *
     * 청크 형식: {'type': 'continue', 'data': {'content': '텍스트'}}
     * - continue 타입만 처리 (complete 타입은 무시 - continue 누적으로 전체 텍스트 완성됨)
     * - continue 청크는 1~5자 단위의 짧은 조각이므로 apostrophe 파싱 문제 없음
     */
    private String extractContinueContent(String chunk) {
        if (chunk == null || chunk.isBlank()) return null;

        // continue 타입이 아니면 무시
        if (!chunk.contains("'continue'")) return null;

        // 'content': '텍스트'} 패턴에서 텍스트 추출
        int markerIdx = chunk.indexOf(CONTENT_MARKER);
        if (markerIdx == -1) return null;

        int start = markerIdx + CONTENT_MARKER.length();
        if (start >= chunk.length()) return null;

        // '} 패턴을 역탐색해서 닫는 위치 결정
        // 청크 끝부분: ...'텍스트'}} 이므로 lastIndexOf("'}") 로 찾을 수 있음
        int end = chunk.lastIndexOf("'}");
        if (end <= start) return "";

        return chunk.substring(start, end);
    }

    /**
     * 모델별 토큰 차감 배율
     * alan-4-turbo: 2.0배 (고성능 모델)
     * alan-4.1:     1.5배 (중간 모델)
     * alan-4.0:     1.0배 (기본 모델)
     */
    private double getTokenMultiplier(String modelName) {
        if (modelName == null) return 1.0;
        return switch (modelName) {
            case "alan-4-turbo" -> 2.0;
            case "alan-4.1"     -> 1.5;
            default             -> 1.0;
        };
    }

    /**
     * 스트리밍 완료 후 DB 저장 + 토큰 차감
     * reactor 스레드에서 호출 - 각 Repository 메서드가 자체 트랜잭션으로 처리
     * @return 토큰 한도 도달 여부
     */
    public boolean saveStreamResult(Long chatroomId, Long userId, String aiMessage, int tokensUsed, int tokenLimit) {
        chatMessageRepository.save(
                ChatMessageEntity.of(chatroomId, "assistant", aiMessage, tokensUsed));

        UserEntity freshUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        int newUsed = Math.min(freshUser.getUsedToken() + tokensUsed, tokenLimit);
        freshUser.setUsedToken(newUsed);
        userRepository.save(freshUser);

        log.info("토큰 차감 완료 - userId = {}, tokensUsed = {}, usedToken = {}/{}", userId, tokensUsed, newUsed, tokenLimit);
        return newUsed >= tokenLimit;
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
        int usedToken  = user.getUsedToken();
        if (usedToken >= tokenLimit) {
            log.warn("토큰 한도 초과 - userId = {}, usedToken = {}, tokenLimit = {}", user.getId(), usedToken, tokenLimit);
            throw new IllegalArgumentException("토큰 한도를 초과했습니다. 플랜을 업그레이드 해주세요");
        }
    }
}