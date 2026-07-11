package com.company.icps.claim.repository;

import com.company.icps.claim.dto.ClaimSearchRequest;
import com.company.icps.claim.entity.Claim;
import org.springframework.data.jpa.domain.Specification;

public final class ClaimSpecifications {

    private ClaimSpecifications() { }

    public static Specification<Claim> matching(ClaimSearchRequest request) {
        return (root, query, builder) -> {
            var predicate = builder.conjunction();
            if (request.getStatus() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("status"), request.getStatus()));
            }
            if (request.getClaimType() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("claimType"), request.getClaimType()));
            }
            if (request.getFromDate() != null) {
                predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("incidentDate"), request.getFromDate()));
            }
            if (request.getToDate() != null) {
                predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("incidentDate"), request.getToDate()));
            }
            if (request.getCustomerName() != null && !request.getCustomerName().isBlank()) {
                String term = "%" + request.getCustomerName().trim().toLowerCase() + "%";
                var firstName = builder.lower(root.join("customer").get("firstName"));
                var lastName = builder.lower(root.join("customer").get("lastName"));
                predicate = builder.and(predicate, builder.or(builder.like(firstName, term), builder.like(lastName, term)));
            }
            if (request.getPolicyNumber() != null && !request.getPolicyNumber().isBlank()) {
                predicate = builder.and(predicate, builder.like(builder.lower(root.get("policyNumber")),
                        "%" + request.getPolicyNumber().trim().toLowerCase() + "%"));
            }
            return predicate;
        };
    }

    public static Specification<Claim> ownedBy(Long customerId) {
        return (root, query, builder) -> builder.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<Claim> assignedTo(Long agentId) {
        return (root, query, builder) -> builder.equal(root.get("assignedAgent").get("id"), agentId);
    }
}
