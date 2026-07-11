import client from '@/api/axios'; import { API } from '@/constants/api'; import type { ApiResponse, DocumentItem } from '@/types';
export const documentService = {
  list: async (claimId: number) => (await client.get<ApiResponse<DocumentItem[]>>(API.claims.documents(claimId))).data.data,
  upload: async (claimId: number, file: File, onProgress?: (percent: number) => void) => { const form = new FormData(); form.append('file', file); return (await client.post<ApiResponse<DocumentItem>>(API.claims.documents(claimId), form, { headers: { 'Content-Type': 'multipart/form-data' }, onUploadProgress: (event) => { if (event.total) onProgress?.(Math.round((event.loaded / event.total) * 100)); } })).data.data; },
  remove: async (id: number) => { await client.delete(API.documents.byId(id)); },
  download: async (id: number, fileName: string) => { const response = await client.get(API.documents.byId(id), { responseType: 'blob' }); const url = URL.createObjectURL(response.data as Blob); const link = document.createElement('a'); link.href = url; link.download = fileName; link.click(); URL.revokeObjectURL(url); }
};
