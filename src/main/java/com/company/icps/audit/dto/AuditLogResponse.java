package com.company.icps.audit.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class AuditLogResponse {
    Long id;
    Long claimId;
    String claimNumber;
    String actorEmail;
    String action;
    String oldStatus;
    String newStatus;
    String details;
    LocalDateTime createdAt;
}
