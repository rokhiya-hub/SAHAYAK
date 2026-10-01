package com.drrcp.victimregistration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "victims")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Victim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Full name is required")
    @Column(nullable = false)
    private String fullName;

    @NotBlank(message = "Aadhaar number is required")
    @Pattern(regexp = "^\\d{12}$", message = "Aadhaar number must contain exactly 12 digits")
    @Column(nullable = false, unique = true, length = 12)
    private String aadharNumber;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone number must be a valid 10-digit Indian number")
    @Column(nullable = false, length = 10)
    private String phoneNumber;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer age;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer familySize;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MedicalNeeds medicalNeeds;

    @Column
    private Long currentShelterId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private VictimStatus status = VictimStatus.REGISTERED;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime registeredAt = LocalDateTime.now();

    @Version
    @Builder.Default
    @Column(nullable = false)
    private Long version = 0L;

    @PrePersist
    public void setRegisteredAt() {
        if (registeredAt == null) {
            registeredAt = LocalDateTime.now();
        }
    }
}