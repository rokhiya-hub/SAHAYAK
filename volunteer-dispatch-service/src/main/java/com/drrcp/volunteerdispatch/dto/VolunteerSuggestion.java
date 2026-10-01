package com.drrcp.volunteerdispatch.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VolunteerSuggestion {
    private VolunteerResponse volunteer;
    private double distanceKm;
    private double score;
}