package com.company.icps.document.service;

import com.company.icps.claim.entity.Claim;
import com.company.icps.claim.service.ClaimService;
import com.company.icps.common.constants.AppConstants;
import com.company.icps.common.exception.FileUploadException;
import com.company.icps.common.exception.ResourceNotFoundException;
import com.company.icps.document.dto.DocumentResponse;
import com.company.icps.document.entity.ClaimDocument;
import com.company.icps.document.repository.DocumentRepository;
import com.company.icps.user.entity.Role;
import com.company.icps.user.entity.User;
import com.company.icps.user.repository.UserRepository;
import com.company.icps.audit.service.AuditService;
import com.company.icps.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final ClaimService claimService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Transactional
    public DocumentResponse uploadDocument(Long claimId, MultipartFile file, String email) {
        validateFile(file);

        Claim claim = claimService.getClaimEntity(claimId);
        User uploader = getUser(email);
        validateClaimAccess(claim, uploader);

        String storedFileName = fileStorageService.storeFile(file);

        ClaimDocument document = ClaimDocument.builder()
                .fileName(file.getOriginalFilename())
                .storedFileName(storedFileName)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .filePath(fileStorageService.getFilePath(storedFileName))
                .claim(claim)
                .uploadedBy(uploader)
                .build();

        ClaimDocument savedDocument = documentRepository.save(document);
        auditService.log(uploader, claim, "DOCUMENT_UPLOADED", claim.getStatus(), claim.getStatus(),
                "Uploaded document: " + savedDocument.getFileName());

        // Notify assigned agent if the document was uploaded by the customer
        if (claim.getAssignedAgent() != null && claim.getCustomer().getId().equals(uploader.getId())) {
            String subject = "New Document Uploaded: " + claim.getClaimNumber();
            String message = String.format("Customer uploaded a new document (%s) for claim %s.", 
                    savedDocument.getFileName(), claim.getClaimNumber());
            notificationService.notifyUser(claim.getAssignedAgent(), message, subject);
        }

        return toResponse(savedDocument);
    }

    @Transactional(readOnly = true)
    public Resource downloadDocument(Long documentId, String email) {
        ClaimDocument document = getDocumentEntity(documentId);
        User requester = getUser(email);
        validateClaimAccess(document.getClaim(), requester);
        return fileStorageService.loadFile(document.getStoredFileName());
    }

    @Transactional(readOnly = true)
    public ClaimDocument getDocumentEntity(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByClaimId(Long claimId, String email) {
        Claim claim = claimService.getClaimEntity(claimId);
        User requester = getUser(email);
        validateClaimAccess(claim, requester);
        return documentRepository.findByClaimId(claimId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void deleteDocument(Long documentId, String email) {
        ClaimDocument document = getDocumentEntity(documentId);
        User requester = getUser(email);
        // Only the customer who uploaded or admin can delete
        boolean isOwner = document.getClaim().getCustomer().getEmail().equals(email);
        boolean isAdmin = requester.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to delete this document");
        }
        fileStorageService.deleteFile(document.getStoredFileName());
        documentRepository.delete(document);
    }

    // ---- Helpers ----

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileUploadException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !Arrays.asList(AppConstants.ALLOWED_FILE_TYPES).contains(contentType)) {
            throw new FileUploadException("File type not allowed. Accepted types: PDF, JPG, PNG");
        }

        if (file.getSize() > AppConstants.MAX_FILE_SIZE) {
            throw new FileUploadException("File size exceeds maximum allowed size of 10MB");
        }
    }

    /**
     * Allows access if:
     * - The user is the claim's customer
     * - The user is the assigned agent on the claim
     * - The user is an ADMIN or SUPERVISOR
     * - The user is an INVESTIGATOR
     */
    private void validateClaimAccess(Claim claim, User requester) {
        boolean isCustomer = claim.getCustomer().getId().equals(requester.getId());
        boolean isAgent = claim.getAssignedAgent() != null
                && claim.getAssignedAgent().getId().equals(requester.getId());
        boolean isPrivileged = requester.getRole() == Role.ADMIN
                || requester.getRole() == Role.SUPERVISOR
                || requester.getRole() == Role.INVESTIGATOR
                || requester.getRole() == Role.CLAIM_AGENT;

        if (!isCustomer && !isAgent && !isPrivileged) {
            throw new AccessDeniedException("You do not have permission to access this claim's documents");
        }
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private DocumentResponse toResponse(ClaimDocument document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .claimId(document.getClaim().getId())
                .claimNumber(document.getClaim().getClaimNumber())
                .uploadedBy(document.getUploadedBy().getEmail())
                .uploadedAt(document.getCreatedAt())
                .build();
    }
}
