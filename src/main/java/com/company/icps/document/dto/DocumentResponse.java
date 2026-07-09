package com.company.icps.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    private Long id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Long claimId;
    private String claimNumber;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
}
