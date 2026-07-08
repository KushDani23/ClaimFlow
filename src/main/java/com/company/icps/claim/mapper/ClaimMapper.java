package com.company.icps.claim.mapper;

import com.company.icps.claim.dto.ClaimResponse;
import com.company.icps.claim.entity.Claim;
import com.company.icps.user.entity.User;

public final class ClaimMapper {

    private ClaimMapper() {
        // Prevent instantiation
    }

    /**
     * Convert a Claim entity to a ClaimResponse DTO.
     */
    public static ClaimResponse toResponse(Claim claim) {
        User customer = claim.getCustomer();
        return ClaimResponse.builder()
                .id(claim.getId())
                .claimNumber(claim.getClaimNumber())
                .claimType(claim.getClaimType().name())
                .status(claim.getStatus().name())
                .description(claim.getDescription())
                .incidentDate(claim.getIncidentDate())
                .claimAmount(claim.getClaimAmount())
                .policyNumber(claim.getPolicyNumber())
                .customerName(customer.getFirstName() + " " + customer.getLastName())
                .customerEmail(customer.getEmail())
                .createdAt(claim.getCreatedAt())
                .updatedAt(claim.getUpdatedAt())
                .build();
    }
}
