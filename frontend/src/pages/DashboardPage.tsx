import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Activity,
  ArrowUpRight,
  BarChart3,
  CheckCircle2,
  CircleX,
  ClipboardList,
  Clock3,
  Download,
  Eye,
  FileText,
  Filter,
  IndianRupee,
  Layers,
  PieChart as PieChartIcon,
  Scale,
  Search,
  ShieldCheck,
  TrendingUp,
  UserCheck,
  Users,
  X,
} from 'lucide-react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { Link } from 'react-router-dom';
import { Button, Card, EmptyState, PageLoader } from '@/components/ui';
import { StatusChip } from '@/components/StatusChip';
import { useAuth } from '@/contexts/AuthContext';
import { adminService } from '@/services/adminService';
import { claimService } from '@/services/claimService';
import { formatCurrency, formatDate } from '@/utils/format';
import type { Claim, ClaimStatus, DashboardStats, Role, UserClaimStat } from '@/types';

// ── Customer Stats Calculation Helper ─────────────────────────────────────────
const customerStats = (claims: Claim[]) => ({
  total: claims.length,
  pending: claims.filter((item) => !['DRAFT', 'APPROVED', 'REJECTED', 'CLOSED'].includes(item.status)).length,
  approved: claims.filter((item) => item.status === 'APPROVED').length,
  rejected: claims.filter((item) => item.status === 'REJECTED').length,
});

// ── Basic Stat Card ──────────────────────────────────────────────────────────
function StatCard({
  label,
  value,
  subtext,
  icon: Icon,
  tone = 'text-brand',
  bgTone = 'bg-blue-50',
}: {
  label: string;
  value: string | number;
  subtext?: string;
  icon: typeof ClipboardList;
  tone?: string;
  bgTone?: string;
}) {
  return (
    <Card className="p-5 shadow-sm border border-slate-200/80 transition-shadow hover:shadow-md">
      <div className="flex items-start justify-between">
        <div className="min-w-0 flex-1">
          <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">{label}</p>
          <p className="mt-2 text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight truncate">
            {value}
          </p>
          {subtext && <p className="mt-1.5 text-xs font-medium text-slate-500 truncate">{subtext}</p>}
        </div>
        <span className={`rounded-xl p-3 shrink-0 ml-3 ${bgTone} ${tone}`}>
          <Icon className="h-6 w-6" />
        </span>
      </div>
    </Card>
  );
}

// ── Status Color Palette & Clean Labels ──────────────────────────────────────
const STATUS_CONFIG: Record<string, { label: string; color: string }> = {
  SUBMITTED: { label: 'Submitted', color: '#3b82f6' },
  UNDER_REVIEW: { label: 'In Review', color: '#0284c7' },
  INVESTIGATION_REQUIRED: { label: 'Needs Investigation', color: '#f59e0b' },
  UNDER_INVESTIGATION: { label: 'Investigating', color: '#d97706' },
  INVESTIGATION_COMPLETED: { label: 'Inv. Complete', color: '#8b5cf6' },
  APPROVED: { label: 'Approved', color: '#10b981' },
  REJECTED: { label: 'Rejected', color: '#ef4444' },
  CLOSED: { label: 'Closed', color: '#64748b' },
  DRAFT: { label: 'Draft', color: '#94a3b8' },
};

// ── Main Page Router ──────────────────────────────────────────────────────────
export function DashboardPage() {
  const { session } = useAuth();
  const isAdmin = session?.role === 'ADMIN';

  const claimsQuery = useQuery({
    queryKey: ['dashboard-claims'],
    queryFn: () => claimService.list({ page: 0, size: 8 }),
    enabled: session?.role === 'CUSTOMER',
  });

  const adminQuery = useQuery({
    queryKey: ['dashboard-admin'],
    queryFn: adminService.dashboard,
    enabled: isAdmin,
  });

  if ((session?.role === 'CUSTOMER' && claimsQuery.isLoading) || (isAdmin && adminQuery.isLoading)) {
    return <PageLoader />;
  }

  if (isAdmin && adminQuery.data) {
    return <AdminDashboard stats={adminQuery.data} />;
  }

  const claims = claimsQuery.data?.content ?? [];
  const stats = customerStats(claims);
  const roleTitle = session?.role?.replace('_', ' ').toLowerCase().replace(/\b\w/g, (x) => x.toUpperCase());

  return (
    <>
      <div className="mb-7 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-ink">Good day, {session?.firstName}</h1>
          <p className="mt-1 text-sm text-slate-500">
            {session?.role === 'CUSTOMER'
              ? 'Here is an overview of your claims.'
              : `Your ${roleTitle} workspace is ready.`}
          </p>
        </div>
        {session?.role === 'CUSTOMER' && (
          <Link
            className="rounded-lg bg-brand px-4 py-2.5 text-sm font-semibold text-white hover:bg-sky-700 shadow-sm"
            to="/claims/new"
          >
            Start a claim
          </Link>
        )}
      </div>

      {session?.role === 'CUSTOMER' ? (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard label="Total claims" value={stats.total} icon={ClipboardList} />
            <StatCard
              label="In progress"
              value={stats.pending}
              icon={Clock3}
              tone="text-amber-600"
              bgTone="bg-amber-50"
            />
            <StatCard
              label="Approved"
              value={stats.approved}
              icon={CheckCircle2}
              tone="text-emerald-600"
              bgTone="bg-emerald-50"
            />
            <StatCard
              label="Rejected"
              value={stats.rejected}
              icon={Activity}
              tone="text-red-600"
              bgTone="bg-red-50"
            />
          </div>
          <RecentClaims claims={claims} />
        </>
      ) : (
        <RoleOverview role={session?.role ?? 'CLAIM_AGENT'} />
      )}
    </>
  );
}

