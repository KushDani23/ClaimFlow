package com.company.icps.claim.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimResponse {

    private Long id;
    private String claimNumber;
    private String claimType;
    private String status;
    private String description;
    private LocalDate incidentDate;
    private BigDecimal claimAmount;
    private String policyNumber;
    private String customerName;
    private String customerEmail;
    private String assignedAgent;
    private String agentNotes;
    private String investigationNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
