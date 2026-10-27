package com.company.icps.document.entity;

import com.company.icps.claim.entity.Claim;
import com.company.icps.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "claim_documents")
public class ClaimDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String storedFileName;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private Long fileSize;

    /**
     * Legacy column: stores the filesystem path used before BYTEA storage was introduced.
     * Kept nullable so that pre-existing rows (which have a path but no BYTEA data) are
     * not broken by the schema update.  New uploads set this to an empty string.
     */
    @Column(nullable = true)
    private String filePath;

    /**
     * Actual file bytes stored in PostgreSQL as BYTEA.
     * New uploads always populate this column.
     * Legacy rows uploaded before this change will have this field null;
     * the retrieval path falls back to the filesystem for those rows.
     */
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "file_data", columnDefinition = "bytea")
    private byte[] fileData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
