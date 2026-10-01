package com.drrcp.reporting.service;

import com.drrcp.reporting.domain.AuditLog;
import com.drrcp.reporting.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogWriter {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(String action, String performedBy, boolean success, String details) {
        auditLogRepository.save(new AuditLog(action, performedBy, success, details));
    }
}