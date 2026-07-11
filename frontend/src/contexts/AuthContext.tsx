import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { authService } from '@/services/authService';
import type { UserSession } from '@/types';

interface AuthContextValue { session: UserSession | null; isAuthenticated: boolean; login: (email: string, password: string) => Promise<UserSession>; register: (payload: { firstName: string; lastName: string; email: string; password: string }) => Promise<UserSession>; logout: () => void; }
const AuthContext = createContext<AuthContextValue | null>(null);
const SESSION_KEY = 'icps_session';
const initialSession = (): UserSession | null => { try { const saved = localStorage.getItem(SESSION_KEY); return saved ? JSON.parse(saved) as UserSession : null; } catch { return null; } };
export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<UserSession | null>(initialSession);
  const save = useCallback((data: UserSession) => { localStorage.setItem(SESSION_KEY, JSON.stringify(data)); localStorage.setItem('icps_token', data.token); setSession(data); return data; }, []);
  const login = useCallback(async (email: string, password: string) => save(await authService.login(email, password)), [save]);
  const register = useCallback(async (payload: { firstName: string; lastName: string; email: string; password: string }) => save(await authService.register(payload)), [save]);
  const logout = useCallback(() => { localStorage.removeItem(SESSION_KEY); localStorage.removeItem('icps_token'); setSession(null); }, []);
  const value = useMemo(() => ({ session, isAuthenticated: Boolean(session), login, register, logout }), [session, login, register, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
export const useAuth = () => { const context = useContext(AuthContext); if (!context) throw new Error('useAuth must be used inside AuthProvider'); return context; };
