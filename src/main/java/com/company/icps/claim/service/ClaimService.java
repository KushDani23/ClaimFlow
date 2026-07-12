package com.company.icps.claim.service;

import com.company.icps.claim.dto.ClaimResponse;
import com.company.icps.claim.dto.CreateClaimRequest;
import com.company.icps.claim.dto.UpdateClaimRequest;
import com.company.icps.claim.entity.Claim;
import com.company.icps.claim.entity.ClaimStatus;
import com.company.icps.claim.repository.ClaimRepository;
import com.company.icps.claim.repository.ClaimSpecifications;
import com.company.icps.claim.dto.ClaimSearchRequest;
import com.company.icps.audit.service.AuditService;
import com.company.icps.notification.service.NotificationService;
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
import java.util.Map;
import java.util.Set;

import com.company.icps.user.entity.Role;

import static com.company.icps.claim.entity.ClaimStatus.*;

@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    // Valid state transitions: from → set of allowed targets
    private static final Map<ClaimStatus, Set<ClaimStatus>> VALID_TRANSITIONS = Map.of(
            DRAFT, Set.of(SUBMITTED),
            SUBMITTED, Set.of(UNDER_REVIEW),
            UNDER_REVIEW, Set.of(APPROVED, REJECTED, INVESTIGATION_REQUIRED),
            INVESTIGATION_REQUIRED, Set.of(UNDER_INVESTIGATION),
            UNDER_INVESTIGATION, Set.of(INVESTIGATION_COMPLETED),
            INVESTIGATION_COMPLETED, Set.of(APPROVED, REJECTED),
            APPROVED, Set.of(CLOSED),
            REJECTED, Set.of(CLOSED)
    );

    // ---- Customer Operations ----

    @Transactional
    public ClaimResponse createClaim(CreateClaimRequest request, String email) {
        User customer = getUserByEmail(email);

        Claim claim = Claim.builder()
                .claimNumber(generateClaimNumber())
                .claimType(request.getClaimType())
                .status(DRAFT)
                .description(request.getDescription())
                .incidentDate(request.getIncidentDate())
                .claimAmount(request.getClaimAmount())
                .policyNumber(request.getPolicyNumber())
                .customer(customer)
                .build();

        Claim savedClaim = claimRepository.save(claim);
        auditService.log(customer, savedClaim, "CLAIM_CREATED", null, DRAFT, "Claim created as a draft");
        return toResponse(savedClaim);
    }

    @Transactional(readOnly = true)
    public ClaimResponse getClaimById(Long claimId, String email) {
        Claim claim = getClaimEntity(claimId);
        User requester = getUserByEmail(email);
        // Customers can only view their own claims; staff can view any
        if (requester.getRole() == Role.CUSTOMER) {
            validateOwnership(claim, email);
        }
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
        User customer = getUserByEmail(email);
        return transitionClaim(claim, SUBMITTED, "Customer submitted the claim", customer);
    }

    // ---- Workflow Operations (Agent / Investigator / Supervisor) ----

    @Transactional
    public ClaimResponse transitionClaim(Long claimId, ClaimStatus targetStatus, String notes, String email) {
        Claim claim = getClaimEntity(claimId);
        User actor = getUserByEmail(email);

        // Assign the actor as the agent if not already assigned
        if (claim.getAssignedAgent() == null) {
            claim.setAssignedAgent(actor);
        }

        // Store notes in the appropriate field based on target status
        if (notes != null && !notes.isBlank()) {
            if (targetStatus == INVESTIGATION_COMPLETED) {
                claim.setInvestigationNotes(notes);
            } else {
                claim.setAgentNotes(notes);
            }
        }

        return transitionClaim(claim, targetStatus, notes, actor);
    }

    @Transactional(readOnly = true)
    public Page<ClaimResponse> getClaimsByStatus(ClaimStatus status, Pageable pageable) {
        return claimRepository.findByStatus(status, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ClaimResponse> searchClaims(ClaimSearchRequest request, String email, Pageable pageable) {
        User user = getUserByEmail(email);
        var specification = ClaimSpecifications.matching(request);
        if (user.getRole() == Role.CUSTOMER) {
            specification = specification.and(ClaimSpecifications.ownedBy(user.getId()));
        } else if (user.getRole() == Role.CLAIM_AGENT || user.getRole() == Role.INVESTIGATOR) {
            specification = specification.and(ClaimSpecifications.assignedTo(user.getId()));
        }
        return claimRepository.findAll(specification, pageable).map(this::toResponse);
    }

    // ---- Helpers ----

    public Claim getClaimEntity(Long claimId) {
        return claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "id", claimId));
    }

    private ClaimResponse transitionClaim(Claim claim, ClaimStatus targetStatus, String notes, User actor) {
        ClaimStatus currentStatus = claim.getStatus();

        Set<ClaimStatus> allowed = VALID_TRANSITIONS.get(currentStatus);
        if (allowed == null || !allowed.contains(targetStatus)) {
            throw new InvalidStateTransitionException(currentStatus.name(), targetStatus.name());
        }

        claim.setStatus(targetStatus);
        Claim savedClaim = claimRepository.save(claim);
        auditService.log(actor, savedClaim, actionFor(targetStatus), currentStatus, targetStatus, notes);
        
        // Notify Customer
        String subject = "Claim Status Updated: " + savedClaim.getClaimNumber();
        String message = String.format("Your claim %s is now %s. %s", 
                savedClaim.getClaimNumber(), targetStatus.name(), notes != null ? notes : "");
        notificationService.notifyUser(savedClaim.getCustomer(), message, subject);
        
        // Notify Agent if assigned and actor is not the agent
        if (savedClaim.getAssignedAgent() != null && !savedClaim.getAssignedAgent().getId().equals(actor.getId())) {
            String agentMsg = String.format("Claim %s you are assigned to is now %s.", 
                    savedClaim.getClaimNumber(), targetStatus.name());
            notificationService.notifyUser(savedClaim.getAssignedAgent(), agentMsg, subject);
        }

        return toResponse(savedClaim);
    }

    private void validateOwnership(Claim claim, String email) {
        if (!claim.getCustomer().getEmail().equals(email)) {
            throw new AccessDeniedException("You do not have permission to access this claim");
        }
    }

    private void validateDraftStatus(Claim claim) {
        if (claim.getStatus() != DRAFT) {
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

    // ---- Mapping ----

    public ClaimResponse toResponse(Claim claim) {
        User customer = claim.getCustomer();
        User agent = claim.getAssignedAgent();
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
                .assignedAgent(agent != null ? agent.getEmail() : null)
                .agentNotes(claim.getAgentNotes())
                .investigationNotes(claim.getInvestigationNotes())
                .createdAt(claim.getCreatedAt())
                .updatedAt(claim.getUpdatedAt())
                .build();
    }

    private String actionFor(ClaimStatus targetStatus) {
        return switch (targetStatus) {
            case SUBMITTED -> "CLAIM_SUBMITTED";
            case APPROVED -> "CLAIM_APPROVED";
            case REJECTED -> "CLAIM_REJECTED";
            case UNDER_REVIEW -> "CLAIM_REVIEW_STARTED";
            case INVESTIGATION_REQUIRED -> "INVESTIGATION_REQUESTED";
            case UNDER_INVESTIGATION -> "INVESTIGATION_STARTED";
            case INVESTIGATION_COMPLETED -> "INVESTIGATION_COMPLETED";
            case CLOSED -> "CLAIM_CLOSED";
            default -> "CLAIM_STATUS_CHANGED";
        };
    }
}
