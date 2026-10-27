import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, ExternalLink, FileText, Paperclip, X } from 'lucide-react';
import toast from 'react-hot-toast';
import { Button, Card, EmptyState, PageLoader, Select } from '@/components/ui';
import { Pagination } from '@/components/Pagination';
import { StatusChip } from '@/components/StatusChip';
import { ROLES } from '@/constants/app';
import { adminService } from '@/services/adminService';
import { documentService } from '@/services/documentService';
import { formatBytes, formatCurrency, formatDate } from '@/utils/format';
import type { Role } from '@/types';

// ── Inline documents panel for Admin ──────────────────────────────────────────
function AdminClaimDocumentsPanel({ claimId, onClose }: { claimId: number; onClose: () => void }) {
  const docs = useQuery({
    queryKey: ['documents', claimId],
    queryFn: () => documentService.list(claimId),
  });
  const download = useMutation({
    mutationFn: ({ id, fileName }: { id: number; fileName: string }) => documentService.download(id, fileName),
  });
  const view = useMutation({
    mutationFn: (id: number) => documentService.view(id),
  });

  return (
    <div className="border-t border-blue-100 bg-blue-50/70 px-6 py-4">
      <div className="flex items-center justify-between mb-3">
        <p className="text-sm font-semibold text-blue-900 flex items-center gap-1.5">
          <FileText className="h-4 w-4 text-blue-700" />
          Attached Customer Documents & PDFs
        </p>
        <button
          onClick={onClose}
          className="text-slate-400 hover:text-slate-600 rounded p-1 transition-colors"
          title="Close panel"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      {docs.isLoading ? (
        <p className="text-xs text-slate-500 py-2">Loading documents…</p>
      ) : docs.data && docs.data.length > 0 ? (
        <div className="space-y-2">
          {docs.data.map((doc) => (
            <div
              key={doc.id}
              className="flex items-center gap-3 rounded-lg border border-blue-200 bg-white px-3.5 py-2.5 shadow-sm"
            >
              <FileText className="h-4 w-4 shrink-0 text-red-500" />
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-slate-800">{doc.fileName}</p>
                <p className="text-xs text-slate-500">
                  {formatBytes(doc.fileSize)} · Uploaded {doc.uploadedAt ? formatDate(doc.uploadedAt) : 'Recently'}
                </p>
              </div>
              <div className="flex items-center gap-1 shrink-0">
                <button
                  type="button"
                  title="View PDF in new tab"
                  onClick={() => view.mutate(doc.id)}
                  className="flex items-center gap-1 rounded px-2.5 py-1 text-xs font-semibold text-blue-700 hover:bg-blue-100 transition-colors"
                >
                  <ExternalLink className="h-3.5 w-3.5" />
                  View
                </button>
                <button
                  type="button"
                  title="Download document"
                  onClick={() => download.mutate({ id: doc.id, fileName: doc.fileName })}
                  className="flex items-center gap-1 rounded px-2.5 py-1 text-xs font-semibold text-slate-700 hover:bg-slate-100 transition-colors"
                >
                  <Download className="h-3.5 w-3.5" />
                  Download
                </button>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <p className="text-xs text-slate-500 py-2 italic">No documents attached to this claim.</p>
      )}
    </div>
  );
}

// ── Users Management Page ────────────────────────────────────────────────────
export function UsersPage() {
  const [page, setPage] = useState(0);
  const client = useQueryClient();

  const query = useQuery({
    queryKey: ['users', page],
    queryFn: () => adminService.users({ page, size: 10 }),
  });

  const update = useMutation({
    mutationFn: ({ id, role }: { id: number; role: Role }) => adminService.updateRole(id, role),
    onSuccess: () => {
      toast.success('User role updated');
      client.invalidateQueries({ queryKey: ['users'] });
    },
  });

  const toggle = useMutation({
    mutationFn: adminService.toggleUser,
    onSuccess: () => {
      toast.success('User status updated');
      client.invalidateQueries({ queryKey: ['users'] });
    },
  });

  if (query.isLoading) return <PageLoader />;

  return (
    <>
      <div className="mb-7">
        <h1 className="text-2xl font-bold text-ink">User management</h1>
        <p className="mt-1 text-sm text-slate-500">Manage system access and user roles.</p>
      </div>

      <Card className="overflow-hidden">
        {query.data?.content.length ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-5 py-3">User</th>
                  <th className="px-5 py-3">Role</th>
                  <th className="px-5 py-3">Created</th>
                  <th className="px-5 py-3">Status</th>
                  <th className="px-5 py-3" />
                </tr>
              </thead>
              <tbody>
                {query.data.content.map((user) => (
                  <tr key={user.id} className="border-t border-slate-100">
                    <td className="px-5 py-4">
                      <p className="font-semibold text-ink">
                        {user.firstName} {user.lastName}
                      </p>
                      <p className="text-xs text-slate-500">{user.email}</p>
                    </td>
                    <td className="px-5 py-4">
                      <Select
                        className="h-9 min-w-36"
                        value={user.role}
                        onChange={(event) => update.mutate({ id: user.id, role: event.target.value as Role })}
                      >
                        {ROLES.map((role) => (
                          <option key={role}>{role}</option>
                        ))}
                      </Select>
                    </td>
                    <td className="px-5 py-4 text-slate-600">{formatDate(user.createdAt)}</td>
                    <td className="px-5 py-4">
                      <span
                        className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
                          user.enabled ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600'
                        }`}
                      >
                        {user.enabled ? 'Active' : 'Disabled'}
                      </span>
                    </td>
                    <td className="px-5 py-4 text-right">
                      <Button
                        variant="secondary"
                        className="min-h-8 px-2.5 text-xs"
                        onClick={() => toggle.mutate(user.id)}
                      >
                        {user.enabled ? 'Disable' : 'Enable'}
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState title="No users found" description="Users will appear here once registered." />
        )}
        {query.data && <Pagination page={query.data} onPageChange={setPage} />}
      </Card>
    </>
  );
}

// ── Admin All Claims Page with Document Viewer/Downloader ────────────────────
export function AdminClaimsPage() {
  const [page, setPage] = useState(0);
  const [expandedDocsClaim, setExpandedDocsClaim] = useState<number | null>(null);

  const query = useQuery({
    queryKey: ['admin-claims', page],
    queryFn: () => adminService.claims({ page, size: 10 }),
  });

  if (query.isLoading) return <PageLoader />;

  return (
    <>
      <div className="mb-7">
        <h1 className="text-2xl font-bold text-ink">All claims</h1>
        <p className="mt-1 text-sm text-slate-500">
          Full administrative view across all claims, statuses, and customer-uploaded documents.
        </p>
      </div>

      <Card className="overflow-hidden">
        {query.data?.content.length ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-5 py-3">Claim</th>
                  <th className="px-5 py-3">Customer</th>
                  <th className="px-5 py-3">Amount</th>
                  <th className="px-5 py-3">Agent</th>
                  <th className="px-5 py-3">Status</th>
                  <th className="px-5 py-3 text-right">Documents</th>
                </tr>
              </thead>
              <tbody>
                {query.data.content.map((claim) => (
                  <>
                    <tr key={claim.id} className="border-t border-slate-100 hover:bg-slate-50/50 transition-colors">
                      <td className="px-5 py-4">
                        <p className="font-semibold text-ink">{claim.claimNumber}</p>
                        <p className="text-xs text-slate-500">
                          {claim.claimType} · {claim.policyNumber}
                        </p>
                      </td>
                      <td className="px-5 py-4 text-slate-600 font-medium">
                        {claim.customerName}
                      </td>
                      <td className="px-5 py-4 text-slate-800 font-semibold">
                        {formatCurrency(claim.claimAmount)}
                      </td>
                      <td className="px-5 py-4 text-slate-600">
                        {claim.assignedAgent ? (
                          <span className="inline-flex items-center gap-1 text-slate-700">
                            {claim.assignedAgent}
                          </span>
                        ) : (
                          <span className="text-slate-400 italic">Unassigned</span>
                        )}
                      </td>
                      <td className="px-5 py-4">
                        <StatusChip status={claim.status} />
                      </td>
                      <td className="px-5 py-4 text-right">
                        <button
                          type="button"
                          onClick={() =>
                            setExpandedDocsClaim(expandedDocsClaim === claim.id ? null : claim.id)
                          }
                          className={`inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-semibold transition-colors ${
                            expandedDocsClaim === claim.id
                              ? 'bg-blue-600 text-white shadow-sm'
                              : 'bg-slate-100 text-slate-700 hover:bg-blue-50 hover:text-blue-700'
                          }`}
                        >
                          <Paperclip className="h-3.5 w-3.5" />
                          {expandedDocsClaim === claim.id ? 'Hide PDFs' : 'View PDFs'}
                        </button>
                      </td>
                    </tr>

                    {expandedDocsClaim === claim.id && (
                      <tr key={`docs-${claim.id}`}>
                        <td colSpan={6} className="p-0">
                          <AdminClaimDocumentsPanel
                            claimId={claim.id}
                            onClose={() => setExpandedDocsClaim(null)}
                          />
                        </td>
                      </tr>
                    )}
                  </>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState title="No claims found" description="Claim activity will appear here." />
        )}
        {query.data && <Pagination page={query.data} onPageChange={setPage} />}
      </Card>
    </>
  );
}
