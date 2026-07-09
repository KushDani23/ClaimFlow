package com.company.icps.document.repository;

import com.company.icps.document.entity.ClaimDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<ClaimDocument, Long> {

    List<ClaimDocument> findByClaimId(Long claimId);

    long countByClaimId(Long claimId);
}
