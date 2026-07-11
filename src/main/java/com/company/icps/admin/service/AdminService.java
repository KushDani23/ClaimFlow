package com.company.icps.admin.service;

import com.company.icps.admin.dto.DashboardStatsResponse;
import com.company.icps.claim.dto.ClaimResponse;
import com.company.icps.claim.entity.Claim;
import com.company.icps.claim.entity.ClaimStatus;
import com.company.icps.claim.repository.ClaimRepository;
import com.company.icps.common.exception.ResourceNotFoundException;
import com.company.icps.user.dto.UserProfileResponse;
import com.company.icps.user.entity.Role;
import com.company.icps.user.entity.User;
import com.company.icps.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ClaimRepository claimRepository;

    // ---- User Management ----

    @Transactional(readOnly = true)
    public Page<UserProfileResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toUserResponse);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserById(Long id) {
        return toUserResponse(findUser(id));
    }

    @Transactional
    public UserProfileResponse updateUserRole(Long id, Role role) {
        User user = findUser(id);
        user.setRole(role);
        return toUserResponse(userRepository.save(user));
    }

    @Transactional
    public UserProfileResponse toggleUserStatus(Long id) {
        User user = findUser(id);
        user.setEnabled(!user.isEnabled());
        return toUserResponse(userRepository.save(user));
    }

    // ---- Claim Management ----

    @Transactional(readOnly = true)
    public Page<ClaimResponse> getAllClaims(Pageable pageable) {
        return claimRepository.findAll(pageable).map(this::toClaimResponse);
    }

    @Transactional(readOnly = true)
    public ClaimResponse getClaimById(Long id) {
        return toClaimResponse(claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "id", id)));
    }

    // ---- Dashboard ----

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        Map<String, Long> claimsByStatus = Arrays.stream(ClaimStatus.values())
                .collect(Collectors.toMap(Enum::name, claimRepository::countByStatus));

        return DashboardStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalClaims(claimRepository.count())
                .claimsByStatus(claimsByStatus)
                .build();
    }

    // ---- Mapping ----

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    private UserProfileResponse toUserResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private ClaimResponse toClaimResponse(Claim claim) {
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
}
