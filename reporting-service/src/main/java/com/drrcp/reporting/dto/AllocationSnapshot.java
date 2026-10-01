package com.drrcp.reporting.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AllocationSnapshot {
    private String resourceName;
    private Long shelterId;
    private Integer quantityAllocated;
    private LocalDateTime allocatedAt;
    private String status;
}