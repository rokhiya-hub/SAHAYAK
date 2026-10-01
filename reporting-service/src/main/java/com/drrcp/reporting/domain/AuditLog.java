package com.drrcp.reporting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String action;

    @Column(nullable = false, length = 120)
    private String performedBy;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 2000)
    private String details;

    public AuditLog(String action, String performedBy, boolean success, String details) {
        this.action = action;
        this.performedBy = performedBy;
        this.success = success;
        this.details = details;
    }

    @PrePersist
    public void setTimestamp() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}