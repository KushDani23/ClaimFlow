package com.company.icps.claim.controller;

import com.company.icps.claim.dto.ClaimResponse;
import com.company.icps.claim.dto.ClaimSearchRequest;
import com.company.icps.claim.entity.ClaimStatus;
import com.company.icps.claim.entity.ClaimType;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/claims/search")
@RequiredArgsConstructor
@Tag(name = "Claim search", description = "Filter claims using role-scoped access")
@SecurityRequirement(name = "bearerAuth")
public class ClaimSearchController {
    private final ClaimService claimService;

    @Operation(summary = "Search claims", description = "Customers only see their claims; agents and investigators only see assigned claims.")
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'CLAIM_AGENT', 'INVESTIGATOR', 'SUPERVISOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<ClaimResponse>>> search(
            @RequestParam(required = false) ClaimStatus status,
            @RequestParam(required = false) ClaimType claimType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String policyNumber,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIRECTION) String sortDir,
            Authentication authentication) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        ClaimSearchRequest request = ClaimSearchRequest.builder()
                .status(status).claimType(claimType).fromDate(fromDate).toDate(toDate)
                .customerName(customerName).policyNumber(policyNumber).build();
        return ResponseEntity.ok(ApiResponse.success("Claims retrieved successfully",
                claimService.searchClaims(request, authentication.getName(), PageRequest.of(page, size, sort))));
    }
}
