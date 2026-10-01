package com.drrcp.sheltermanagement.dto;

import com.drrcp.sheltermanagement.domain.Shelter;
import com.drrcp.sheltermanagement.domain.ShelterStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class ShelterResponse {
    private Long id;
    private String name;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer totalCapacity;
    private Integer currentOccupancy;
    private Integer availableCapacity;
    private ShelterStatus status;
    private List<String> resourcesAvailable;
    private Double distanceKm;

    public static ShelterResponse from(Shelter shelter) {
        return from(shelter, null);
    }

    public static ShelterResponse from(Shelter shelter, Double distanceKm) {
        return ShelterResponse.builder()
                .id(shelter.getId())
                .name(shelter.getName())
                .address(shelter.getAddress())
                .latitude(shelter.getLatitude())
                .longitude(shelter.getLongitude())
                .totalCapacity(shelter.getTotalCapacity())
                .currentOccupancy(shelter.getCurrentOccupancy())
                .availableCapacity(shelter.getAvailableCapacity())
                .status(shelter.getStatus())
                .resourcesAvailable(shelter.getResourcesAvailable())
                .distanceKm(distanceKm)
                .build();
    }
}
