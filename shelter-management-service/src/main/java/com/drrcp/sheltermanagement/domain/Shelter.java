package com.drrcp.sheltermanagement.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shelters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shelter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false)
    private String address;

    @NotNull
    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @NotNull
    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer totalCapacity;

    @Builder.Default
    @Column(nullable = false)
    private Integer currentOccupancy = 0;

    @Version
    @Builder.Default
    @Column(nullable = false)
    private Long version = 0L;

    @ElementCollection
    @CollectionTable(name = "shelter_resources", joinColumns = @JoinColumn(name = "shelter_id"))
    @Column(name = "resource_name")
    @Builder.Default
    private List<String> resourcesAvailable = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ShelterStatus status = ShelterStatus.OPEN;

    @Transient
    public Integer getAvailableCapacity() {
        if (totalCapacity == null || currentOccupancy == null) {
            return 0;
        }
        return Math.max(totalCapacity - currentOccupancy, 0);
    }

    @PrePersist
    @PreUpdate
    public void syncStatus() {
        if (status == ShelterStatus.CLOSED) {
            return;
        }
        this.status = getAvailableCapacity() <= 0 ? ShelterStatus.FULL : ShelterStatus.OPEN;
    }
}
