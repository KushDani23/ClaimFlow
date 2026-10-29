package com.company.icps.admin.service;

import com.company.icps.admin.dto.ClaimTypeStat;
import com.company.icps.admin.dto.DashboardStatsResponse;
import com.company.icps.admin.dto.UserClaimStat;
import com.company.icps.audit.entity.AuditLog;
import com.company.icps.audit.repository.AuditLogRepository;
import com.company.icps.claim.dto.ClaimResponse;
import com.company.icps.claim.entity.Claim;
import com.company.icps.claim.entity.ClaimStatus;
import com.company.icps.claim.entity.ClaimType;
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

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ClaimRepository claimRepository;
    private final AuditLogRepository auditLogRepository;

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
        List<Claim> allClaims = claimRepository.findAll();
        List<User> allUsers = userRepository.findAll();
        List<AuditLog> allAuditLogs = auditLogRepository.findAll();

        long totalClaims = allClaims.size();
        long approvedClaims = allClaims.stream().filter(c -> c.getStatus() == ClaimStatus.APPROVED).count();
        long rejectedClaims = allClaims.stream().filter(c -> c.getStatus() == ClaimStatus.REJECTED).count();
        long settledClaims = approvedClaims + rejectedClaims;
        long pendingClaims = allClaims.stream()
                .filter(c -> c.getStatus() != ClaimStatus.APPROVED && c.getStatus() != ClaimStatus.REJECTED && c.getStatus() != ClaimStatus.CLOSED)
                .count();

        BigDecimal totalClaimAmount = allClaims.stream()
                .map(Claim::getClaimAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal approvedAmount = allClaims.stream()
                .filter(c -> c.getStatus() == ClaimStatus.APPROVED)
                .map(Claim::getClaimAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal rejectedAmount = allClaims.stream()
                .filter(c -> c.getStatus() == ClaimStatus.REJECTED)
                .map(Claim::getClaimAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal settledAmount = approvedAmount.add(rejectedAmount);

        BigDecimal pendingAmount = allClaims.stream()
                .filter(c -> c.getStatus() != ClaimStatus.APPROVED && c.getStatus() != ClaimStatus.REJECTED && c.getStatus() != ClaimStatus.CLOSED)
                .map(Claim::getClaimAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double approvalRate = settledClaims > 0 ? ((double) approvedClaims / settledClaims) * 100.0 : 0.0;
        double rejectionRate = settledClaims > 0 ? ((double) rejectedClaims / settledClaims) * 100.0 : 0.0;

        Map<String, Long> claimsByStatus = Arrays.stream(ClaimStatus.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        status -> allClaims.stream().filter(c -> c.getStatus() == status).count()
                ));

        List<ClaimTypeStat> claimsByType = Arrays.stream(ClaimType.values())
                .map(type -> {
                    List<Claim> typeClaims = allClaims.stream().filter(c -> c.getClaimType() == type).toList();
                    long total = typeClaims.size();
                    long approved = typeClaims.stream().filter(c -> c.getStatus() == ClaimStatus.APPROVED).count();
                    long rejected = typeClaims.stream().filter(c -> c.getStatus() == ClaimStatus.REJECTED).count();
                    BigDecimal totalAmt = typeClaims.stream()
                            .map(Claim::getClaimAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal appAmt = typeClaims.stream()
                            .filter(c -> c.getStatus() == ClaimStatus.APPROVED)
                            .map(Claim::getClaimAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return ClaimTypeStat.builder()
                            .type(type.name())
                            .totalClaims(total)
                            .approvedClaims(approved)
                            .rejectedClaims(rejected)
                            .totalAmount(totalAmt)
                            .approvedAmount(appAmt)
                            .build();
                })
                .filter(s -> s.getTotalClaims() > 0)
                .toList();

        Map<Long, List<AuditLog>> logsByActor = allAuditLogs.stream()
                .filter(l -> l.getActor() != null)
                .collect(Collectors.groupingBy(l -> l.getActor().getId()));

        List<UserClaimStat> userStats = allUsers.stream()
                .map(user -> buildUserClaimStat(user, allClaims, logsByActor.getOrDefault(user.getId(), Collections.emptyList())))
                .sorted(Comparator.comparing(UserClaimStat::getTotalClaims).reversed()
                        .thenComparing(UserClaimStat::getName))
                .toList();

        return DashboardStatsResponse.builder()
                .totalUsers(allUsers.size())
                .totalClaims(totalClaims)
                .approvedClaims(approvedClaims)
                .rejectedClaims(rejectedClaims)
                .settledClaims(settledClaims)
                .pendingClaims(pendingClaims)
                .totalClaimAmount(totalClaimAmount)
                .approvedAmount(approvedAmount)
                .rejectedAmount(rejectedAmount)
                .settledAmount(settledAmount)
                .pendingAmount(pendingAmount)
                .approvalRate(Math.round(approvalRate * 10.0) / 10.0)
                .rejectionRate(Math.round(rejectionRate * 10.0) / 10.0)
                .claimsByStatus(claimsByStatus)
                .claimsByType(claimsByType)
                .userStats(userStats)
                .build();
    }

    private UserClaimStat buildUserClaimStat(User user, List<Claim> allClaims, List<AuditLog> userLogs) {
        String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + " " +
                (user.getLastName() != null ? user.getLastName() : "")).trim();
        if (fullName.isEmpty()) {
            fullName = user.getEmail();
        }

        Role role = user.getRole();
        List<Claim> relevantClaims;

        if (role == Role.CUSTOMER) {
            relevantClaims = allClaims.stream()
                    .filter(c -> c.getCustomer() != null && c.getCustomer().getId().equals(user.getId()))
                    .toList();
        } else if (role == Role.CLAIM_AGENT) {
            relevantClaims = allClaims.stream()
                    .filter(c -> c.getAssignedAgent() != null && c.getAssignedAgent().getId().equals(user.getId()))
                    .toList();
        } else if (role == Role.INVESTIGATOR || role == Role.SUPERVISOR) {
            Set<Long> claimIds = userLogs.stream()
                    .map(l -> l.getClaim() != null ? l.getClaim().getId() : null)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            relevantClaims = allClaims.stream()
                    .filter(c -> claimIds.contains(c.getId()))
                    .toList();
        } else {
            // ADMIN
            relevantClaims = Collections.emptyList();
        }

        long total = relevantClaims.size();
        long approved = relevantClaims.stream().filter(c -> c.getStatus() == ClaimStatus.APPROVED).count();
        long rejected = relevantClaims.stream().filter(c -> c.getStatus() == ClaimStatus.REJECTED).count();
        long pending = relevantClaims.stream()
                .filter(c -> c.getStatus() != ClaimStatus.APPROVED && c.getStatus() != ClaimStatus.REJECTED && c.getStatus() != ClaimStatus.CLOSED)
                .count();

        BigDecimal totalAmt = relevantClaims.stream()
                .map(Claim::getClaimAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal appAmt = relevantClaims.stream()
                .filter(c -> c.getStatus() == ClaimStatus.APPROVED)
                .map(Claim::getClaimAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal rejAmt = relevantClaims.stream()
                .filter(c -> c.getStatus() == ClaimStatus.REJECTED)
                .map(Claim::getClaimAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return UserClaimStat.builder()
                .userId(user.getId())
                .name(fullName)
                .email(user.getEmail())
                .role(role != null ? role.name() : "CUSTOMER")
                .totalClaims(total)
                .approvedClaims(approved)
                .rejectedClaims(rejected)
                .pendingClaims(pending)
                .totalAmount(totalAmt)
                .approvedAmount(appAmt)
                .rejectedAmount(rejAmt)
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
