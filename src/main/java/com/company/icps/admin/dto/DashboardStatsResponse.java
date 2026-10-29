package com.company.icps.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    private long totalUsers;
    private long totalClaims;

    // Core claim status breakdown
    private long approvedClaims;
    private long rejectedClaims;
    private long settledClaims; // approved + rejected
    private long pendingClaims;

    // Financial totals
    private BigDecimal totalClaimAmount;
    private BigDecimal approvedAmount;
    private BigDecimal rejectedAmount;
    private BigDecimal settledAmount;
    private BigDecimal pendingAmount;

    // Rates (percentage 0 - 100)
    private double approvalRate;
    private double rejectionRate;

    // Status map (retained for compatibility)
    private Map<String, Long> claimsByStatus;

    // Insurance Type breakdown
    private List<ClaimTypeStat> claimsByType;

    // Individual User & Role breakdown (Customer, Claim Agent, Investigator, Supervisor)
    private List<UserClaimStat> userStats;
}
