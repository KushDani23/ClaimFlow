import axios, { AxiosError } from 'axios';
import toast from 'react-hot-toast';
import type { ApiResponse } from '@/types';

const client = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api', timeout: 15000, headers: { 'Content-Type': 'application/json' } });
client.interceptors.request.use((config) => { const token = localStorage.getItem('icps_token'); if (token) config.headers.Authorization = `Bearer ${token}`; return config; });
client.interceptors.response.use((response) => response, (error: AxiosError<ApiResponse<unknown>>) => {
  const status = error.response?.status;
  if (status === 401) { localStorage.removeItem('icps_session'); localStorage.removeItem('icps_token'); if (location.pathname !== '/login') location.assign('/login'); }
  const message = error.response?.data?.message ?? (error.code === 'ECONNABORTED' ? 'The request timed out. Please try again.' : 'Unable to reach the server. Please try again.');
  if (status !== 401) toast.error(message);
  return Promise.reject(error);
});
export default client;
