import js from '@eslint/js';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import tseslint from 'typescript-eslint';

export default tseslint.config(js.configs.recommended, ...tseslint.configs.recommended, {
  ignores: ['dist', 'node_modules'],
  files: ['**/*.{ts,tsx}'],
  plugins: { 'react-hooks': reactHooks, 'react-refresh': reactRefresh },
  rules: { ...reactHooks.configs.recommended.rules, 'no-undef': 'off', 'react-refresh/only-export-components': 'off' }
});
