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
import java.util.List;

/**
 * One-time startup migration that backfills the {@code file_data} (BYTEA) column for
 * legacy {@code claim_documents} rows that were uploaded before the BYTEA storage change.
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li>A <em>narrow</em> native query fetches only {@code (id, stored_file_name, file_name)}
 *       for rows where {@code file_data IS NULL}.  File bytes are <strong>not</strong> loaded
 *       in this pass, so the startup check is cheap even when many large documents already
 *       live in the table.</li>
 *   <li>For each un-migrated row the full entity is loaded individually (via
 *       {@code findById}), the bytes are read from the filesystem through
 *       {@link FileStorageService}, and the entity is saved back with the BYTEA column
 *       populated.</li>
 * </ol>
 *
 * <p>This component is idempotent — once all rows have {@code file_data} populated the
 * initial query returns zero rows and the bean becomes a no-op on subsequent startups.
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
        // Cheap narrow query — does NOT load file_data bytes
        List<Object[]> missingRows = documentRepository.findRowsMissingFileData();

        if (missingRows.isEmpty()) {
            log.info("[DocumentDataMigration] All documents already have BYTEA data. Nothing to migrate.");
            return;
        }

        log.info("[DocumentDataMigration] Found {} legacy document(s) without BYTEA data. Migrating…",
                missingRows.size());

        int migrated = 0;
        int failed = 0;

        for (Object[] row : missingRows) {
            Long id = ((Number) row[0]).longValue();
            String storedFileName = (String) row[1];
            String fileName = (String) row[2];

            try {
                // Load the full entity only for rows that actually need migration
                ClaimDocument doc = documentRepository.findById(id).orElse(null);
                if (doc == null) {
                    log.warn("[DocumentDataMigration] Document id={} disappeared between check and load — skipping", id);
                    continue;
                }

                org.springframework.core.io.Resource resource =
                        fileStorageService.loadFile(storedFileName);
                byte[] bytes = resource.getContentAsByteArray();

                doc.setFileData(bytes);
                doc.setFilePath(""); // clear absolute filesystem path after migrating
                documentRepository.save(doc);
                migrated++;
                log.info("[DocumentDataMigration] Migrated document id={} name={} ({} bytes)",
                        id, fileName, bytes.length);

            } catch (IOException | com.company.icps.common.exception.FileUploadException e) {
                failed++;
                log.warn("[DocumentDataMigration] Could not migrate document id={} name='{}': {} — " +
                        "file may be missing from the filesystem. It will be served via filesystem " +
                        "fallback if the file still exists, or will return an error if deleted.",
                        id, fileName, e.getMessage());
            }
        }

        log.info("[DocumentDataMigration] Complete — migrated={} failed={}", migrated, failed);
    }
}
