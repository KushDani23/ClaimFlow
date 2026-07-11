import client from '@/api/axios'; import { API } from '@/constants/api'; import type { ApiResponse, Claim, ClaimPayload, Page } from '@/types';
export interface ListParams { page?: number; size?: number; sortDir?: 'asc' | 'desc'; sortBy?: string; status?: string; }
export const claimService = {
  list: async (params: ListParams) => (await client.get<ApiResponse<Page<Claim>>>(API.claims.root, { params })).data.data,
  get: async (id: number) => (await client.get<ApiResponse<Claim>>(API.claims.byId(id))).data.data,
  create: async (payload: ClaimPayload) => (await client.post<ApiResponse<Claim>>(API.claims.root, payload)).data.data,
  update: async (id: number, payload: Partial<ClaimPayload>) => (await client.put<ApiResponse<Claim>>(API.claims.byId(id), payload)).data.data,
  remove: async (id: number) => { await client.delete<ApiResponse<void>>(API.claims.byId(id)); },
  submit: async (id: number) => (await client.post<ApiResponse<Claim>>(API.claims.submit(id))).data.data,
  workflowList: async (status: string, params: ListParams) => (await client.get<ApiResponse<Page<Claim>>>(API.workflow.claims, { params: { ...params, status } })).data.data,
  workflow: async (id: number, action: string, notes?: string) => (await client.post<ApiResponse<Claim>>(API.workflow.action(id, action), notes ? { notes } : undefined)).data.data
};
