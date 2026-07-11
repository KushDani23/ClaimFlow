export type Role = 'ADMIN' | 'CUSTOMER' | 'CLAIM_AGENT' | 'INVESTIGATOR' | 'SUPERVISOR';
export type ClaimStatus = 'DRAFT' | 'SUBMITTED' | 'UNDER_REVIEW' | 'INVESTIGATION_REQUIRED' | 'UNDER_INVESTIGATION' | 'INVESTIGATION_COMPLETED' | 'APPROVED' | 'REJECTED' | 'CLOSED';
export type ClaimType = 'HEALTH' | 'AUTO' | 'HOME' | 'LIFE' | 'TRAVEL' | 'PROPERTY' | 'LIABILITY';

export interface ApiResponse<T> { success: boolean; message: string; data: T; status: number; timestamp: string; }
export interface Page<T> { content: T[]; totalElements: number; totalPages: number; size: number; number: number; first: boolean; last: boolean; empty: boolean; }
export interface AuthResponse { token: string; email: string; role: Role; firstName: string; lastName: string; }
export interface UserProfile { id: number; firstName: string; lastName: string; email: string; role: Role; enabled: boolean; createdAt: string; }
export interface Claim { id: number; claimNumber: string; claimType: ClaimType; status: ClaimStatus; description: string; incidentDate: string; claimAmount: number; policyNumber: string; customerName: string; customerEmail: string; assignedAgent: string | null; agentNotes: string | null; investigationNotes: string | null; createdAt: string; updatedAt: string; }
export interface ClaimPayload { claimType: ClaimType; description: string; incidentDate: string; claimAmount: number; policyNumber: string; }
export interface DocumentItem { id: number; fileName: string; fileType: string; fileSize: number; claimId: number; claimNumber: string; uploadedBy: string; uploadedAt: string; }
export interface DashboardStats { totalUsers: number; totalClaims: number; claimsByStatus: Partial<Record<ClaimStatus, number>>; }
export interface UserSession { token: string; email: string; role: Role; firstName: string; lastName: string; }
