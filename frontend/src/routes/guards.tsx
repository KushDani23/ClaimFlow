import { Navigate, Outlet, useLocation } from 'react-router-dom'; import { useAuth } from '@/contexts/AuthContext'; import type { Role } from '@/types';
export function ProtectedRoute() { const { isAuthenticated } = useAuth(); const location = useLocation(); return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace state={{ from: location }} />; }
export function RoleRoute({ roles }: { roles: Role[] }) { const { session } = useAuth(); return session && roles.includes(session.role) ? <Outlet /> : <Navigate to="/unauthorized" replace />; }
