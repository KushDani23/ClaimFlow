package com.company.icps.audit.service;

import com.company.icps.audit.dto.AuditLogResponse;
import com.company.icps.audit.entity.AuditLog;
import com.company.icps.audit.repository.AuditLogRepository;
import com.company.icps.claim.entity.Claim;
import com.company.icps.claim.entity.ClaimStatus;
import com.company.icps.claim.repository.ClaimRepository;
import com.company.icps.common.exception.ResourceNotFoundException;
import com.company.icps.user.entity.Role;
import com.company.icps.user.entity.User;
import com.company.icps.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ClaimRepository claimRepository;

    @Transactional
    public void log(User actor, Claim claim, String action, ClaimStatus oldStatus,
                    ClaimStatus newStatus, String details) {
        auditLogRepository.save(AuditLog.builder()
                .actor(actor)
                .claim(claim)
                .action(action)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .details(details)
                .build());
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getClaimTimeline(Long claimId, String email, Pageable pageable) {
        User requester = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "id", claimId));
        boolean ownsClaim = claim.getCustomer().getId().equals(requester.getId());
        boolean handlesClaim = claim.getAssignedAgent() != null
                && claim.getAssignedAgent().getId().equals(requester.getId());
        boolean privileged = requester.getRole() == Role.ADMIN
                || requester.getRole() == Role.SUPERVISOR
                || requester.getRole() == Role.INVESTIGATOR
                || requester.getRole() == Role.CLAIM_AGENT;
        if (!ownsClaim && !handlesClaim && !privileged) {
            throw new AccessDeniedException("You do not have permission to view this claim timeline");
        }
        return auditLogRepository.findByClaimIdOrderByCreatedAtDesc(claimId, pageable).map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .claimId(log.getClaim() != null ? log.getClaim().getId() : null)
                .claimNumber(log.getClaim() != null ? log.getClaim().getClaimNumber() : null)
                .actorEmail(log.getActor().getEmail())
                .action(log.getAction())
                .oldStatus(log.getOldStatus() != null ? log.getOldStatus().name() : null)
                .newStatus(log.getNewStatus() != null ? log.getNewStatus().name() : null)
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
