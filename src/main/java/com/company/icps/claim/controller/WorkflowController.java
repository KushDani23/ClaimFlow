package com.company.icps.claim.controller;

import com.company.icps.claim.dto.ClaimResponse;
import com.company.icps.claim.dto.WorkflowActionRequest;
import com.company.icps.claim.entity.ClaimStatus;
import com.company.icps.claim.service.ClaimService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workflow/claims")
@RequiredArgsConstructor
@Tag(name = "Workflow", description = "Claim workflow endpoints for agents, investigators, and supervisors")
@SecurityRequirement(name = "bearerAuth")
public class WorkflowController {

    private final ClaimService claimService;

    @Operation(summary = "List claims by status", description = "Returns claims filtered by status (for agents/supervisors)")
    @GetMapping
    @PreAuthorize("hasAnyRole('CLAIM_AGENT', 'INVESTIGATOR', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<Page<ClaimResponse>>> getClaimsByStatus(
            @RequestParam ClaimStatus status,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIRECTION) String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();
        Page<ClaimResponse> claims = claimService.getClaimsByStatus(status, PageRequest.of(page, size, sort));
        return ResponseEntity.ok(ApiResponse.success("Claims retrieved successfully", claims));
    }

    @Operation(summary = "Pick up a claim for review", description = "Agent moves a SUBMITTED claim to UNDER_REVIEW")
    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('CLAIM_AGENT')")
    public ResponseEntity<ApiResponse<ClaimResponse>> reviewClaim(
            @PathVariable Long id,
            @RequestBody(required = false) WorkflowActionRequest request,
            Authentication auth
    ) {
        return ok("Claim is now under review",
                claimService.transitionClaim(id, ClaimStatus.UNDER_REVIEW, notesFrom(request), auth.getName()));
    }

    @Operation(summary = "Approve a claim", description = "Agent or Supervisor approves a claim")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('CLAIM_AGENT', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<ClaimResponse>> approveClaim(
            @PathVariable Long id,
            @RequestBody(required = false) WorkflowActionRequest request,
            Authentication auth
    ) {
        return ok("Claim approved",
                claimService.transitionClaim(id, ClaimStatus.APPROVED, notesFrom(request), auth.getName()));
    }

    @Operation(summary = "Reject a claim", description = "Agent or Supervisor rejects a claim")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('CLAIM_AGENT', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<ClaimResponse>> rejectClaim(
            @PathVariable Long id,
            @RequestBody(required = false) WorkflowActionRequest request,
            Authentication auth
    ) {
        return ok("Claim rejected",
                claimService.transitionClaim(id, ClaimStatus.REJECTED, notesFrom(request), auth.getName()));
    }

    @Operation(summary = "Flag for investigation", description = "Agent flags a claim for investigation")
    @PostMapping("/{id}/investigate")
    @PreAuthorize("hasRole('CLAIM_AGENT')")
    public ResponseEntity<ApiResponse<ClaimResponse>> flagForInvestigation(
            @PathVariable Long id,
            @RequestBody(required = false) WorkflowActionRequest request,
            Authentication auth
    ) {
        return ok("Claim flagged for investigation",
                claimService.transitionClaim(id, ClaimStatus.INVESTIGATION_REQUIRED, notesFrom(request), auth.getName()));
    }

    @Operation(summary = "Start investigation", description = "Investigator begins investigating a claim")
    @PostMapping("/{id}/start-investigation")
    @PreAuthorize("hasRole('INVESTIGATOR')")
    public ResponseEntity<ApiResponse<ClaimResponse>> startInvestigation(
            @PathVariable Long id,
            Authentication auth
    ) {
        return ok("Investigation started",
                claimService.transitionClaim(id, ClaimStatus.UNDER_INVESTIGATION, null, auth.getName()));
    }

    @Operation(summary = "Complete investigation", description = "Investigator completes investigation with findings")
    @PostMapping("/{id}/complete-investigation")
    @PreAuthorize("hasRole('INVESTIGATOR')")
    public ResponseEntity<ApiResponse<ClaimResponse>> completeInvestigation(
            @PathVariable Long id,
            @RequestBody(required = false) WorkflowActionRequest request,
            Authentication auth
    ) {
        return ok("Investigation completed",
                claimService.transitionClaim(id, ClaimStatus.INVESTIGATION_COMPLETED, notesFrom(request), auth.getName()));
    }

    @Operation(summary = "Close a claim", description = "Close an approved or rejected claim")
    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('CLAIM_AGENT', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<ClaimResponse>> closeClaim(
            @PathVariable Long id,
            @RequestBody(required = false) WorkflowActionRequest request,
            Authentication auth
    ) {
        return ok("Claim closed",
                claimService.transitionClaim(id, ClaimStatus.CLOSED, notesFrom(request), auth.getName()));
    }

    // ---- Helpers to avoid repetition ----

    private ResponseEntity<ApiResponse<ClaimResponse>> ok(String message, ClaimResponse claim) {
        return ResponseEntity.ok(ApiResponse.success(message, claim));
    }

    private String notesFrom(WorkflowActionRequest request) {
        return request != null ? request.getNotes() : null;
    }
}
