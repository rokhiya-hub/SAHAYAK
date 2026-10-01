package com.drrcp.volunteerdispatch.dto;

import com.drrcp.volunteerdispatch.domain.DispatchAssignment;
import com.drrcp.volunteerdispatch.domain.DispatchStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DispatchResponse {
    private Long id;
    private Long volunteerId;
    private Long shelterId;
    private String task;
    private LocalDateTime assignedAt;
    private DispatchStatus status;

    public static DispatchResponse from(DispatchAssignment assignment) {
        return DispatchResponse.builder()
                .id(assignment.getId())
                .volunteerId(assignment.getVolunteer().getId())
                .shelterId(assignment.getShelterId())
                .task(assignment.getTask())
                .assignedAt(assignment.getAssignedAt())
                .status(assignment.getStatus())
                .build();
    }
}