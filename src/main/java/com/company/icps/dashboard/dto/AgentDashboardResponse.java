package com.company.icps.dashboard.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgentDashboardResponse {
    long assignedClaims;
    long pendingReviews;
    long activeInvestigations;
}
