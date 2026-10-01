package com.drrcp.volunteerdispatch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShelterResponse {
    private Long id;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer availableCapacity;
    private String status;
}