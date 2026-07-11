package com.company.icps.dashboard.service;

import com.company.icps.claim.dto.ClaimResponse;
import com.company.icps.claim.entity.ClaimStatus;
import com.company.icps.claim.repository.ClaimRepository;
import com.company.icps.claim.service.ClaimService;
import com.company.icps.common.exception.ResourceNotFoundException;
import com.company.icps.dashboard.dto.AgentDashboardResponse;
import com.company.icps.dashboard.dto.CustomerDashboardResponse;
import com.company.icps.dashboard.dto.SupervisorDashboardResponse;
import com.company.icps.user.entity.User;
import com.company.icps.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final ClaimService claimService;

    @Transactional(readOnly = true)
    public CustomerDashboardResponse customerDashboard(String email) {
        User user = user(email);
        long approved = claimRepository.countByCustomerIdAndStatus(user.getId(), ClaimStatus.APPROVED);
        long rejected = claimRepository.countByCustomerIdAndStatus(user.getId(), ClaimStatus.REJECTED);
        long closed = claimRepository.countByCustomerIdAndStatus(user.getId(), ClaimStatus.CLOSED);
        long total = claimRepository.countByCustomerId(user.getId());
        List<ClaimResponse> recent = claimRepository.findTop5ByCustomerIdOrderByCreatedAtDesc(user.getId())
                .stream().map(claimService::toResponse).toList();
        return CustomerDashboardResponse.builder()
                .totalClaims(total)
                .pendingClaims(total - approved - rejected - closed)
                .approvedClaims(approved)
                .rejectedClaims(rejected)
                .recentClaims(recent)
                .build();
    }

    @Transactional(readOnly = true)
    public AgentDashboardResponse agentDashboard(String email) {
        User user = user(email);
        return AgentDashboardResponse.builder()
                .assignedClaims(claimRepository.countByAssignedAgentId(user.getId()))
                .pendingReviews(claimRepository.countByAssignedAgentIdAndStatus(user.getId(), ClaimStatus.UNDER_REVIEW))
                .activeInvestigations(claimRepository.countByAssignedAgentIdAndStatus(user.getId(), ClaimStatus.INVESTIGATION_REQUIRED))
                .build();
    }

    @Transactional(readOnly = true)
    public SupervisorDashboardResponse supervisorDashboard() {
        return SupervisorDashboardResponse.builder()
                .pendingApprovals(claimRepository.countByStatus(ClaimStatus.UNDER_REVIEW)
                        + claimRepository.countByStatus(ClaimStatus.INVESTIGATION_COMPLETED))
                .approvedClaims(claimRepository.countByStatus(ClaimStatus.APPROVED))
                .rejectedClaims(claimRepository.countByStatus(ClaimStatus.REJECTED))
                .build();
    }

    private User user(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
