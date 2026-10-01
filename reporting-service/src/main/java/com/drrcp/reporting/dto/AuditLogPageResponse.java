package com.drrcp.reporting.dto;

import com.drrcp.reporting.domain.AuditLog;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AuditLogPageResponse {
    private List<AuditLogItem> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static AuditLogPageResponse from(Page<AuditLog> page) {
        return AuditLogPageResponse.builder()
                .content(page.getContent().stream().map(AuditLogItem::from).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Getter
    @Builder
    public static class AuditLogItem {
        private Long id;
        private String action;
        private String performedBy;
        private LocalDateTime timestamp;
        private boolean success;
        private String details;

        private static AuditLogItem from(AuditLog log) {
            return AuditLogItem.builder()
                    .id(log.getId())
                    .action(log.getAction())
                    .performedBy(log.getPerformedBy())
                    .timestamp(log.getTimestamp())
                    .success(log.isSuccess())
                    .details(log.getDetails())
                    .build();
        }
    }
}