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
public class ClaimTypeStat {
    private String type;
    private long totalClaims;
    private long approvedClaims;
    private long rejectedClaims;
    private BigDecimal totalAmount;
    private BigDecimal approvedAmount;
}

