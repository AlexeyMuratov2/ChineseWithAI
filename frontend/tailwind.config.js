/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        app: {
          bg: 'var(--color-bg)',
          fg: 'var(--color-fg)',
          muted: 'var(--color-muted)',
          border: 'var(--color-border)',
          accent: 'var(--color-accent)',
        },
      },
    },
  },
  plugins: [],
}

