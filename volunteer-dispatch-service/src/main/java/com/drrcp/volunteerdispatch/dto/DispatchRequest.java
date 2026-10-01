package com.drrcp.volunteerdispatch.dto;

import com.drrcp.volunteerdispatch.domain.VolunteerSkill;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DispatchRequest {

    @NotNull(message = "Volunteer id is required")
    private Long volunteerId;

    @NotNull(message = "Shelter id is required")
    private Long shelterId;

    @NotNull(message = "Required skill is required")
    private VolunteerSkill requiredSkill;
}