package com.company.icps.document.service;

import com.company.icps.claim.entity.Claim;
import com.company.icps.claim.service.ClaimService;
import com.company.icps.common.constants.AppConstants;
import com.company.icps.common.exception.FileUploadException;
import com.company.icps.common.exception.ResourceNotFoundException;
import com.company.icps.document.dto.DocumentResponse;
import com.company.icps.document.entity.ClaimDocument;
import com.company.icps.document.repository.DocumentRepository;
import com.company.icps.user.entity.User;
import com.company.icps.user.repository.UserRepository;
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

    /**
     * Upload a document to a claim.
     */
    @Transactional
    public DocumentResponse uploadDocument(Long claimId, MultipartFile file, String email) {
        // Validate file
        validateFile(file);

        // Get claim and verify ownership
        Claim claim = claimService.getClaimEntity(claimId);
        validateClaimAccess(claim, email);

        // Get uploader
        User uploader = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // Store file on disk
        String storedFileName = fileStorageService.storeFile(file);
        String filePath = fileStorageService.getFilePath(storedFileName);

        // Save document metadata in DB
        ClaimDocument document = ClaimDocument.builder()
                .fileName(file.getOriginalFilename())
                .storedFileName(storedFileName)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .filePath(filePath)
                .claim(claim)
                .uploadedBy(uploader)
                .build();

        ClaimDocument savedDocument = documentRepository.save(document);
        return toResponse(savedDocument);
    }

    /**
     * Download a document by ID.
     */
    @Transactional(readOnly = true)
    public Resource downloadDocument(Long documentId, String email) {
        ClaimDocument document = getDocumentEntity(documentId);
        validateClaimAccess(document.getClaim(), email);
        return fileStorageService.loadFile(document.getStoredFileName());
    }

    /**
     * Get document metadata by ID (needed for Content-Type header on download).
     */
    @Transactional(readOnly = true)
    public ClaimDocument getDocumentEntity(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));
    }

    /**
     * List all documents for a claim.
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByClaimId(Long claimId, String email) {
        Claim claim = claimService.getClaimEntity(claimId);
        validateClaimAccess(claim, email);

        List<ClaimDocument> documents = documentRepository.findByClaimId(claimId);
        return documents.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Delete a document by ID.
     */
    @Transactional
    public void deleteDocument(Long documentId, String email) {
        ClaimDocument document = getDocumentEntity(documentId);
        validateClaimAccess(document.getClaim(), email);

        // Delete from disk
        fileStorageService.deleteFile(document.getStoredFileName());

        // Delete from DB
        documentRepository.delete(document);
    }

    // ---- Validation Helpers ----

    /**
     * Validate file type and size.
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileUploadException("File is empty");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !Arrays.asList(AppConstants.ALLOWED_FILE_TYPES).contains(contentType)) {
            throw new FileUploadException(
                    "File type not allowed. Accepted types: PDF, JPG, PNG"
            );
        }

        // Validate file extension
        String originalFileName = file.getOriginalFilename();
        if (originalFileName != null) {
            String extension = originalFileName.substring(originalFileName.lastIndexOf('.')).toLowerCase();
            if (!Arrays.asList(AppConstants.ALLOWED_FILE_EXTENSIONS).contains(extension)) {
                throw new FileUploadException(
                        "File extension not allowed. Accepted extensions: .pdf, .jpg, .jpeg, .png"
                );
            }
        }

        // Validate file size
        if (file.getSize() > AppConstants.MAX_FILE_SIZE) {
            throw new FileUploadException(
                    "File size exceeds maximum allowed size of 10MB"
            );
        }
    }

    /**
     * Verify the authenticated user has access to the claim.
     */
    private void validateClaimAccess(Claim claim, String email) {
        if (!claim.getCustomer().getEmail().equals(email)) {
            throw new AccessDeniedException("You do not have permission to access this claim's documents");
        }
    }

    /**
     * Map ClaimDocument entity to DocumentResponse DTO.
     */
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
