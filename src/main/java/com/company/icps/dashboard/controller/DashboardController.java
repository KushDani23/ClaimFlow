package com.company.icps.dashboard.controller;

import com.company.icps.common.response.ApiResponse;
import com.company.icps.dashboard.dto.AgentDashboardResponse;
import com.company.icps.dashboard.dto.CustomerDashboardResponse;
import com.company.icps.dashboard.dto.SupervisorDashboardResponse;
import com.company.icps.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboards", description = "Role-specific claim reporting")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {
    private final DashboardService dashboardService;

    @Operation(summary = "Get customer dashboard")
    @GetMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<CustomerDashboardResponse>> customer(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Customer dashboard retrieved successfully",
                dashboardService.customerDashboard(authentication.getName())));
    }

    @Operation(summary = "Get agent dashboard")
    @GetMapping("/agent")
    @PreAuthorize("hasRole('CLAIM_AGENT')")
    public ResponseEntity<ApiResponse<AgentDashboardResponse>> agent(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Agent dashboard retrieved successfully",
                dashboardService.agentDashboard(authentication.getName())));
    }

    @Operation(summary = "Get supervisor dashboard")
    @GetMapping("/supervisor")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<ApiResponse<SupervisorDashboardResponse>> supervisor() {
        return ResponseEntity.ok(ApiResponse.success("Supervisor dashboard retrieved successfully",
                dashboardService.supervisorDashboard()));
    }
}
