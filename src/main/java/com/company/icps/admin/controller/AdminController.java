package com.company.icps.admin.controller;

import com.company.icps.admin.dto.DashboardStatsResponse;
import com.company.icps.admin.dto.UpdateRoleRequest;
import com.company.icps.admin.service.AdminService;
import com.company.icps.claim.dto.ClaimResponse;
import com.company.icps.common.constants.AppConstants;
import com.company.icps.common.response.ApiResponse;
import com.company.icps.user.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin-only endpoints for user and claim management")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    // ---- User Management ----

    @Operation(summary = "List all users")
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserProfileResponse>>> getAllUsers(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIRECTION) String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully",
                adminService.getAllUsers(PageRequest.of(page, size, sort))));
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully",
                adminService.getUserById(id)));
    }

    @Operation(summary = "Update user role")
    @PutMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("User role updated successfully",
                adminService.updateUserRole(id, request.getRole())));
    }

    @Operation(summary = "Enable or disable a user account")
    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<ApiResponse<UserProfileResponse>> toggleUserStatus(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully",
                adminService.toggleUserStatus(id)));
    }

    // ---- Claim Management ----

    @Operation(summary = "List all claims")
    @GetMapping("/claims")
    public ResponseEntity<ApiResponse<Page<ClaimResponse>>> getAllClaims(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIRECTION) String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();
        return ResponseEntity.ok(ApiResponse.success("Claims retrieved successfully",
                adminService.getAllClaims(PageRequest.of(page, size, sort))));
    }

    @Operation(summary = "Get any claim by ID")
    @GetMapping("/claims/{id}")
    public ResponseEntity<ApiResponse<ClaimResponse>> getClaimById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Claim retrieved successfully",
                adminService.getClaimById(id)));
    }

    // ---- Dashboard ----

    @Operation(summary = "Get system dashboard statistics")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats retrieved successfully",
                adminService.getDashboardStats()));
    }
}
