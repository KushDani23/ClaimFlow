import { useEffect, useState } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { Check as CheckIcon, Copy, Eye, EyeOff, Info, LockKeyhole, ShieldAlert, Sparkles, X } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { z } from 'zod';
import toast from 'react-hot-toast';
import { Button, Field, Input } from '@/components/ui';
import { useAuth } from '@/contexts/AuthContext';

// ── Dummy credentials configured for each role ─────────────────────────────
const DUMMY_CREDENTIALS: Record<string, { label: string; email: string; password: string }> = {
  ADMIN: { label: 'Admin', email: 'dummy_admin@gmail.com', password: 'dummyadmin#@1234' },
  SUPERVISOR: { label: 'Supervisor', email: 'dummy_supervisor@gmail.com', password: 'dummysupervisor#@1234' },
  CLAIM_AGENT: { label: 'Claim Agent', email: 'dummy_claimagent@gmail.com', password: 'dummyclaimagent#@1234' },
  INVESTIGATOR: { label: 'Investigator', email: 'dummy_investigator@gmail.com', password: 'dummyinvestigator#@1234' },
  CUSTOMER: { label: 'Customer', email: 'dummy_customer@gmail.com', password: 'dummycustomer#@1234' },
};

// ── Role options in dropdown ───────────────────────────────────────────────
const ROLE_OPTIONS = [
  { value: 'CUSTOMER', label: 'Customer' },
  { value: 'CLAIM_AGENT', label: 'Claim Agent' },
  { value: 'INVESTIGATOR', label: 'Investigator' },
  { value: 'SUPERVISOR', label: 'Supervisor' },
  { value: 'ADMIN', label: 'Admin' },
];

// ── Form Validation Schema ──────────────────────────────────────────────────
const schema = z.object({
  email: z.string().email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
  remember: z.boolean().optional(),
});
type FormData = z.infer<typeof schema>;

// ── Copy Button Component ──────────────────────────────────────────────────
function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false);
  return (
    <button
      type="button"
      title="Copy"
      onClick={() => {
        navigator.clipboard.writeText(text).then(() => {
          setCopied(true);
          setTimeout(() => setCopied(false), 2000);
        });
      }}
      className="ml-2 rounded p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition-colors"
    >
      {copied ? <CheckIcon className="h-3.5 w-3.5 text-green-500" /> : <Copy className="h-3.5 w-3.5" />}
    </button>
  );
}

