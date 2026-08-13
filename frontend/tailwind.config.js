/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  theme: {
    extend: {
      colors: {
        clinic: { 50: '#f6f3ee', 100: '#e7f0e8', 200: '#d6e2da', 500: '#6e9182', 600: '#315c54', 700: '#274c46', 900: '#233f3b' },
        surface: '#fffdf9', canvas: '#f4eee6', ink: '#233f3b', muted: '#66766f', line: '#ded8ce', success: '#5d876f', danger: '#a45249',
      },
      fontFamily: { sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'], display: ['Space Grotesk', 'Inter', 'ui-sans-serif', 'sans-serif'] },
      boxShadow: { card: '0 4px 18px rgba(49, 92, 84, 0.06)', raised: '0 10px 30px rgba(49, 92, 84, 0.10)' },
    },
  },
  plugins: [],
}
