package com.drrcp.resourceinventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
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
@Table(name = "resource_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceCategory category;

    @NotBlank
    @Column(nullable = false)
    private String unit;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer totalQuantity;

    @NotNull
    @Min(0)
    @Builder.Default
    @Column(nullable = false)
    private Integer reservedQuantity = 0;

    @Column
    private Long shelterId;

    @Version
    @Builder.Default
    @Column(nullable = false)
    private Long version = 0L;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime lastUpdated = LocalDateTime.now();

    @Transient
    public Integer getAvailableQuantity() {
        if (totalQuantity == null || reservedQuantity == null) {
            return 0;
        }
        return Math.max(totalQuantity - reservedQuantity, 0);
    }

    @PrePersist
    @PreUpdate
    public void updateLastUpdated() {
        lastUpdated = LocalDateTime.now();
    }
}