import { STATUS_LABELS, STATUS_STYLES } from '@/constants/app'; import type { ClaimStatus } from '@/types';
export function StatusChip({ status }: { status: ClaimStatus }) { return <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${STATUS_STYLES[status]}`}>{STATUS_LABELS[status]}</span>; }
