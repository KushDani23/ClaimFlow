package com.company.icps.claim.service;

import com.company.icps.claim.dto.ClaimResponse;
import com.company.icps.claim.dto.CreateClaimRequest;
import com.company.icps.claim.dto.UpdateClaimRequest;
import com.company.icps.claim.entity.Claim;
import com.company.icps.claim.entity.ClaimStatus;
import com.company.icps.claim.mapper.ClaimMapper;
import com.company.icps.claim.repository.ClaimRepository;
import com.company.icps.common.exception.InvalidStateTransitionException;
import com.company.icps.common.exception.ResourceNotFoundException;
import com.company.icps.user.entity.User;
import com.company.icps.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;

    /**
     * Create a new claim in DRAFT status.
     */
    @Transactional
    public ClaimResponse createClaim(CreateClaimRequest request, String email) {
        User customer = getUserByEmail(email);

        String claimNumber = generateClaimNumber(customer.getId());

        Claim claim = Claim.builder()
                .claimNumber(claimNumber)
                .claimType(request.getClaimType())
                .status(ClaimStatus.DRAFT)
                .description(request.getDescription())
                .incidentDate(request.getIncidentDate())
                .claimAmount(request.getClaimAmount())
                .policyNumber(request.getPolicyNumber())
                .customer(customer)
                .build();

        Claim savedClaim = claimRepository.save(claim);
        return ClaimMapper.toResponse(savedClaim);
    }

    /**
     * Get a claim by ID — only accessible by the claim owner.
     */
    @Transactional(readOnly = true)
    public ClaimResponse getClaimById(Long claimId, String email) {
        Claim claim = getClaimEntity(claimId);
        validateOwnership(claim, email);
        return ClaimMapper.toResponse(claim);
    }

    /**
     * Get all claims for the authenticated customer (paginated).
     */
    @Transactional(readOnly = true)
    public Page<ClaimResponse> getMyClaims(String email, Pageable pageable) {
        User customer = getUserByEmail(email);
        Page<Claim> claims = claimRepository.findByCustomerId(customer.getId(), pageable);
        return claims.map(ClaimMapper::toResponse);
    }

    /**
     * Update a claim — only DRAFT claims owned by the customer.
     */
    @Transactional
    public ClaimResponse updateDraftClaim(Long claimId, UpdateClaimRequest request, String email) {
        Claim claim = getClaimEntity(claimId);
        validateOwnership(claim, email);
        validateDraftStatus(claim);

        // Update only provided fields
        if (request.getClaimType() != null) {
            claim.setClaimType(request.getClaimType());
        }
        if (request.getDescription() != null) {
            claim.setDescription(request.getDescription());
        }
        if (request.getIncidentDate() != null) {
            claim.setIncidentDate(request.getIncidentDate());
        }
        if (request.getClaimAmount() != null) {
            claim.setClaimAmount(request.getClaimAmount());
        }
        if (request.getPolicyNumber() != null) {
            claim.setPolicyNumber(request.getPolicyNumber());
        }

        Claim updatedClaim = claimRepository.save(claim);
        return ClaimMapper.toResponse(updatedClaim);
    }

    /**
     * Delete a claim — only DRAFT claims owned by the customer.
     */
    @Transactional
    public void deleteDraftClaim(Long claimId, String email) {
        Claim claim = getClaimEntity(claimId);
        validateOwnership(claim, email);
        validateDraftStatus(claim);

        claimRepository.delete(claim);
    }

    /**
     * Submit a claim — transitions from DRAFT to SUBMITTED.
     */
    @Transactional
    public ClaimResponse submitClaim(Long claimId, String email) {
        Claim claim = getClaimEntity(claimId);
        validateOwnership(claim, email);

        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new InvalidStateTransitionException(
                    claim.getStatus().name(), ClaimStatus.SUBMITTED.name()
            );
        }

        claim.setStatus(ClaimStatus.SUBMITTED);
        Claim submittedClaim = claimRepository.save(claim);
        return ClaimMapper.toResponse(submittedClaim);
    }

    // ---- Helper Methods ----

    /**
     * Fetch claim entity or throw 404.
     */
    public Claim getClaimEntity(Long claimId) {
        return claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "id", claimId));
    }

    /**
     * Verify the authenticated user is the claim owner.
     */
    private void validateOwnership(Claim claim, String email) {
        if (!claim.getCustomer().getEmail().equals(email)) {
            throw new AccessDeniedException("You do not have permission to access this claim");
        }
    }

    /**
     * Verify the claim is still in DRAFT status.
     */
    private void validateDraftStatus(Claim claim) {
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new InvalidStateTransitionException(
                    "Only DRAFT claims can be modified. Current status: " + claim.getStatus().name()
            );
        }
    }

    /**
     * Fetch user by email or throw 404.
     */
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    /**
     * Generate a unique claim number in format: CLM-yyyyMMdd-XXXXX
     */
    private String generateClaimNumber(Long customerId) {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = claimRepository.count() + 1;
        return String.format("CLM-%s-%05d", datePart, count);
    }
}
