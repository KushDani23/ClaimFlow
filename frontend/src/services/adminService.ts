import client from '@/api/axios'; import { API } from '@/constants/api'; import type { ApiResponse, Claim, DashboardStats, Page, Role, UserProfile } from '@/types';
export const adminService = {
  dashboard: async () => (await client.get<ApiResponse<DashboardStats>>(API.admin.dashboard)).data.data,
  users: async (params: { page?: number; size?: number; sortDir?: string }) => (await client.get<ApiResponse<Page<UserProfile>>>(API.admin.users, { params })).data.data,
  claims: async (params: { page?: number; size?: number; sortDir?: string }) => (await client.get<ApiResponse<Page<Claim>>>(API.admin.claims, { params })).data.data,
  updateRole: async (id: number, role: Role) => (await client.put<ApiResponse<UserProfile>>(API.admin.updateRole(id), { role })).data.data,
  toggleUser: async (id: number) => (await client.put<ApiResponse<UserProfile>>(API.admin.toggleUser(id))).data.data
};
