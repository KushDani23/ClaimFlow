import { useQuery } from '@tanstack/react-query';
import { BadgeCheck, CalendarDays, Mail, ShieldCheck, UserRound } from 'lucide-react';
import { Card, PageLoader } from '@/components/ui';
import { authService } from '@/services/authService';
import { formatDate, titleCase } from '@/utils/format';

export function ProfilePage() {
  const profileQuery = useQuery({ queryKey: ['profile'], queryFn: authService.profile });

  if (profileQuery.isLoading) return <PageLoader />;
  if (profileQuery.isError || !profileQuery.data) {
    return <Card className="max-w-2xl p-6"><h1 className="text-xl font-bold text-ink">Profile unavailable</h1><p className="mt-2 text-sm text-slate-500">We could not load your profile details. Please sign in again and retry.</p></Card>;
  }

  const profile = profileQuery.data;
  const details = [
    { label: 'Full name', value: `${profile.firstName} ${profile.lastName}`, icon: UserRound },
    { label: 'Email address', value: profile.email, icon: Mail },
    { label: 'Role', value: titleCase(profile.role), icon: ShieldCheck },
    { label: 'Member since', value: formatDate(profile.createdAt), icon: CalendarDays }
  ];

  return <div className="max-w-3xl"><div className="mb-7"><h1 className="text-2xl font-bold text-ink">My profile</h1><p className="mt-1 text-sm text-slate-500">Your ClaimFlow account information.</p></div><Card className="overflow-hidden"><div className="bg-gradient-to-r from-brand to-sky-500 px-6 py-7 text-white"><div className="flex items-center gap-4"><span className="grid h-16 w-16 place-items-center rounded-full bg-white/20"><UserRound className="h-8 w-8" /></span><div><h2 className="text-xl font-bold">{profile.firstName} {profile.lastName}</h2><p className="mt-1 text-sm text-blue-50">{titleCase(profile.role)}</p></div></div></div><div className="divide-y divide-slate-100 px-6">{details.map(({ label, value, icon: Icon }) => <div key={label} className="flex items-center gap-4 py-5"><span className="rounded-lg bg-sky-50 p-2 text-brand"><Icon className="h-5 w-5" /></span><div><p className="text-xs font-semibold uppercase tracking-wide text-slate-400">{label}</p><p className="mt-1 text-sm font-medium text-ink">{value}</p></div></div>)}<div className="flex items-center gap-4 py-5"><span className="rounded-lg bg-emerald-50 p-2 text-emerald-600"><BadgeCheck className="h-5 w-5" /></span><div><p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Account status</p><p className="mt-1 text-sm font-medium text-ink">{profile.enabled ? 'Active' : 'Inactive'}</p></div></div></div></Card></div>;
}
