package com.company.icps.document.service;

import com.company.icps.document.entity.ClaimDocument;
import com.company.icps.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * One-time startup migration that backfills the {@code file_data} (BYTEA) column for
 * legacy {@code claim_documents} rows that were uploaded before the BYTEA storage change.
 *
 * <p>For each document where {@code file_data IS NULL} and a {@code file_path} is recorded,
 * this bean attempts to read the file from the filesystem and store the bytes in the DB.
 * This is idempotent: documents that already have {@code file_data} populated are skipped.
 *
 * <p>This component is safe to keep in production — after all legacy rows are migrated it
 * becomes a no-op (the query returns zero rows).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentDataMigration {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateFilesystemDocumentsToDatabase() {
        List<ClaimDocument> legacyDocs = documentRepository.findAll().stream()
                .filter(d -> d.getFileData() == null || d.getFileData().length == 0)
                .filter(d -> d.getStoredFileName() != null && !d.getStoredFileName().isBlank())
                .toList();

        if (legacyDocs.isEmpty()) {
            log.info("[DocumentDataMigration] No legacy filesystem documents to migrate.");
            return;
        }

        log.info("[DocumentDataMigration] Found {} legacy document(s) without BYTEA data. Migrating…",
                legacyDocs.size());

        int migrated = 0;
        int failed = 0;

        for (ClaimDocument doc : legacyDocs) {
            try {
                org.springframework.core.io.Resource resource =
                        fileStorageService.loadFile(doc.getStoredFileName());
                byte[] bytes = resource.getContentAsByteArray();
                doc.setFileData(bytes);
                doc.setFilePath(""); // clear the absolute path after migrating
                documentRepository.save(doc);
                migrated++;
                log.info("[DocumentDataMigration] Migrated document id={} name={} ({} bytes)",
                        doc.getId(), doc.getFileName(), bytes.length);
            } catch (IOException | com.company.icps.common.exception.FileUploadException e) {
                failed++;
                log.warn("[DocumentDataMigration] Could not migrate document id={} name={}: {} — " +
                        "the file may be missing. It will still be served via filesystem fallback " +
                        "until the file is re-uploaded.",
                        doc.getId(), doc.getFileName(), e.getMessage());
            }
        }

        log.info("[DocumentDataMigration] Complete — migrated={} failed={}", migrated, failed);
    }
}
