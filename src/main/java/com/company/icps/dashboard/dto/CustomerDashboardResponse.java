package com.company.icps.dashboard.dto;

import com.company.icps.claim.dto.ClaimResponse;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CustomerDashboardResponse {
    long totalClaims;
    long pendingClaims;
    long approvedClaims;
    long rejectedClaims;
    List<ClaimResponse> recentClaims;
}
