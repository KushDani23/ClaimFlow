package com.company.icps.dashboard.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SupervisorDashboardResponse {
    long pendingApprovals;
    long approvedClaims;
    long rejectedClaims;
}