// ── Alert 1: Invalid Credentials (closes in 3 seconds) ──────────────────────
function InvalidCredentialsModal({ countdown, onClose }: { countdown: number; onClose: () => void }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 p-4 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="w-full max-w-md rounded-2xl border border-red-200 bg-white p-6 shadow-2xl">
        <div className="flex items-start gap-4">
          <div className="grid h-12 w-12 shrink-0 place-items-center rounded-full bg-red-100">
            <ShieldAlert className="h-6 w-6 text-red-600" />
          </div>
          <div className="flex-1">
            <h3 className="text-lg font-bold text-red-800">Invalid credentials</h3>
            <p className="mt-1 text-sm font-medium text-red-600">
              Invalid credentials.
            </p>
            <div className="mt-4 flex items-center justify-between rounded-lg bg-red-50 px-3 py-2 text-xs text-red-700">
              <span>Closing automatically in:</span>
              <span className="font-mono font-bold text-sm bg-red-200 px-2 py-0.5 rounded text-red-900">{countdown}s</span>
            </div>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-600 rounded p-1">
            <X className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Alert 2: Dummy Mode Suggestion ──────────────────────────────────────────
function DummySuggestionModal({
  role,
  onClose,
  onUseCredentials,
}: {
  role: string;
  onClose: () => void;
  onUseCredentials: (email: string, password: string) => void;
}) {
  const dummy = DUMMY_CREDENTIALS[role] || DUMMY_CREDENTIALS.ADMIN;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 p-4 backdrop-blur-sm animate-in zoom-in-95 duration-200">
      <div className="w-full max-w-md rounded-2xl border border-blue-200 bg-white shadow-2xl overflow-hidden">
        <div className="flex items-start justify-between bg-gradient-to-r from-blue-900 to-indigo-900 p-5 text-white">
          <div className="flex items-center gap-3">
            <div className="grid h-10 w-10 place-items-center rounded-xl bg-blue-500/20 backdrop-blur-sm border border-blue-300/30">
              <Sparkles className="h-5 w-5 text-blue-300" />
            </div>
            <div>
              <h3 className="text-base font-bold">Try Dummy {dummy.label} Mode</h3>
              <p className="text-xs text-blue-200 mt-0.5">Explore the system with pre-configured dummy access</p>
            </div>
          </div>
          <button onClick={onClose} className="text-blue-300 hover:text-white rounded p-1">
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="p-6">
          <p className="text-sm text-slate-700 leading-relaxed font-medium">
            You can try dummy {dummy.label.toLowerCase()} mode with these credentials and password:
          </p>

          <div className="mt-4 space-y-2.5 rounded-xl border border-slate-200 bg-slate-50 p-4 font-mono text-sm">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400 w-20 shrink-0">Email</span>
              <span className="flex-1 text-slate-800 font-medium truncate">{dummy.email}</span>
              <CopyButton text={dummy.email} />
            </div>
            <div className="border-t border-slate-200/60 pt-2 flex items-center justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400 w-20 shrink-0">Password</span>
              <span className="flex-1 text-slate-800 font-medium truncate">{dummy.password}</span>
              <CopyButton text={dummy.password} />
            </div>
          </div>

          <div className="mt-6 flex flex-col-reverse sm:flex-row sm:justify-end gap-2.5">
            <Button variant="secondary" onClick={onClose} className="sm:w-auto w-full">
              Close
            </Button>
            <Button
              onClick={() => onUseCredentials(dummy.email, dummy.password)}
              className="sm:w-auto w-full bg-blue-600 hover:bg-blue-700 text-white shadow-md shadow-blue-500/20"
            >
              Use dummy credentials
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}

// ── Main Login Page ──────────────────────────────────────────────────────────
export function LoginPage() {
  const { login, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [showPassword, setShowPassword] = useState(false);
  const [selectedRole, setSelectedRole] = useState('CUSTOMER');

  // 2-stage alert states
  const [alertStage, setAlertStage] = useState<'IDLE' | 'STAGE1' | 'STAGE2'>('IDLE');
  const [countdown, setCountdown] = useState(3);

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { remember: true },
  });

  // Handle countdown timer for Alert 1
  useEffect(() => {
    if (alertStage !== 'STAGE1') return;

    if (countdown <= 0) {
      // Transition from Stage 1 to Stage 2 automatically after 3 seconds
      setAlertStage('STAGE2');
      return;
    }

    const timer = setTimeout(() => {
      setCountdown((prev) => prev - 1);
    }, 1000);

    return () => clearTimeout(timer);
  }, [alertStage, countdown]);

  const handleUseCredentials = (email: string, password: string) => {
    setValue('email', email, { shouldValidate: true });
    setValue('password', password, { shouldValidate: true });
    setAlertStage('IDLE');
  };

  const handleInvalid = () => {
    toast.error('Invalid credentials');
    setCountdown(3);
    setAlertStage('STAGE1');
  };

  const submit = async (data: FormData) => {
    try {
      const session = await login(data.email, data.password);

      // Verify that user's actual role matches the chosen role in the dropdown
      if (session.role !== selectedRole) {
        logout();
        toast.error('Invalid credentials');
        setCountdown(3);
        setAlertStage('STAGE1');
        return;
      }

      toast.success(`Welcome back, ${session.firstName}!`);

      // Route to destination or role-appropriate dashboard
      const fromPath = (location.state as { from?: Location } | null)?.from?.pathname;
      if (fromPath && fromPath !== '/login') {
        navigate(fromPath, { replace: true });
      } else {
        if (session.role === 'ADMIN') {
          navigate('/dashboard', { replace: true });
        } else if (session.role === 'CUSTOMER') {
          navigate('/dashboard', { replace: true });
        } else {
          navigate('/work-queue', { replace: true });
        }
      }
    } catch (err: unknown) {
      // Wrong credentials or user does not exist in DB:
      // Show Alert 1 for 3 seconds, then automatically transition to Alert 2 with dummy credentials
      toast.error('Invalid credentials');
      setCountdown(3);
      setAlertStage('STAGE1');
    }
  };

  const dummyInfo = DUMMY_CREDENTIALS[selectedRole];

  return (
    <>
      {alertStage === 'STAGE1' && (
        <InvalidCredentialsModal
          countdown={countdown}
          onClose={() => setAlertStage('IDLE')}
        />
      )}

      {alertStage === 'STAGE2' && (
        <DummySuggestionModal
          role={selectedRole}
          onClose={() => setAlertStage('IDLE')}
          onUseCredentials={handleUseCredentials}
        />
      )}

      <AuthShell title="Welcome back" subtitle="Sign in to continue to ClaimFlow">
        <form className="space-y-4" onSubmit={handleSubmit(submit, handleInvalid)}>
          <div>
            <label htmlFor="role-select" className="block text-sm font-medium text-slate-700 mb-1.5">
              I am logging in as…
            </label>
            <select
              id="role-select"
              value={selectedRole}
              onChange={(e) => setSelectedRole(e.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-800 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 transition-colors font-medium"
            >
              {ROLE_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          <Field label="Work email" error={errors.email?.message}>
            <Input type="email" autoComplete="email" placeholder="you@company.com" {...register('email')} />
          </Field>

          <Field label="Password" error={errors.password?.message}>
            <div className="relative">
              <Input
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                placeholder="Enter your password"
                {...register('password')}
              />
              <button
                type="button"
                className="absolute right-3 top-3 text-slate-400 hover:text-slate-600 transition-colors"
                onClick={() => setShowPassword(!showPassword)}
                aria-label="Toggle password visibility"
              >
                {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
              </button>
            </div>
          </Field>

          <div className="flex items-center justify-between text-sm">
            <label className="flex items-center gap-2 text-slate-600">
              <input type="checkbox" className="accent-brand rounded" {...register('remember')} />
              Remember me
            </label>
            <Link className="font-semibold text-brand hover:underline" to="/forgot-password">
              Forgot password?
            </Link>
          </div>

          <Button type="submit" className="w-full" loading={isSubmitting}>
            Sign in
          </Button>
        </form>

        {/* Optional helper panel to directly test dummy mode without entering wrong credentials */}
        {dummyInfo && (
          <div className="mt-5 rounded-xl border border-slate-200/80 bg-slate-50/80 p-3.5 text-xs text-slate-600 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Info className="h-4 w-4 text-blue-500 shrink-0" />
              <span>Need test credentials for {dummyInfo.label}?</span>
            </div>
            <button
              type="button"
              onClick={() => handleUseCredentials(dummyInfo.email, dummyInfo.password)}
              className="text-blue-600 font-semibold hover:underline shrink-0"
            >
              Fill Dummy
            </button>
          </div>
        )}

        <p className="mt-5 text-center text-sm text-slate-600">
          New to ClaimFlow?{' '}
          <Link className="font-semibold text-brand hover:underline" to="/register">
            Create an account
          </Link>
        </p>
      </AuthShell>
    </>
  );
}

// ── Auth Shell ───────────────────────────────────────────────────────────────
export function AuthShell({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children: React.ReactNode;
}) {
  return (
    <main className="grid min-h-screen lg:grid-cols-[1.1fr_.9fr]">
      <section className="hidden bg-navy p-12 text-white lg:flex lg:flex-col lg:justify-between">
        <div className="flex items-center gap-3">
          <span className="grid h-10 w-10 place-items-center rounded-lg bg-sky-500 font-bold text-white shadow-md">
            CF
          </span>
          <strong className="text-xl tracking-tight">ClaimFlow</strong>
        </div>
        <div>
          <LockKeyhole className="mb-6 h-10 w-10 text-sky-300" />
          <h1 className="max-w-md text-4xl font-bold leading-tight">
            Insurance claims, managed with confidence.
          </h1>
          <p className="mt-5 max-w-md leading-7 text-slate-300">
            A secure workspace that keeps customers and claim teams aligned throughout the entire claim lifecycle.
          </p>
        </div>
        <p className="text-sm text-slate-400">Enterprise Insurance Claim Processing System</p>
      </section>
      <section className="grid place-items-center bg-mist p-5">
        <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-7 shadow-panel sm:p-9">
          <div className="mb-7">
            <div className="mb-5 flex items-center gap-2 lg:hidden">
              <span className="grid h-8 w-8 place-items-center rounded-md bg-brand text-sm font-bold text-white">
                CF
              </span>
              <strong className="text-ink text-lg">ClaimFlow</strong>
            </div>
            <h2 className="text-2xl font-bold text-ink">{title}</h2>
            <p className="mt-1 text-sm text-slate-500">{subtitle}</p>
          </div>
          {children}
        </div>
      </section>
    </main>
  );
}
