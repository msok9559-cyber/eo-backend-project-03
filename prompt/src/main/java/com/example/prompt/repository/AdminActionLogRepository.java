package com.example.prompt.repository;

import com.example.prompt.domain.AdminActionLogEntity;
import com.example.prompt.dto.common.enums.AdminUserActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLogEntity, Long> {

    @Query("""
        select l
        from AdminActionLogEntity l
        where (:adminId = '' or l.adminId like concat('%', :adminId, '%'))
          and (:actionType = '' or l.actionType = :actionType)
          and (:startDateTime is null or l.createdAt >= :startDateTime)
          and (:endDateTime is null or l.createdAt <= :endDateTime)
        order by l.createdAt desc
    """)
    Page<AdminActionLogEntity> searchLogs(
            @Param("adminId") String adminId,
            @Param("actionType") String actionType,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            Pageable pageable
    );
}
