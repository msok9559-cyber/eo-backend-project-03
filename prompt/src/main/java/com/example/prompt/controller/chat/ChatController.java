package com.example.prompt.controller.chat;

import com.example.prompt.dto.chat.ChatMessageDto;
import com.example.prompt.dto.chat.ChatRoomDto;
import com.example.prompt.security.CustomUserDetails;
import com.example.prompt.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 채팅방 생성
     * POST "http://localhost:8080/api/chat/rooms"
     * Body: { "chatTitle": "새 채팅", "model": "alan-4.0" }
     */
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomDto.Response> createRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid ChatRoomDto.Request dto
    ) {
        return ResponseEntity.ok(chatService.createChatRoom(userDetails.getId(), dto));
    }

    /**
     * 채팅방 목록 조회
     * GET "http://localhost:8080/api/chat/rooms"
     */
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDto.Response>> getRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(chatService.getChatRooms(userDetails.getId()));
    }

    /**
     * 메시지 목록 조회 (대화 내역)
     * GET "http://localhost:8080/api/chat/rooms/1/messages"
     */
    @GetMapping("/rooms/{chatroomId}/messages")
    public ResponseEntity<List<ChatMessageDto.Response>> getMessages(
            @PathVariable Long chatroomId
    ) {
        return ResponseEntity.ok(chatService.getMessages(chatroomId));
    }

    /**
     * SSE 스트리밍 — AI에게 질문하고 ChatGPT처럼 글자 하나씩 받기
     * GET "http://localhost:8080/api/chat/rooms/1/stream?content=안녕"
     */
    @GetMapping(value = "/rooms/{chatroomId}/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long chatroomId,
            @RequestParam String content
    ) {
        return chatService.streamMessage(chatroomId, userDetails.getId(), content);
    }

    /**
     * 채팅방 제목 수정
     * PATCH "http://localhost:8080/api/chat/rooms/1/title?chatTitle=수정된제목"
     */
    @PatchMapping("/rooms/{chatroomId}/title")
    public ResponseEntity<String> updateTitle(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long chatroomId,
            @RequestParam String chatTitle
    ) {
        chatService.updateTitle(chatroomId, userDetails.getId(), chatTitle);
        return ResponseEntity.ok("제목 수정 완료");
    }

    /**
     * 채팅방 단건 삭제
     * DELETE "http://localhost:8080/api/chat/rooms/1"
     */
    @DeleteMapping("/rooms/{chatroomId}")
    public ResponseEntity<String> deleteRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long chatroomId
    ) {
        chatService.deleteChatRoom(chatroomId, userDetails.getId());
        return ResponseEntity.ok("채팅방 삭제 완료");
    }

    /**
     * 전체 채팅방 삭제
     * DELETE "http://localhost:8080/api/chat/rooms/all"
     */
    @DeleteMapping("/rooms/all")
    public ResponseEntity<String> deleteAllRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        chatService.deleteAllChatRooms(userDetails.getId());
        return ResponseEntity.ok("전체 삭제 완료");
    }
}