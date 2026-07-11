package com.company.icps.claim.dto;

import com.company.icps.claim.entity.ClaimStatus;
import com.company.icps.claim.entity.ClaimType;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class ClaimSearchRequest {
    ClaimStatus status;
    ClaimType claimType;
    LocalDate fromDate;
    LocalDate toDate;
    String customerName;
    String policyNumber;
}
