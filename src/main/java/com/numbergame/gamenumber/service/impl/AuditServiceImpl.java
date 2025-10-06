package com.numbergame.gamenumber.service.impl;

import com.numbergame.gamenumber.entity.AuditLog;
import com.numbergame.gamenumber.enums.GameEventType;
import com.numbergame.gamenumber.repository.AuditLogRepository;
import com.numbergame.gamenumber.service.IAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements IAuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(Long userId, String username, GameEventType eventType, String description) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .eventType(eventType)
                    .description(description)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created: {} - {}", eventType, description);
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage(), e);
        }
    }

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEventWithMetadata(Long userId, String username, GameEventType eventType, String description, String metadata) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .eventType(eventType)
                    .description(description)
                    .metadata(metadata)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log with metadata created: {} - {}", eventType, description);
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage(), e);
        }
    }

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEventWithRequest(Long userId, String username, GameEventType eventType, String description, String ipAddress, String userAgent) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .eventType(eventType)
                    .description(description)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log with request info created: {} - {} from {}", eventType, description, ipAddress);
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage(), e);
        }
    }
}
