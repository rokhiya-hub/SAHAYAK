package com.drrcp.reporting.controller;

import com.drrcp.reporting.dto.AuditLogPageResponse;
import com.drrcp.reporting.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-log")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public AuditLogPageResponse getAuditLog(
            @PageableDefault(size = 25, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return AuditLogPageResponse.from(auditLogRepository.findAll(pageable));
    }
}