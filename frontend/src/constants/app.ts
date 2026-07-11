import type { ClaimStatus, ClaimType, Role } from '@/types';

export const CLAIM_TYPES: ClaimType[] = ['HEALTH', 'AUTO', 'HOME', 'LIFE', 'TRAVEL', 'PROPERTY', 'LIABILITY'];
export const ROLES: Role[] = ['CUSTOMER', 'CLAIM_AGENT', 'INVESTIGATOR', 'SUPERVISOR', 'ADMIN'];
export const STATUS_LABELS: Record<ClaimStatus, string> = { DRAFT: 'Draft', SUBMITTED: 'Submitted', UNDER_REVIEW: 'Under review', INVESTIGATION_REQUIRED: 'Investigation required', UNDER_INVESTIGATION: 'Under investigation', INVESTIGATION_COMPLETED: 'Investigation complete', APPROVED: 'Approved', REJECTED: 'Rejected', CLOSED: 'Closed' };
export const STATUS_STYLES: Record<ClaimStatus, string> = { DRAFT: 'bg-slate-100 text-slate-700', SUBMITTED: 'bg-blue-50 text-blue-700', UNDER_REVIEW: 'bg-amber-50 text-amber-800', INVESTIGATION_REQUIRED: 'bg-orange-50 text-orange-700', UNDER_INVESTIGATION: 'bg-orange-50 text-orange-700', INVESTIGATION_COMPLETED: 'bg-violet-50 text-violet-700', APPROVED: 'bg-emerald-50 text-emerald-700', REJECTED: 'bg-red-50 text-red-700', CLOSED: 'bg-slate-200 text-slate-700' };
