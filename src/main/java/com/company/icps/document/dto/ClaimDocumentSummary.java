package com.company.icps.document.dto;

import java.time.LocalDateTime;

/**
 * Lightweight projection of a {@code ClaimDocument} row that deliberately omits the
 * {@code file_data} (BYTEA) column.
 *
 * <p>Used by JPQL constructor expressions so that listing endpoints never load file bytes
 * into memory — only the document metadata needed for the UI list view is fetched.
 *
 * @param id             document primary key
 * @param fileName       original filename as supplied by the uploader
 * @param storedFileName UUID-based storage filename (used for legacy filesystem fallback)
 * @param fileType       MIME type (e.g. {@code application/pdf})
 * @param fileSize       file size in bytes
 * @param filePath       legacy filesystem path (empty string for new BYTEA-backed documents)
 * @param claimId        foreign key of the owning claim
 * @param claimNumber    human-readable claim reference number
 * @param uploadedByEmail email of the user who uploaded the document
 * @param createdAt      upload timestamp
 */
public record ClaimDocumentSummary(
        Long id,
        String fileName,
        String storedFileName,
        String fileType,
        Long fileSize,
        String filePath,
        Long claimId,
        String claimNumber,
        String uploadedByEmail,
        LocalDateTime createdAt
) {}
