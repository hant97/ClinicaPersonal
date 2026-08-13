/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#eff6ff',
          100: '#dbeafe',
          200: '#bfdbfe',
          300: '#93c5fd',
          400: '#60a5fa',
          500: '#3b82f6',
          600: '#2563eb',
          700: '#1d4ed8',
          800: '#1e40af',
          900: '#1e3a8a',
          950: '#172554',
        },
        triage: {
          critical: { bg: '#fef2f2', border: '#fecaca', text: '#b91c1c', dot: '#ef4444' },
          urgent: { bg: '#fffbeb', border: '#fde68a', text: '#b45309', dot: '#f59e0b' },
          priority: { bg: '#eff6ff', border: '#bfdbfe', text: '#1d4ed8', dot: '#3b82f6' },
          stable: { bg: '#ecfdf5', border: '#a7f3d0', text: '#047857', dot: '#10b981' },
          specialty: { bg: '#f5f3ff', border: '#ddd6fe', text: '#6d28d9', dot: '#8b5cf6' },
        },
        surface: '#ffffff',
        canvas: '#f8fafc',
        'canvas-subtle': '#f1f5f9',
        ink: '#0f172a',
        'ink-muted': '#334155',
        muted: '#64748b',
        line: '#e2e8f0',
        'line-subtle': '#f1f5f9',
        success: '#10b981',
        danger: '#ef4444',
        warning: '#f59e0b',
        info: '#3b82f6',
        // FlowGrid alias mapping for existing clinic-* classes
        clinic: {
          50: '#eff6ff',
          100: '#dbeafe',
          200: '#bfdbfe',
          500: '#3b82f6',
          600: '#2563eb',
          700: '#1d4ed8',
          900: '#0f172a',
        },
      },
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        display: ['Inter', 'Space Grotesk', 'ui-sans-serif', 'sans-serif'],
        mono: ['JetBrains Mono', 'ui-monospace', 'monospace'],
      },
      boxShadow: {
        card: '0 1px 3px 0 rgba(0, 0, 0, 0.05), 0 1px 2px -1px rgba(0, 0, 0, 0.05)',
        raised: '0 4px 12px 0 rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.05)',
        floating: '0 20px 30px -10px rgba(0, 0, 0, 0.08), 0 1px 3px 0 rgba(0, 0, 0, 0.02)',
      },
    },
  },
  plugins: [],
}
