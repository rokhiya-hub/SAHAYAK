package com.drrcp.victimregistration.dto;

import com.drrcp.victimregistration.domain.MedicalNeeds;
import com.drrcp.victimregistration.domain.Victim;
import com.drrcp.victimregistration.domain.VictimStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class VictimResponse {
    private Long id;
    private String fullName;
    private String aadharNumber;
    private String phoneNumber;
    private Integer age;
    private Integer familySize;
    private MedicalNeeds medicalNeeds;
    private Long currentShelterId;
    private VictimStatus status;
    private LocalDateTime registeredAt;
    private Long version;

    public static VictimResponse from(Victim victim) {
        return VictimResponse.builder()
                .id(victim.getId())
                .fullName(victim.getFullName())
                .aadharNumber(victim.getAadharNumber())
                .phoneNumber(victim.getPhoneNumber())
                .age(victim.getAge())
                .familySize(victim.getFamilySize())
                .medicalNeeds(victim.getMedicalNeeds())
                .currentShelterId(victim.getCurrentShelterId())
                .status(victim.getStatus())
                .registeredAt(victim.getRegisteredAt())
                .version(victim.getVersion())
                .build();
    }
}