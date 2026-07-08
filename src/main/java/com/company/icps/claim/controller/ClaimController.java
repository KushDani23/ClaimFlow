package com.company.icps.claim.controller;

import com.company.icps.claim.dto.ClaimResponse;
import com.company.icps.claim.dto.CreateClaimRequest;
import com.company.icps.claim.dto.UpdateClaimRequest;
import com.company.icps.claim.service.ClaimService;
import com.company.icps.common.constants.AppConstants;
import com.company.icps.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
@Tag(name = "Claims", description = "Claim management endpoints for customers")
@SecurityRequirement(name = "bearerAuth")
public class ClaimController {

    private final ClaimService claimService;

    @Operation(summary = "Create a new claim", description = "Creates a new claim in DRAFT status for the authenticated customer")
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ClaimResponse>> createClaim(
            @Valid @RequestBody CreateClaimRequest request,
            Authentication authentication
    ) {
        ClaimResponse claim = claimService.createClaim(request, authentication.getName());
        return new ResponseEntity<>(
                ApiResponse.success("Claim created successfully", claim, HttpStatus.CREATED.value()),
                HttpStatus.CREATED
        );
    }

    @Operation(summary = "Get my claims", description = "Returns paginated list of claims for the authenticated customer")
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Page<ClaimResponse>>> getMyClaims(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIRECTION) String sortDir,
            Authentication authentication
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ClaimResponse> claims = claimService.getMyClaims(authentication.getName(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Claims retrieved successfully", claims));
    }

    @Operation(summary = "Get claim by ID", description = "Returns a specific claim owned by the authenticated customer")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ClaimResponse>> getClaimById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        ClaimResponse claim = claimService.getClaimById(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Claim retrieved successfully", claim));
    }

    @Operation(summary = "Update a draft claim", description = "Updates a claim that is still in DRAFT status")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ClaimResponse>> updateClaim(
            @PathVariable Long id,
            @Valid @RequestBody UpdateClaimRequest request,
            Authentication authentication
    ) {
        ClaimResponse claim = claimService.updateDraftClaim(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Claim updated successfully", claim));
    }

    @Operation(summary = "Delete a draft claim", description = "Deletes a claim that is still in DRAFT status")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> deleteClaim(
            @PathVariable Long id,
            Authentication authentication
    ) {
        claimService.deleteDraftClaim(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Claim deleted successfully", null));
    }

    @Operation(summary = "Submit a claim", description = "Transitions a DRAFT claim to SUBMITTED status")
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ClaimResponse>> submitClaim(
            @PathVariable Long id,
            Authentication authentication
    ) {
        ClaimResponse claim = claimService.submitClaim(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Claim submitted successfully", claim));
    }
}
