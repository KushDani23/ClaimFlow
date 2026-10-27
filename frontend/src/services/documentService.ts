import client from '@/api/axios';
import { API } from '@/constants/api';
import type { ApiResponse, DocumentItem } from '@/types';

export const documentService = {
  list: async (claimId: number) =>
    (await client.get<ApiResponse<DocumentItem[]>>(API.claims.documents(claimId))).data.data,

  upload: async (claimId: number, file: File, onProgress?: (percent: number) => void) => {
    const form = new FormData();
    form.append('file', file);
    return (
      await client.post<ApiResponse<DocumentItem>>(API.claims.documents(claimId), form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (event) => {
          if (event.total) onProgress?.(Math.round((event.loaded / event.total) * 100));
        },
      })
    ).data.data;
  },

  remove: async (id: number) => {
    await client.delete(API.documents.byId(id));
  },

  /** Triggers a file-save download dialog in the browser */
  download: async (id: number, fileName: string) => {
    const response = await client.get(API.documents.byId(id), { responseType: 'blob' });
    const url = URL.createObjectURL(response.data as Blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    // Revoke after a short delay to allow the download to start
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  },

  /**
   * Opens the document inline in a new browser tab (PDF preview, image viewer, etc.)
   * The blob URL is revoked after the tab has had time to load the content.
   */
  view: async (id: number) => {
    // ?inline=true tells the backend to send Content-Disposition: inline
    const response = await client.get(`${API.documents.byId(id)}?inline=true`, {
      responseType: 'blob',
    });
    const blob = response.data as Blob;
    const url = URL.createObjectURL(blob);
    const tab = window.open(url, '_blank', 'noopener,noreferrer');
    // Revoke the object URL after enough time for the new tab to load the content.
    // If the tab is blocked by a pop-up blocker, inform the user.
    if (!tab) {
      URL.revokeObjectURL(url);
      throw new Error(
        'Pop-up blocked. Please allow pop-ups for this site and try again.',
      );
    }
    setTimeout(() => URL.revokeObjectURL(url), 30000);
  },
};
