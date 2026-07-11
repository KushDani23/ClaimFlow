import { useEffect } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { useNavigate, useParams } from 'react-router-dom';
import { z } from 'zod';
import toast from 'react-hot-toast';
import { Button, Card, Field, Input, PageLoader, Select, Textarea } from '@/components/ui';
import { CLAIM_TYPES } from '@/constants/app';
import { claimService } from '@/services/claimService';
import type { ClaimPayload, ClaimType } from '@/types';

const schema = z.object({
  claimType: z.enum(CLAIM_TYPES as [ClaimType, ...ClaimType[]]),
  description: z.string().min(10, 'Describe the incident in at least 10 characters').max(2000),
  incidentDate: z.string().min(1, 'Incident date is required').refine((value) => new Date(value) <= new Date(), 'Incident date cannot be in the future'),
  claimAmount: z.coerce.number().positive('Amount must be greater than zero'),
  policyNumber: z.string().min(1, 'Policy number is required')
});
type FormData = z.infer<typeof schema>;

export function ClaimFormPage() {
  const { id } = useParams();
  const editing = Boolean(id);
  const navigate = useNavigate();
  const client = useQueryClient();
  const existing = useQuery({ queryKey: ['claim', id], queryFn: () => claimService.get(Number(id)), enabled: editing });
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<FormData>({ resolver: zodResolver(schema) });
  const mutation = useMutation({
    mutationFn: (data: ClaimPayload) => editing ? claimService.update(Number(id), data) : claimService.create(data),
    onSuccess: (claim) => { toast.success(editing ? 'Claim updated' : 'Draft claim created'); client.invalidateQueries({ queryKey: ['claims'] }); navigate(`/claims/${claim.id}`); }
  });

  useEffect(() => {
    if (existing.data) reset({ claimType: existing.data.claimType, description: existing.data.description, incidentDate: existing.data.incidentDate, claimAmount: existing.data.claimAmount, policyNumber: existing.data.policyNumber });
  }, [existing.data, reset]);

  if (editing && existing.isLoading) return <PageLoader />;

  return <><div className="mb-7"><h1 className="text-2xl font-bold text-ink">{editing ? 'Update draft claim' : 'Start a new claim'}</h1><p className="mt-1 text-sm text-slate-500">Provide accurate details to help us assess your claim.</p></div><Card className="max-w-3xl p-5 sm:p-7"><form className="space-y-5" onSubmit={handleSubmit((data) => mutation.mutate(data))}><div className="grid gap-5 sm:grid-cols-2"><Field label="Claim type" error={errors.claimType?.message}><Select {...register('claimType')}><option value="">Select claim type</option>{CLAIM_TYPES.map((item) => <option key={item} value={item}>{item}</option>)}</Select></Field><Field label="Policy number" error={errors.policyNumber?.message}><Input placeholder="e.g. POL-2026-001" {...register('policyNumber')} /></Field><Field label="Incident date" error={errors.incidentDate?.message}><Input type="date" max={new Date().toISOString().split('T')[0]} {...register('incidentDate')} /></Field><Field label="Claim amount (INR)" error={errors.claimAmount?.message}><Input type="number" min="1" step="0.01" placeholder="0.00" {...register('claimAmount')} /></Field></div><Field label="Incident description" error={errors.description?.message}><Textarea placeholder="Tell us what happened, including relevant circumstances and impact." {...register('description')} /></Field><div className="flex flex-wrap justify-end gap-3 border-t border-slate-100 pt-5"><Button type="button" variant="secondary" onClick={() => navigate(-1)}>Cancel</Button><Button type="submit" loading={isSubmitting || mutation.isPending}>{editing ? 'Save changes' : 'Save draft'}</Button></div></form></Card></>;
}
