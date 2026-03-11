package com.example.prompt.repository;

import com.example.prompt.domain.AdminActionLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLogEntity, Long> {

    Page<AdminActionLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
