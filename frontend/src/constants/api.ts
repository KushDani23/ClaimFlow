export const API = {
  auth: { login: '/auth/login', register: '/auth/register' },
  users: { profile: '/users/profile' },
  claims: { root: '/claims', byId: (id: number) => `/claims/${id}`, submit: (id: number) => `/claims/${id}/submit`, documents: (id: number) => `/claims/${id}/documents` },
  documents: { byId: (id: number) => `/documents/${id}` },
  workflow: { claims: '/workflow/claims', action: (id: number, action: string) => `/workflow/claims/${id}/${action}` },
  admin: { dashboard: '/admin/dashboard', users: '/admin/users', claims: '/admin/claims', updateRole: (id: number) => `/admin/users/${id}/role`, toggleUser: (id: number) => `/admin/users/${id}/toggle-status` }
} as const;
