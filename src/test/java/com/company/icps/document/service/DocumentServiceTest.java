package com.company.icps.document.service;

import com.company.icps.audit.service.AuditService;
import com.company.icps.claim.entity.Claim;
import com.company.icps.claim.entity.ClaimStatus;
import com.company.icps.claim.entity.ClaimType;
import com.company.icps.claim.service.ClaimService;
import com.company.icps.document.entity.ClaimDocument;
import com.company.icps.document.repository.DocumentRepository;
import com.company.icps.user.entity.Role;
import com.company.icps.user.entity.User;
import com.company.icps.user.repository.UserRepository;
import com.company.icps.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {
    @Mock private DocumentRepository documentRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private ClaimService claimService;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;
    @InjectMocks private DocumentService documentService;

    @Test
    void uploadsPdfAndRecordsAuditEntry() {
        User customer = User.builder().id(1L).email("customer@icps.test").firstName("Test").lastName("User")
                .password("encoded").role(Role.CUSTOMER).build();
        Claim claim = Claim.builder().id(5L).claimNumber("CLM-5").claimType(ClaimType.HEALTH)
                .status(ClaimStatus.DRAFT).description("Valid description here")
                .incidentDate(LocalDate.now().minusDays(1)).claimAmount(BigDecimal.TEN).policyNumber("POL-5")
                .customer(customer).build();
        MockMultipartFile file = new MockMultipartFile("file", "evidence.pdf", "application/pdf", "test".getBytes());
        when(claimService.getClaimEntity(5L)).thenReturn(claim);
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(fileStorageService.storeFile(file)).thenReturn("stored-evidence.pdf");
        when(fileStorageService.getFilePath("stored-evidence.pdf")).thenReturn("uploads/stored-evidence.pdf");
        when(documentRepository.save(any(ClaimDocument.class))).thenAnswer(invocation -> {
            ClaimDocument document = invocation.getArgument(0);
            document.setId(7L);
            return document;
        });

        var response = documentService.uploadDocument(5L, file, customer.getEmail());

        assertEquals("evidence.pdf", response.getFileName());
        verify(auditService).log(customer, claim, "DOCUMENT_UPLOADED", ClaimStatus.DRAFT, ClaimStatus.DRAFT,
                "Uploaded document: evidence.pdf");
    }
}