// ── Non-Admin Staff Overview ──────────────────────────────────────────────────
function RoleOverview({ role }: { role: string }) {
  const heading =
    role === 'INVESTIGATOR'
      ? 'Investigation cases'
      : role === 'SUPERVISOR'
      ? 'Pending approvals'
      : 'Review queue';

  return (
    <Card className="p-8">
      <ClipboardList className="mb-4 h-8 w-8 text-brand" />
      <h2 className="text-xl font-bold text-ink">{heading}</h2>
      <p className="mt-2 max-w-xl text-sm leading-6 text-slate-500">
        Your active work is organized by status in the work queue. Open it to review and progress claims that
        are ready for your role.
      </p>
      <Link to="/work-queue" className="mt-5 inline-flex rounded-lg bg-brand px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-sky-700">
        Open work queue
      </Link>
    </Card>
  );
}

// ── Customer Recent Claims Table ──────────────────────────────────────────────
function RecentClaims({ claims }: { claims: Claim[] }) {
  return (
    <Card className="mt-6 overflow-hidden">
      <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
        <h2 className="font-bold text-ink">Recent claims</h2>
        <Link className="text-sm font-semibold text-brand hover:underline" to="/claims">
          View all
        </Link>
      </div>
      {claims.length === 0 ? (
        <EmptyState title="No claims yet" description="Create your first claim to begin tracking it here." />
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
              <tr>
                <th className="px-5 py-3">Claim</th>
                <th className="px-5 py-3">Type</th>
                <th className="px-5 py-3">Created</th>
                <th className="px-5 py-3">Status</th>
              </tr>
            </thead>
            <tbody>
              {claims.map((claim) => (
                <tr key={claim.id} className="border-t border-slate-100 hover:bg-slate-50/50">
                  <td className="px-5 py-4 font-semibold text-ink">
                    <Link className="hover:text-brand" to={`/claims/${claim.id}`}>
                      {claim.claimNumber}
                    </Link>
                  </td>
                  <td className="px-5 py-4 text-slate-600">{claim.claimType}</td>
                  <td className="px-5 py-4 text-slate-600">{formatDate(claim.createdAt)}</td>
                  <td className="px-5 py-4">
                    <StatusChip status={claim.status} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  );
}

// ── Admin User Claims Drilldown Modal ─────────────────────────────────────────
function UserClaimsDrilldownModal({
  user,
  onClose,
}: {
  user: UserClaimStat;
  onClose: () => void;
}) {
  const claimsQuery = useQuery({
    queryKey: ['admin-claims-all'],
    queryFn: () => adminService.claims({ page: 0, size: 100 }),
  });

  const matchingClaims = useMemo(() => {
    if (!claimsQuery.data?.content) return [];
    const all = claimsQuery.data.content;
    if (user.role === 'CUSTOMER') {
      return all.filter((c) => c.customerEmail === user.email);
    }
    if (user.role === 'CLAIM_AGENT') {
      return all.filter((c) => c.assignedAgent === user.email || c.assignedAgent === user.name);
    }
    return all;
  }, [claimsQuery.data, user]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 p-4 backdrop-blur-sm animate-in fade-in duration-150">
      <div className="w-full max-w-4xl max-h-[88vh] flex flex-col rounded-2xl bg-white shadow-2xl border border-slate-200 overflow-hidden">
        {/* Modal Header */}
        <div className="flex items-center justify-between border-b border-slate-200 bg-slate-50 px-6 py-4">
          <div className="flex items-center gap-3">
            <div className="grid h-10 w-10 place-items-center rounded-xl bg-blue-100 text-blue-700 font-bold text-sm">
              {user.name.slice(0, 2).toUpperCase()}
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-bold text-slate-900">{user.name}</h3>
                <span className="rounded-full bg-slate-200/80 px-2 py-0.5 text-xs font-semibold text-slate-700">
                  {user.role}
                </span>
              </div>
              <p className="text-xs text-slate-500">{user.email}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-200/60 hover:text-slate-700 transition-colors"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* User Summary Strip */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 bg-blue-50/50 px-6 py-3 border-b border-slate-100 text-xs">
          <div>
            <span className="text-slate-500 block">Total Claims</span>
            <span className="font-bold text-slate-800 text-sm">{user.totalClaims}</span>
          </div>
          <div>
            <span className="text-emerald-700 block">Approved</span>
            <span className="font-bold text-emerald-800 text-sm">
              {user.approvedClaims} ({formatCurrency(user.approvedAmount || 0)})
            </span>
          </div>
          <div>
            <span className="text-rose-700 block">Rejected</span>
            <span className="font-bold text-rose-800 text-sm">
              {user.rejectedClaims} ({formatCurrency(user.rejectedAmount || 0)})
            </span>
          </div>
          <div>
            <span className="text-slate-600 block">Total Volume</span>
            <span className="font-bold text-slate-900 text-sm">{formatCurrency(user.totalAmount || 0)}</span>
          </div>
        </div>

        {/* Claims Table */}
        <div className="flex-1 overflow-y-auto p-6">
          {claimsQuery.isLoading ? (
            <p className="text-center py-8 text-sm text-slate-500">Loading claims…</p>
          ) : matchingClaims.length === 0 ? (
            <div className="text-center py-10">
              <ClipboardList className="mx-auto h-10 w-10 text-slate-300 mb-2" />
              <p className="text-sm font-semibold text-slate-700">No matching claims found in current page</p>
              <p className="text-xs text-slate-500 mt-1">
                You can review all system claims under the All Claims management page.
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto rounded-lg border border-slate-200">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                  <tr>
                    <th className="px-4 py-2.5">Claim Number</th>
                    <th className="px-4 py-2.5">Type</th>
                    <th className="px-4 py-2.5">Amount</th>
                    <th className="px-4 py-2.5">Status</th>
                    <th className="px-4 py-2.5">Date</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {matchingClaims.map((c) => (
                    <tr key={c.id} className="hover:bg-slate-50/70">
                      <td className="px-4 py-3 font-semibold text-ink">
                        <Link
                          to={`/claims/${c.id}`}
                          className="hover:text-blue-600 inline-flex items-center gap-1"
                        >
                          {c.claimNumber}
                          <ArrowUpRight className="h-3.5 w-3.5 text-slate-400" />
                        </Link>
                      </td>
                      <td className="px-4 py-3 text-slate-600">{c.claimType}</td>
                      <td className="px-4 py-3 font-semibold text-slate-900">
                        {formatCurrency(c.claimAmount)}
                      </td>
                      <td className="px-4 py-3">
                        <StatusChip status={c.status} />
                      </td>
                      <td className="px-4 py-3 text-xs text-slate-500">{formatDate(c.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="border-t border-slate-200 bg-slate-50 px-6 py-3 flex justify-between items-center">
          <Link
            to="/admin/claims"
            className="text-xs font-semibold text-blue-600 hover:underline flex items-center gap-1"
          >
            Open All Claims Table <ArrowUpRight className="h-3 w-3" />
          </Link>
          <Button variant="secondary" onClick={onClose} className="px-4 text-xs">
            Close
          </Button>
        </div>
      </div>
    </div>
  );
}

// ── Proper Custom Tooltip for Charts ──────────────────────────────────────────
function CustomChartTooltip({
  active,
  payload,
  label,
  isCurrency = false,
}: {
  active?: boolean;
  payload?: Array<{ value: number; name?: string; color?: string; payload?: Record<string, unknown> }>;
  label?: string;
  isCurrency?: boolean;
}) {
  if (!active || !payload || !payload.length) return null;
  const item = payload[0];
  const formattedVal = isCurrency ? formatCurrency(item.value) : `${item.value.toLocaleString()} claims`;

  return (
    <div className="rounded-xl border border-slate-200 bg-white/95 backdrop-blur-md p-3 shadow-xl text-xs">
      <p className="font-bold text-slate-800 text-sm">{label || item.name}</p>
      <div className="mt-1.5 flex items-center gap-2">
        <span
          className="h-2.5 w-2.5 rounded-full shrink-0"
          style={{ backgroundColor: item.color || '#3b82f6' }}
        />
        <span className="font-semibold text-slate-700">{formattedVal}</span>
      </div>
      {Boolean(item.payload?.subtitle) && (
        <p className="mt-1 text-[11px] text-slate-500">{String(item.payload?.subtitle)}</p>
      )}
    </div>
  );
}

// ── Complete & Enhanced Admin Dashboard ───────────────────────────────────────
function AdminDashboard({ stats }: { stats: DashboardStats }) {
  const { session } = useAuth();
  const [activeRoleTab, setActiveRoleTab] = useState<'ALL' | Role>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedUserForDrilldown, setSelectedUserForDrilldown] = useState<UserClaimStat | null>(null);

  // Computed totals & financial statistics
  const totalClaims = stats.totalClaims || 0;
  const approvedClaims = stats.approvedClaims || stats.claimsByStatus?.APPROVED || 0;
  const rejectedClaims = stats.rejectedClaims || stats.claimsByStatus?.REJECTED || 0;
  const settledClaims = stats.settledClaims || approvedClaims + rejectedClaims;
  const pendingClaims =
    stats.pendingClaims !== undefined
      ? stats.pendingClaims
      : Math.max(0, totalClaims - settledClaims - (stats.claimsByStatus?.CLOSED || 0));

  const totalAmount = stats.totalClaimAmount || 0;
  const approvedAmount = stats.approvedAmount || 0;
  const rejectedAmount = stats.rejectedAmount || 0;
  const settledAmount = stats.settledAmount || approvedAmount + rejectedAmount;
  const pendingAmount = stats.pendingAmount || Math.max(0, totalAmount - settledAmount);

  const approvalRate =
    stats.approvalRate !== undefined
      ? stats.approvalRate
      : settledClaims > 0
      ? Math.round((approvedClaims / settledClaims) * 1000) / 10
      : 0;

  const rejectionRate =
    stats.rejectionRate !== undefined
      ? stats.rejectionRate
      : settledClaims > 0
      ? Math.round((rejectedClaims / settledClaims) * 1000) / 10
      : 0;

  // Chart 1: Claims by Status (sorted logically from intake -> decision)
  const orderedStatuses: ClaimStatus[] = [
    'SUBMITTED',
    'UNDER_REVIEW',
    'INVESTIGATION_REQUIRED',
    'UNDER_INVESTIGATION',
    'INVESTIGATION_COMPLETED',
    'APPROVED',
    'REJECTED',
    'CLOSED',
    'DRAFT',
  ];

  const statusChartData = useMemo(() => {
    return orderedStatuses
      .map((status) => {
        const count = stats.claimsByStatus?.[status] || 0;
        const cfg = STATUS_CONFIG[status] || { label: status, color: '#3b82f6' };
        const percent = totalClaims > 0 ? ((count / totalClaims) * 100).toFixed(1) : '0';
        return {
          status,
          name: cfg.label,
          count,
          color: cfg.color,
          subtitle: `${percent}% of total claims`,
        };
      })
      .filter((item) => item.count > 0 || ['APPROVED', 'REJECTED', 'SUBMITTED', 'UNDER_REVIEW'].includes(item.status));
  }, [stats.claimsByStatus, totalClaims]);

  // Chart 2: Financial Overview
  const financialChartData = useMemo(() => {
    return [
      { name: 'Total Requested', amount: totalAmount, color: '#0284c7' },
      { name: 'Approved Payout', amount: approvedAmount, color: '#10b981' },
      { name: 'Rejected Savings', amount: rejectedAmount, color: '#ef4444' },
      { name: 'Pending Review', amount: pendingAmount, color: '#f59e0b' },
    ];
  }, [totalAmount, approvedAmount, rejectedAmount, pendingAmount]);

  // Chart 3: Insurance Types Breakdown
  const typeChartData = useMemo(() => {
    if (!stats.claimsByType || stats.claimsByType.length === 0) return [];
    return stats.claimsByType.map((t) => ({
      name: t.type.charAt(0) + t.type.slice(1).toLowerCase(),
      total: t.totalClaims,
      approved: t.approvedClaims,
      amount: t.totalAmount,
    }));
  }, [stats.claimsByType]);

  // Filtered User Stats Table
  const filteredUserStats = useMemo(() => {
    if (!stats.userStats) return [];
    return stats.userStats.filter((u) => {
      const matchesRole = activeRoleTab === 'ALL' || u.role === activeRoleTab;
      const matchesSearch =
        searchQuery === '' ||
        u.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        u.email.toLowerCase().includes(searchQuery.toLowerCase());
      return matchesRole && matchesSearch;
    });
  }, [stats.userStats, activeRoleTab, searchQuery]);

  // CSV Export Functionality
  const handleExportCSV = () => {
    if (!stats.userStats) return;
    const headers = ['User ID', 'Name', 'Email', 'Role', 'Total Claims', 'Approved', 'Rejected', 'Pending', 'Total Amount', 'Approved Amount'];
    const rows = stats.userStats.map((u) => [
      u.userId,
      `"${u.name}"`,
      u.email,
      u.role,
      u.totalClaims,
      u.approvedClaims,
      u.rejectedClaims,
      u.pendingClaims,
      u.totalAmount || 0,
      u.approvedAmount || 0,
    ]);
    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map((r) => r.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `claimflow_admin_analytics_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <>
      {/* Modal for User Drilldown */}
      {selectedUserForDrilldown && (
        <UserClaimsDrilldownModal
          user={selectedUserForDrilldown}
          onClose={() => setSelectedUserForDrilldown(null)}
        />
      )}

      {/* Header & Quick Action Buttons */}
      <div className="mb-7 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
              Executive Command Center
            </h1>
            <span className="rounded-full bg-emerald-100 text-emerald-800 text-xs font-bold px-2.5 py-0.5 border border-emerald-300">
              Admin Live
            </span>
          </div>
          <p className="mt-1 text-sm text-slate-500">
            Welcome back, {session?.firstName || 'Vikram'}! Full operational overview, financial impact, and role metrics.
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            onClick={handleExportCSV}
            className="inline-flex items-center gap-1.5 rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 shadow-sm hover:bg-slate-50 hover:border-slate-400 transition-colors"
          >
            <Download className="h-3.5 w-3.5" />
            Export CSV
          </button>
          <Link
            to="/admin/claims"
            className="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-3.5 py-2 text-xs font-semibold text-white shadow-sm hover:bg-blue-700 transition-colors"
          >
            <FileText className="h-3.5 w-3.5" />
            All Claims & PDFs
          </Link>
          <Link
            to="/users"
            className="inline-flex items-center gap-1.5 rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 shadow-sm hover:bg-slate-50 transition-colors"
          >
            <Users className="h-3.5 w-3.5" />
            User Management
          </Link>
        </div>
      </div>

      {/* ── Key Operational KPI Metrics (6 Cards Grid) ────────────────────── */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 mb-7">
        <StatCard
          label="Total Claims"
          value={totalClaims}
          subtext={`Vol: ${formatCurrency(totalAmount)}`}
          icon={ClipboardList}
          tone="text-blue-600"
          bgTone="bg-blue-50"
        />
        <StatCard
          label="Approved Claims"
          value={approvedClaims}
          subtext={`Paid: ${formatCurrency(approvedAmount)}`}
          icon={CheckCircle2}
          tone="text-emerald-600"
          bgTone="bg-emerald-50"
        />
        <StatCard
          label="Rejected Claims"
          value={rejectedClaims}
          subtext={`Saved: ${formatCurrency(rejectedAmount)}`}
          icon={CircleX}
          tone="text-rose-600"
          bgTone="bg-rose-50"
        />
        <StatCard
          label="Settled (Appr + Rej)"
          value={settledClaims}
          subtext={`Total: ${formatCurrency(settledAmount)}`}
          icon={Scale}
          tone="text-purple-600"
          bgTone="bg-purple-50"
        />
        <StatCard
          label="Active / In Progress"
          value={pendingClaims}
          subtext={`Pending: ${formatCurrency(pendingAmount)}`}
          icon={Clock3}
          tone="text-amber-600"
          bgTone="bg-amber-50"
        />
        <Card className="p-5 shadow-sm border border-slate-200/80">
          <div className="flex items-start justify-between">
            <div className="min-w-0 flex-1">
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">Approval Rate</p>
              <p className="mt-2 text-2xl font-extrabold text-slate-900 tracking-tight">
                {approvalRate}%
              </p>
              <div className="mt-2 w-full bg-rose-100 rounded-full h-2 overflow-hidden flex">
                <div
                  className="bg-emerald-500 h-2 rounded-l-full transition-all duration-500"
                  style={{ width: `${Math.min(100, Math.max(0, approvalRate))}%` }}
                />
                <div
                  className="bg-rose-500 h-2 rounded-r-full transition-all duration-500"
                  style={{ width: `${Math.min(100, Math.max(0, rejectionRate))}%` }}
                />
              </div>
              <p className="mt-1.5 text-[11px] text-slate-500 flex justify-between font-medium">
                <span className="text-emerald-700">Appr: {approvalRate}%</span>
                <span className="text-rose-700">Rej: {rejectionRate}%</span>
              </p>
            </div>
            <span className="rounded-xl p-3 shrink-0 ml-3 bg-emerald-50 text-emerald-600">
              <TrendingUp className="h-6 w-6" />
            </span>
          </div>
        </Card>
      </div>

      {/* ── High-Quality Analytical Graphs (Side by Side Grid) ────────────── */}
      <div className="grid gap-6 lg:grid-cols-2 mb-7">
        {/* Graph 1: Claims by Status Distribution */}
        <Card className="p-6 shadow-sm border border-slate-200">
          <div className="flex items-center justify-between border-b border-slate-100 pb-4 mb-5">
            <div>
              <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <BarChart3 className="h-5 w-5 text-blue-600" />
                Claims by Processing Status
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">Real-time volume distribution across workflow phases</p>
            </div>
            <span className="text-xs font-semibold px-2 py-1 rounded bg-slate-100 text-slate-700">
              {totalClaims} Total
            </span>
          </div>

          <div className="h-80 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart
                data={statusChartData}
                margin={{ top: 15, right: 15, left: -10, bottom: 45 }}
              >
                <CartesianGrid vertical={false} strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis
                  dataKey="name"
                  tick={{ fontSize: 11, fill: '#64748b' }}
                  interval={0}
                  angle={-30}
                  textAnchor="end"
                  height={50}
                />
                <YAxis allowDecimals={false} tick={{ fontSize: 11, fill: '#64748b' }} />
                <Tooltip content={<CustomChartTooltip />} />
                <Bar dataKey="count" radius={[6, 6, 0, 0]}>
                  {statusChartData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>

          {/* Quick Status Legend Strip */}
          <div className="mt-3 pt-3 border-t border-slate-100 flex flex-wrap items-center justify-center gap-3 text-xs">
            <span className="inline-flex items-center gap-1.5 text-slate-600 font-medium">
              <span className="h-2.5 w-2.5 rounded-full bg-emerald-500" /> Approved
            </span>
            <span className="inline-flex items-center gap-1.5 text-slate-600 font-medium">
              <span className="h-2.5 w-2.5 rounded-full bg-rose-500" /> Rejected
            </span>
            <span className="inline-flex items-center gap-1.5 text-slate-600 font-medium">
              <span className="h-2.5 w-2.5 rounded-full bg-sky-500" /> Review / Active
            </span>
            <span className="inline-flex items-center gap-1.5 text-slate-600 font-medium">
              <span className="h-2.5 w-2.5 rounded-full bg-amber-500" /> Investigation
            </span>
          </div>
        </Card>

        {/* Graph 2: Financial Overview & Settlement Impact */}
        <Card className="p-6 shadow-sm border border-slate-200">
          <div className="flex items-center justify-between border-b border-slate-100 pb-4 mb-5">
            <div>
              <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <IndianRupee className="h-5 w-5 text-emerald-600" />
                Financial Settlement Overview
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">Value breakdown: Requested, Approved, Rejected & Pending</p>
            </div>
            <span className="text-xs font-semibold px-2 py-1 rounded bg-emerald-50 text-emerald-700">
              {formatCurrency(totalAmount)}
            </span>
          </div>

          <div className="h-80 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart
                data={financialChartData}
                margin={{ top: 15, right: 15, left: 10, bottom: 25 }}
              >
                <CartesianGrid vertical={false} strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="name" tick={{ fontSize: 11, fill: '#64748b' }} />
                <YAxis
                  tickFormatter={(val) => `₹${(val / 1000).toFixed(0)}k`}
                  tick={{ fontSize: 11, fill: '#64748b' }}
                />
                <Tooltip content={<CustomChartTooltip isCurrency />} />
                <Bar dataKey="amount" radius={[6, 6, 0, 0]}>
                  {financialChartData.map((entry, index) => (
                    <Cell key={`cell-fin-${index}`} fill={entry.color} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>

          {/* Settlement Rate Callout */}
          <div className="mt-3 pt-3 border-t border-slate-100 grid grid-cols-2 text-center text-xs">
            <div className="border-r border-slate-100 pr-2">
              <span className="text-slate-500 block">Total Approved Payout</span>
              <span className="font-bold text-emerald-600 text-sm">{formatCurrency(approvedAmount)}</span>
            </div>
            <div className="pl-2">
              <span className="text-slate-500 block">Protected Fraud / Rejected Savings</span>
              <span className="font-bold text-rose-600 text-sm">{formatCurrency(rejectedAmount)}</span>
            </div>
          </div>
        </Card>
      </div>

      {/* ── Insurance Type Breakdown (if types data available) ──────────────── */}
      {typeChartData.length > 0 && (
        <Card className="p-6 mb-7 shadow-sm border border-slate-200">
          <div className="flex items-center justify-between border-b border-slate-100 pb-4 mb-4">
            <div>
              <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <Layers className="h-5 w-5 text-indigo-600" />
                Performance by Insurance Category
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">Claim volume and approved count per policy type</p>
            </div>
          </div>

          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-7">
            {typeChartData.map((t) => (
              <div
                key={t.name}
                className="rounded-xl border border-slate-200/80 bg-slate-50/60 p-3.5 hover:bg-slate-50 transition-colors"
              >
                <p className="text-xs font-bold text-slate-700 uppercase tracking-wide">{t.name}</p>
                <p className="mt-1 text-xl font-extrabold text-slate-900">{t.total}</p>
                <div className="mt-2 text-xs flex justify-between text-slate-500 border-t border-slate-200/60 pt-1.5">
                  <span className="text-emerald-700 font-semibold">{t.approved} Approved</span>
                  <span>{formatCurrency(t.amount)}</span>
                </div>
              </div>
            ))}
          </div>
        </Card>
      )}

      {/* ── Individual Claims Breakdown by Customer & Staff ─────────────────── */}
      <Card className="overflow-hidden shadow-sm border border-slate-200">
        {/* Table Header & Controls */}
        <div className="border-b border-slate-200 bg-white px-6 py-5">
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div>
              <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
                <Users className="h-5 w-5 text-blue-600" />
                Individual Claim Counts & Performance by User
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">
                Detailed acceptance and rejection breakdown for every Customer, Supervisor, Claim Agent, and Investigator
              </p>
            </div>

            {/* Live Search */}
            <div className="relative w-full sm:w-64">
              <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
              <input
                type="text"
                placeholder="Search by name or email…"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full rounded-lg border border-slate-300 bg-slate-50/50 pl-9 pr-3 py-1.5 text-xs text-slate-800 placeholder:text-slate-400 focus:bg-white focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 transition-colors"
              />
              {searchQuery && (
                <button
                  onClick={() => setSearchQuery('')}
                  className="absolute right-2.5 top-2 text-slate-400 hover:text-slate-600"
                >
                  <X className="h-3.5 w-3.5" />
                </button>
              )}
            </div>
          </div>

          {/* Role Filter Tabs */}
          <div className="mt-4 flex flex-wrap items-center gap-2 border-t border-slate-100 pt-3">
            <span className="text-xs font-semibold text-slate-500 mr-1 flex items-center gap-1">
              <Filter className="h-3.5 w-3.5" /> Filter Role:
            </span>
            {(
              [
                { id: 'ALL', label: 'All Users' },
                { id: 'CUSTOMER', label: 'Customers' },
                { id: 'CLAIM_AGENT', label: 'Claim Agents' },
                { id: 'INVESTIGATOR', label: 'Investigators' },
                { id: 'SUPERVISOR', label: 'Supervisors' },
              ] as const
            ).map((tab) => {
              const active = activeRoleTab === tab.id;
              const count =
                tab.id === 'ALL'
                  ? stats.userStats?.length || 0
                  : stats.userStats?.filter((u) => u.role === tab.id).length || 0;

              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveRoleTab(tab.id)}
                  className={`inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-semibold transition-all ${
                    active
                      ? 'bg-blue-600 text-white shadow-sm shadow-blue-500/20'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200/80 hover:text-slate-900'
                  }`}
                >
                  {tab.label}
                  <span
                    className={`rounded-full px-1.5 py-0.2 text-[10px] ${
                      active ? 'bg-white/25 text-white' : 'bg-slate-200 text-slate-700'
                    }`}
                  >
                    {count}
                  </span>
                </button>
              );
            })}
          </div>
        </div>

        {/* User Breakdown Table */}
        {filteredUserStats.length === 0 ? (
          <div className="py-12 text-center">
            <Users className="mx-auto h-12 w-12 text-slate-300 mb-2" />
            <p className="text-sm font-semibold text-slate-700">No users match your filter</p>
            <p className="text-xs text-slate-500 mt-1">Try clearing the search or switching the role tab.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500 border-b border-slate-200">
                <tr>
                  <th className="px-5 py-3">User & Role</th>
                  <th className="px-5 py-3 text-center">Total Claims</th>
                  <th className="px-5 py-3">Accepted (Approved)</th>
                  <th className="px-5 py-3">Rejected</th>
                  <th className="px-5 py-3">In Review / Pending</th>
                  <th className="px-5 py-3">Total Volume</th>
                  <th className="px-5 py-3">Approval Rate</th>
                  <th className="px-5 py-3 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredUserStats.map((user) => {
                  const roleBadgeColor =
                    user.role === 'CUSTOMER'
                      ? 'bg-sky-50 text-sky-700 border-sky-200'
                      : user.role === 'CLAIM_AGENT'
                      ? 'bg-indigo-50 text-indigo-700 border-indigo-200'
                      : user.role === 'INVESTIGATOR'
                      ? 'bg-amber-50 text-amber-700 border-amber-200'
                      : user.role === 'SUPERVISOR'
                      ? 'bg-purple-50 text-purple-700 border-purple-200'
                      : 'bg-emerald-50 text-emerald-700 border-emerald-200';

                  const settledCount = user.approvedClaims + user.rejectedClaims;
                  const rate =
                    settledCount > 0 ? Math.round((user.approvedClaims / settledCount) * 100) : 0;

                  return (
                    <tr key={user.userId} className="hover:bg-slate-50/70 transition-colors">
                      {/* User Info */}
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-3">
                          <div className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-slate-100 font-bold text-xs text-slate-700">
                            {user.name.slice(0, 2).toUpperCase()}
                          </div>
                          <div>
                            <p className="font-bold text-slate-900 leading-snug">{user.name}</p>
                            <p className="text-xs text-slate-500">{user.email}</p>
                            <span
                              className={`inline-block mt-1 rounded-full px-2 py-0.5 text-[10px] font-semibold border ${roleBadgeColor}`}
                            >
                              {user.role.replace('_', ' ')}
                            </span>
                          </div>
                        </div>
                      </td>

                      {/* Total Claims Count */}
                      <td className="px-5 py-4 text-center">
                        <span className="inline-block rounded-lg bg-slate-100 px-3 py-1 font-bold text-slate-800 text-sm">
                          {user.totalClaims}
                        </span>
                      </td>

                      {/* Accepted */}
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-2">
                          <span className="inline-flex items-center gap-1 rounded-md bg-emerald-50 px-2.5 py-1 text-xs font-bold text-emerald-700 border border-emerald-200">
                            <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600" />
                            {user.approvedClaims}
                          </span>
                          {user.approvedAmount > 0 && (
                            <span className="text-xs font-semibold text-emerald-800 hidden sm:inline">
                              {formatCurrency(user.approvedAmount)}
                            </span>
                          )}
                        </div>
                      </td>

                      {/* Rejected */}
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-2">
                          <span className="inline-flex items-center gap-1 rounded-md bg-rose-50 px-2.5 py-1 text-xs font-bold text-rose-700 border border-rose-200">
                            <CircleX className="h-3.5 w-3.5 text-rose-600" />
                            {user.rejectedClaims}
                          </span>
                          {user.rejectedAmount > 0 && (
                            <span className="text-xs font-semibold text-rose-800 hidden sm:inline">
                              {formatCurrency(user.rejectedAmount)}
                            </span>
                          )}
                        </div>
                      </td>

                      {/* Pending */}
                      <td className="px-5 py-4">
                        <span className="inline-flex items-center gap-1 rounded-md bg-amber-50 px-2.5 py-1 text-xs font-bold text-amber-700 border border-amber-200">
                          <Clock3 className="h-3.5 w-3.5 text-amber-600" />
                          {user.pendingClaims}
                        </span>
                      </td>

                      {/* Total Amount */}
                      <td className="px-5 py-4 font-bold text-slate-900">
                        {formatCurrency(user.totalAmount || 0)}
                      </td>

                      {/* Approval Rate */}
                      <td className="px-5 py-4">
                        {settledCount > 0 ? (
                          <div className="w-28">
                            <div className="flex items-center justify-between text-xs font-semibold mb-1">
                              <span className="text-slate-700">{rate}%</span>
                              <span className="text-[10px] text-slate-400">
                                {user.approvedClaims}/{settledCount}
                              </span>
                            </div>
                            <div className="w-full bg-slate-200 rounded-full h-1.5 overflow-hidden">
                              <div
                                className="bg-emerald-500 h-1.5 rounded-full"
                                style={{ width: `${rate}%` }}
                              />
                            </div>
                          </div>
                        ) : (
                          <span className="text-xs text-slate-400 italic">No decisions yet</span>
                        )}
                      </td>

                      {/* Drilldown Action */}
                      <td className="px-5 py-4 text-right">
                        <button
                          onClick={() => setSelectedUserForDrilldown(user)}
                          className="inline-flex items-center gap-1 rounded-lg border border-slate-300 bg-white px-2.5 py-1.5 text-xs font-semibold text-slate-700 shadow-sm hover:bg-slate-100 hover:text-blue-600 transition-colors"
                        >
                          <Eye className="h-3.5 w-3.5" />
                          View Claims
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </>
  );
}
