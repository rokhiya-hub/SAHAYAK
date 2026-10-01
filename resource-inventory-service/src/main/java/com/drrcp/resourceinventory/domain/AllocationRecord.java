package com.drrcp.resourceinventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "allocation_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_item_id", nullable = false)
    private ResourceItem resourceItem;

    @NotNull
    @Column(nullable = false)
    private Long shelterId;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer quantityAllocated;

    @NotBlank
    @Column(nullable = false)
    private String requestedBy;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AllocationStatus status;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime allocatedAt = LocalDateTime.now();

    @Column
    private String rejectionReason;

    @PrePersist
    public void setAllocatedAt() {
        if (allocatedAt == null) {
            allocatedAt = LocalDateTime.now();
        }
    }
}