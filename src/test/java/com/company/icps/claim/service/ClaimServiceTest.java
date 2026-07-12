package com.company.icps.claim.service;

import com.company.icps.audit.service.AuditService;
import com.company.icps.claim.dto.CreateClaimRequest;
import com.company.icps.claim.entity.Claim;
import com.company.icps.claim.entity.ClaimStatus;
import com.company.icps.claim.entity.ClaimType;
import com.company.icps.claim.repository.ClaimRepository;
import com.company.icps.user.entity.Role;
import com.company.icps.user.entity.User;
import com.company.icps.notification.service.NotificationService;
import com.company.icps.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {
    @Mock private ClaimRepository claimRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;
    @InjectMocks private ClaimService claimService;

    @Test
    void createsDraftAndRecordsAuditEntry() {
        User customer = user(1L, "customer@icps.test", Role.CUSTOMER);
        CreateClaimRequest request = new CreateClaimRequest();
        request.setClaimType(ClaimType.HEALTH);
        request.setDescription("Hospital treatment following a covered incident.");
        request.setIncidentDate(LocalDate.now().minusDays(1));
        request.setClaimAmount(BigDecimal.valueOf(2500));
        request.setPolicyNumber("POL-1001");
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(claimRepository.count()).thenReturn(0L);
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> {
            Claim claim = invocation.getArgument(0);
            claim.setId(10L);
            return claim;
        });

        var result = claimService.createClaim(request, customer.getEmail());

        assertEquals("DRAFT", result.getStatus());
        ArgumentCaptor<Claim> captor = ArgumentCaptor.forClass(Claim.class);
        verify(auditService).log(eq(customer), captor.capture(), eq("CLAIM_CREATED"), isNull(),
                eq(ClaimStatus.DRAFT), anyString());
        assertEquals("CLM-" + LocalDate.now().toString().replace("-", "") + "-00001", captor.getValue().getClaimNumber());
    }

    @Test
    void approvesAnInvestigationCompletedClaimAndAuditsTransition() {
        User supervisor = user(2L, "supervisor@icps.test", Role.SUPERVISOR);
        Claim claim = Claim.builder().id(20L).claimNumber("CLM-TEST")
                .claimType(ClaimType.HEALTH).status(ClaimStatus.INVESTIGATION_COMPLETED)
                .description("Covered incident description").incidentDate(LocalDate.now().minusDays(2))
                .claimAmount(BigDecimal.TEN).policyNumber("POL-2").customer(user(1L, "customer@icps.test", Role.CUSTOMER)).build();
        when(claimRepository.findById(20L)).thenReturn(Optional.of(claim));
        when(userRepository.findByEmail(supervisor.getEmail())).thenReturn(Optional.of(supervisor));
        when(claimRepository.save(claim)).thenReturn(claim);

        var result = claimService.transitionClaim(20L, ClaimStatus.APPROVED, "Verified findings", supervisor.getEmail());

        assertEquals("APPROVED", result.getStatus());
        verify(auditService).log(supervisor, claim, "CLAIM_APPROVED", ClaimStatus.INVESTIGATION_COMPLETED,
                ClaimStatus.APPROVED, "Verified findings");
    }

    private User user(Long id, String email, Role role) {
        return User.builder().id(id).email(email).firstName("Test").lastName("User")
                .role(role).password("encoded").enabled(true).build();
    }
}
