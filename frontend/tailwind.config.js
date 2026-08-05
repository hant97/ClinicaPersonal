/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  theme: {
    extend: {
      colors: {
        clinic: { 50: '#f4f8f7', 100: '#e7f0ed', 200: '#c9ddd6', 500: '#3f796c', 600: '#326357', 700: '#285047', 900: '#18342f' },
        surface: '#ffffff', canvas: '#f6f8f7', ink: '#1d2926', muted: '#64736e', line: '#dce5e1', success: '#3f796c', danger: '#b85c5c',
      },
      fontFamily: { sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'], display: ['Space Grotesk', 'Inter', 'ui-sans-serif', 'sans-serif'] },
      boxShadow: { card: '0 4px 18px rgba(24, 52, 47, 0.06)', raised: '0 10px 30px rgba(24, 52, 47, 0.10)' },
    },
  },
  plugins: [],
}
