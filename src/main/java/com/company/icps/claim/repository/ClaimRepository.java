package com.company.icps.claim.repository;

import com.company.icps.claim.entity.Claim;
import com.company.icps.claim.entity.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long>, JpaSpecificationExecutor<Claim> {

    Page<Claim> findByCustomerId(Long customerId, Pageable pageable);

    Page<Claim> findByCustomerIdAndStatus(Long customerId, ClaimStatus status, Pageable pageable);

    Page<Claim> findByStatus(ClaimStatus status, Pageable pageable);

    Optional<Claim> findByClaimNumber(String claimNumber);

    long countByCustomerId(Long customerId);

    long countByStatus(ClaimStatus status);

    long countByCustomerIdAndStatus(Long customerId, ClaimStatus status);

    long countByAssignedAgentId(Long agentId);

    long countByAssignedAgentIdAndStatus(Long agentId, ClaimStatus status);

    Page<Claim> findByAssignedAgentId(Long agentId, Pageable pageable);

    java.util.List<Claim> findTop5ByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
