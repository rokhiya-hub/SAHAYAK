package com.drrcp.volunteerdispatch.dto;

import com.drrcp.volunteerdispatch.domain.AvailabilityStatus;
import com.drrcp.volunteerdispatch.domain.Volunteer;
import com.drrcp.volunteerdispatch.domain.VolunteerSkill;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Set;

@Getter
@Builder
public class VolunteerResponse {
    private Long id;
    private String name;
    private String phone;
    private Set<VolunteerSkill> skills;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private AvailabilityStatus availabilityStatus;
    private Long version;

    public static VolunteerResponse from(Volunteer volunteer) {
        return VolunteerResponse.builder()
                .id(volunteer.getId())
                .name(volunteer.getName())
                .phone(volunteer.getPhone())
                .skills(Set.copyOf(volunteer.getSkills()))
                .latitude(volunteer.getLatitude())
                .longitude(volunteer.getLongitude())
                .availabilityStatus(volunteer.getAvailabilityStatus())
                .version(volunteer.getVersion())
                .build();
    }
}