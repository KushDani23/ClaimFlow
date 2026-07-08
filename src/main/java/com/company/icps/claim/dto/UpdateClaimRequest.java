package com.company.icps.claim.dto;

import com.company.icps.claim.entity.ClaimType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateClaimRequest {

    private ClaimType claimType;

    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;

    @PastOrPresent(message = "Incident date cannot be in the future")
    private LocalDate incidentDate;

    @DecimalMin(value = "0.01", message = "Claim amount must be greater than zero")
    private BigDecimal claimAmount;

    private String policyNumber;
}
