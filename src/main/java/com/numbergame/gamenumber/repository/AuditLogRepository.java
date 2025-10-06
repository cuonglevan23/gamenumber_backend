package com.numbergame.gamenumber.repository;

import com.numbergame.gamenumber.entity.AuditLog;
import com.numbergame.gamenumber.enums.GameEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<AuditLog> findByEventTypeOrderByCreatedAtDesc(GameEventType eventType);

    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.eventType = :eventType ORDER BY a.createdAt DESC")
    List<AuditLog> findByUserIdAndEventType(@Param("userId") Long userId, @Param("eventType") GameEventType eventType);

    @Query("SELECT a FROM AuditLog a WHERE a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentLogs(@Param("since") LocalDateTime since);
}

