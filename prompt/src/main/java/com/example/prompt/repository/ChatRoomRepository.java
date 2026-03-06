package com.example.prompt.repository;

import com.example.prompt.domain.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, Long> {

    // 특정 사용자의 채팅방 목록 조회
    List<ChatRoomEntity> findByIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long id);
}