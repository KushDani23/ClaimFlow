package com.company.icps.document.repository;

import com.company.icps.document.dto.ClaimDocumentSummary;
import com.company.icps.document.entity.ClaimDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<ClaimDocument, Long> {

    /**
     * Returns lightweight metadata for all documents attached to a claim.
     *
     * <p>The JPQL constructor expression selects every column <em>except</em>
     * {@code file_data}, so file bytes are never loaded into the JVM heap during
     * listing operations.
     */
    @Query("""
            SELECT new com.company.icps.document.dto.ClaimDocumentSummary(
                d.id,
                d.fileName,
                d.storedFileName,
                d.fileType,
                d.fileSize,
                d.filePath,
                d.claim.id,
                d.claim.claimNumber,
                d.uploadedBy.email,
                d.createdAt
            )
            FROM ClaimDocument d
            WHERE d.claim.id = :claimId
            """)
    List<ClaimDocumentSummary> findSummaryByClaimId(@Param("claimId") Long claimId);

    /**
     * Returns the IDs and stored filenames of documents that still have no BYTEA data.
     *
     * <p>Used exclusively by {@code DocumentDataMigration} at startup.  The native query
     * selects only three narrow columns so the migration check is cheap even when
     * many large documents already exist in the table.
     */
    @Query(value = """
            SELECT id, stored_file_name, file_name
            FROM claim_documents
            WHERE file_data IS NULL
            """, nativeQuery = true)
    List<Object[]> findRowsMissingFileData();

    long countByClaimId(Long claimId);
}
