package com.company.icps.claim.service;

import com.company.icps.claim.dto.ClaimResponse;
import com.company.icps.claim.dto.CreateClaimRequest;
import com.company.icps.claim.dto.UpdateClaimRequest;
import com.company.icps.claim.entity.Claim;
import com.company.icps.claim.entity.ClaimStatus;
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

    @Transactional
    public ClaimResponse createClaim(CreateClaimRequest request, String email) {
        User customer = getUserByEmail(email);

        Claim claim = Claim.builder()
                .claimNumber(generateClaimNumber())
                .claimType(request.getClaimType())
                .status(ClaimStatus.DRAFT)
                .description(request.getDescription())
                .incidentDate(request.getIncidentDate())
                .claimAmount(request.getClaimAmount())
                .policyNumber(request.getPolicyNumber())
                .customer(customer)
                .build();

        return toResponse(claimRepository.save(claim));
    }

    @Transactional(readOnly = true)
    public ClaimResponse getClaimById(Long claimId, String email) {
        Claim claim = getClaimEntity(claimId);
        validateOwnership(claim, email);
        return toResponse(claim);
    }

    @Transactional(readOnly = true)
    public Page<ClaimResponse> getMyClaims(String email, Pageable pageable) {
        User customer = getUserByEmail(email);
        return claimRepository.findByCustomerId(customer.getId(), pageable).map(this::toResponse);
    }

    @Transactional
    public ClaimResponse updateDraftClaim(Long claimId, UpdateClaimRequest request, String email) {
        Claim claim = getClaimEntity(claimId);
        validateOwnership(claim, email);
        validateDraftStatus(claim);

        if (request.getClaimType() != null) claim.setClaimType(request.getClaimType());
        if (request.getDescription() != null) claim.setDescription(request.getDescription());
        if (request.getIncidentDate() != null) claim.setIncidentDate(request.getIncidentDate());
        if (request.getClaimAmount() != null) claim.setClaimAmount(request.getClaimAmount());
        if (request.getPolicyNumber() != null) claim.setPolicyNumber(request.getPolicyNumber());

        return toResponse(claimRepository.save(claim));
    }

    @Transactional
    public void deleteDraftClaim(Long claimId, String email) {
        Claim claim = getClaimEntity(claimId);
        validateOwnership(claim, email);
        validateDraftStatus(claim);
        claimRepository.delete(claim);
    }

    @Transactional
    public ClaimResponse submitClaim(Long claimId, String email) {
        Claim claim = getClaimEntity(claimId);
        validateOwnership(claim, email);

        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new InvalidStateTransitionException(claim.getStatus().name(), ClaimStatus.SUBMITTED.name());
        }

        claim.setStatus(ClaimStatus.SUBMITTED);
        return toResponse(claimRepository.save(claim));
    }

    // ---- Helpers (also used by DocumentService) ----

    public Claim getClaimEntity(Long claimId) {
        return claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "id", claimId));
    }

    private void validateOwnership(Claim claim, String email) {
        if (!claim.getCustomer().getEmail().equals(email)) {
            throw new AccessDeniedException("You do not have permission to access this claim");
        }
    }

    private void validateDraftStatus(Claim claim) {
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new InvalidStateTransitionException(
                    "Only DRAFT claims can be modified. Current status: " + claim.getStatus().name());
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private String generateClaimNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = claimRepository.count() + 1;
        return String.format("CLM-%s-%05d", datePart, count);
    }

    // ---- Mapping (replaces separate ClaimMapper class) ----

    private ClaimResponse toResponse(Claim claim) {
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
