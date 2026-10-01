package com.drrcp.victimregistration.dto;

import com.drrcp.victimregistration.domain.MedicalNeeds;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VictimRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Aadhaar number is required")
    @Pattern(regexp = "^\\d{12}$", message = "Aadhaar number must contain exactly 12 digits")
    private String aadharNumber;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone number must be a valid 10-digit Indian number")
    private String phoneNumber;

    @NotNull(message = "Age is required")
    @Min(value = 0, message = "Age cannot be negative")
    private Integer age;

    @NotNull(message = "Family size is required")
    @Min(value = 1, message = "Family size must be at least 1")
    private Integer familySize;

    @NotNull(message = "Medical needs are required")
    private MedicalNeeds medicalNeeds;
}