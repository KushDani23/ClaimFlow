package com.company.icps.audit.controller;

import com.company.icps.audit.dto.AuditLogResponse;
import com.company.icps.audit.service.AuditService;
import com.company.icps.common.constants.AppConstants;
import com.company.icps.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/claims/{claimId}/timeline")
@RequiredArgsConstructor
@Tag(name = "Audit logs", description = "Immutable claim activity timeline")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {
    private final AuditService auditService;

    @Operation(summary = "Get a claim activity timeline")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getTimeline(
            @PathVariable Long claimId,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Claim timeline retrieved successfully",
                auditService.getClaimTimeline(claimId, authentication.getName(),
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }
}
