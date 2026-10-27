package com.company.icps.document.controller;

import com.company.icps.common.response.ApiResponse;
import com.company.icps.document.dto.DocumentResponse;
import com.company.icps.document.service.DocumentService;
import com.company.icps.document.service.DocumentService.DocumentDownloadResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Document upload and management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "Upload a document",
            description = "Upload a supporting document (PDF, JPG, PNG, max 10MB) to a claim")
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

    @Operation(summary = "List documents for a claim",
            description = "Returns all documents attached to a specific claim. Staff roles can view documents to assess claims.")
    @GetMapping("/api/claims/{claimId}/documents")
    @PreAuthorize("hasAnyRole('CUSTOMER','CLAIM_AGENT','INVESTIGATOR','SUPERVISOR','ADMIN')")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocumentsByClaimId(
            @PathVariable Long claimId,
            Authentication authentication
    ) {
        List<DocumentResponse> documents = documentService.getDocumentsByClaimId(claimId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved successfully", documents));
    }

    /**
     * Download or preview a document.
     *
     * <p>Fetches the actual file bytes from PostgreSQL (or the legacy filesystem for rows
     * uploaded before the BYTEA migration) and streams them to the client.
     *
     * <ul>
     *   <li>{@code ?inline=true} — sets {@code Content-Disposition: inline} so browsers open
     *       PDF files in their built-in viewer.</li>
     *   <li>Omit the parameter (or {@code ?inline=false}) — forces a download dialog.</li>
     * </ul>
     */
    @Operation(summary = "Download or view a document",
            description = "Fetch a document file by ID. Pass ?inline=true to view in browser (PDF preview), or omit for a forced download.")
    @GetMapping("/api/documents/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','CLAIM_AGENT','INVESTIGATOR','SUPERVISOR','ADMIN')")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable Long id,
            @RequestParam(value = "inline", defaultValue = "false") boolean inline,
            Authentication authentication
    ) {
        DocumentDownloadResult result = documentService.downloadDocument(id, authentication.getName());
        byte[] bytes = result.bytes();
        String originalFileName = result.document().getFileName();
        String contentType = result.document().getFileType();

        // Safely encode the filename for the Content-Disposition header
        String encodedName = URLEncoder.encode(originalFileName, StandardCharsets.UTF_8)
                .replace("+", "%20");

        String disposition = (inline ? "inline" : "attachment")
                + "; filename=\"" + originalFileName + "\""
                + "; filename*=UTF-8''" + encodedName;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length))
                .body(bytes);
    }

    @Operation(summary = "Delete a document", description = "Delete a document from the claim and storage")
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
