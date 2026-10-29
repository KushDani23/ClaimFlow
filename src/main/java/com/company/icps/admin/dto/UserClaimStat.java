package com.company.icps.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserClaimStat {
    private Long userId;
    private String name;
    private String email;
    private String role;
    private long totalClaims;
    private long approvedClaims;
    private long rejectedClaims;
    private long pendingClaims;
    private BigDecimal totalAmount;
    private BigDecimal approvedAmount;
    private BigDecimal rejectedAmount;
}

