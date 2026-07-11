import type { Config } from 'tailwindcss';

export default { content: ['./index.html', './src/**/*.{ts,tsx}'], theme: { extend: { colors: { navy: '#102a43', ink: '#172b4d', brandSlate: '#627d98', mist: '#f5f7fa', brand: '#1769aa' }, boxShadow: { panel: '0 1px 2px rgb(16 42 67 / 6%), 0 4px 16px rgb(16 42 67 / 4%)' } } }, plugins: [] } satisfies Config;
