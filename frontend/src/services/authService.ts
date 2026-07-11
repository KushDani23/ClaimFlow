import client from '@/api/axios'; import { API } from '@/constants/api'; import type { ApiResponse, AuthResponse, UserProfile } from '@/types';
export const authService = {
  login: async (email: string, password: string) => (await client.post<ApiResponse<AuthResponse>>(API.auth.login, { email, password })).data.data,
  register: async (payload: { firstName: string; lastName: string; email: string; password: string }) => (await client.post<ApiResponse<AuthResponse>>(API.auth.register, payload)).data.data,
  profile: async () => (await client.get<ApiResponse<UserProfile>>(API.users.profile)).data.data
};
