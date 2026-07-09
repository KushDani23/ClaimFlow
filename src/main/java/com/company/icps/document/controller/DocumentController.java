package com.company.icps.document.controller;

import com.company.icps.common.response.ApiResponse;
import com.company.icps.document.dto.DocumentResponse;
import com.company.icps.document.entity.ClaimDocument;
import com.company.icps.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Document upload and management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "Upload a document", description = "Upload a supporting document (PDF, JPG, PNG, max 10MB) to a claim")
    @PostMapping("/api/claims/{claimId}/documents")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @PathVariable Long claimId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        DocumentResponse document = documentService.uploadDocument(claimId, file, authentication.getName());
        return new ResponseEntity<>(
                ApiResponse.success("Document uploaded successfully", document, HttpStatus.CREATED.value()),
                HttpStatus.CREATED
        );
    }

    @Operation(summary = "List documents for a claim", description = "Returns all documents attached to a specific claim")
    @GetMapping("/api/claims/{claimId}/documents")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocumentsByClaimId(
            @PathVariable Long claimId,
            Authentication authentication
    ) {
        List<DocumentResponse> documents = documentService.getDocumentsByClaimId(claimId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved successfully", documents));
    }

    @Operation(summary = "Download a document", description = "Download a document file by its ID")
    @GetMapping("/api/documents/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id,
            Authentication authentication
    ) {
        // Get document metadata for headers
        ClaimDocument document = documentService.getDocumentEntity(id);
        // Download the file (validates access internally)
        Resource resource = documentService.downloadDocument(id, authentication.getName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + document.getFileName() + "\"")
                .body(resource);
    }

    @Operation(summary = "Delete a document", description = "Delete a document from the claim and file system")
    @DeleteMapping("/api/documents/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable Long id,
            Authentication authentication
    ) {
        documentService.deleteDocument(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Document deleted successfully", null));
    }
}
