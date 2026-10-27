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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Handles document upload, retrieval, listing, and deletion.
 *
 * <p><b>Storage strategy</b>: File bytes are stored in the {@code file_data} (BYTEA) column of
 * the {@code claim_documents} table.  Existing legacy rows (where {@code file_data} is null)
 * are served via the {@link FileStorageService} filesystem fallback so no historical data
 * is lost.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;   // kept for legacy-row fallback
    private final ClaimService claimService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    // ── Upload ────────────────────────────────────────────────────────────────

    @Transactional
    public DocumentResponse uploadDocument(Long claimId, MultipartFile file, String email) {
        validateFile(file);

        Claim claim = claimService.getClaimEntity(claimId);
        User uploader = getUser(email);
        validateClaimAccess(claim, uploader);

        // Read file bytes inside the transaction so a failure throws before any DB write
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new FileUploadException("Failed to read uploaded file: " + file.getOriginalFilename(), e);
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new FileUploadException("File name is invalid");
        }

        String extension = originalFileName.contains(".")
                ? originalFileName.substring(originalFileName.lastIndexOf('.'))
                : "";
        String storedFileName = UUID.randomUUID() + extension;

        ClaimDocument document = ClaimDocument.builder()
                .fileName(originalFileName)
                .storedFileName(storedFileName)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .filePath("")          // empty — bytes live in file_data column
                .fileData(fileBytes)   // ← stored in PostgreSQL BYTEA
                .claim(claim)
                .uploadedBy(uploader)
                .build();

        ClaimDocument savedDocument = documentRepository.save(document);
        log.info("Document uploaded: id={} name={} size={} claimId={}",
                savedDocument.getId(), savedDocument.getFileName(),
                savedDocument.getFileSize(), claimId);

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

    // ── Download / Retrieve ───────────────────────────────────────────────────

    /**
     * Returns a {@link DocumentDownloadResult} containing the raw file bytes and the
     * document metadata needed to build the HTTP response.
     *
     * <p>For new uploads the bytes come from the PostgreSQL {@code file_data} BYTEA column.
     * For legacy rows (where {@code file_data} is null) the bytes are read from the
     * filesystem via {@link FileStorageService}.
     */
    @Transactional(readOnly = true)
    public DocumentDownloadResult downloadDocument(Long documentId, String email) {
        ClaimDocument document = getDocumentEntity(documentId);
        User requester = getUser(email);
        validateClaimAccess(document.getClaim(), requester);

        byte[] bytes;

        if (document.getFileData() != null && document.getFileData().length > 0) {
            // ── New path: bytes stored in PostgreSQL ──────────────────────────
            bytes = document.getFileData();
        } else {
            // ── Legacy path: bytes on the filesystem ──────────────────────────
            log.info("Document id={} has no BYTEA data; falling back to filesystem", documentId);
            try {
                org.springframework.core.io.Resource resource =
                        fileStorageService.loadFile(document.getStoredFileName());
                bytes = resource.getContentAsByteArray();
            } catch (IOException e) {
                throw new FileUploadException(
                        "Legacy file is missing from the filesystem and has no database backup. " +
                        "Document id=" + documentId + " cannot be retrieved.", e);
            } catch (FileUploadException e) {
                throw new FileUploadException(
                        "Legacy file not found on filesystem for document id=" + documentId +
                        ". The file may have been deleted or the server moved.", e);
            }
        }

        return new DocumentDownloadResult(bytes, document);
    }

    // ── Metadata-only accessors ────────────────────────────────────────────────

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

    // ── Delete ────────────────────────────────────────────────────────────────

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

        // For legacy filesystem rows: attempt to clean up the file, but don't fail if missing
        if (document.getFileData() == null || document.getFileData().length == 0) {
            try {
                fileStorageService.deleteFile(document.getStoredFileName());
            } catch (Exception e) {
                log.warn("Could not delete legacy filesystem file for document id={}: {}",
                        documentId, e.getMessage());
            }
        }

        documentRepository.delete(document);
        log.info("Document deleted: id={} name={}", documentId, document.getFileName());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
     * Grants access if the requester is:
     * <ul>
     *   <li>the claim's customer</li>
     *   <li>the assigned agent on the claim</li>
     *   <li>ADMIN, SUPERVISOR, INVESTIGATOR, or CLAIM_AGENT</li>
     * </ul>
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

    // ── Inner result type ─────────────────────────────────────────────────────

    /**
     * Carries both the raw file bytes and the document metadata so the controller
     * can build the HTTP response in one step without a second round-trip to the DB.
     */
    public record DocumentDownloadResult(byte[] bytes, ClaimDocument document) {}
}
