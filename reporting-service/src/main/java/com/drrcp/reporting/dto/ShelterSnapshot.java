package com.drrcp.reporting.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShelterSnapshot {
    private Long id;
    private String name;
    private String address;
    private Integer currentOccupancy;
    private String status;
}