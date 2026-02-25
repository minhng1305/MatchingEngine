/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        panel: {
          DEFAULT: '#0f1419',
          light: '#161c24',
          lighter: '#1c2530',
          border: '#2a3441',
        },
        accent: {
          DEFAULT: '#3b82f6',
          dim: '#1e3a5f',
        },
        bull: {
          DEFAULT: '#22c55e',
          dim: '#0a3d1f',
          text: '#4ade80',
        },
        bear: {
          DEFAULT: '#ef4444',
          dim: '#3d0a0a',
          text: '#f87171',
        },
      },
      fontFamily: {
        mono: ['"JetBrains Mono"', '"Fira Code"', 'ui-monospace', 'monospace'],
        sans: ['"Inter"', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      fontSize: {
        '2xs': ['0.65rem', { lineHeight: '0.85rem' }],
      },
      animation: {
        'flash-green': 'flashGreen 0.6s ease-out',
        'flash-red': 'flashRed 0.6s ease-out',
        'slide-up': 'slideUp 0.3s ease-out',
        'fade-in': 'fadeIn 0.2s ease-out',
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
      },
      keyframes: {
        flashGreen: {
          '0%': { backgroundColor: 'rgba(34, 197, 94, 0.3)' },
          '100%': { backgroundColor: 'transparent' },
        },
        flashRed: {
          '0%': { backgroundColor: 'rgba(239, 68, 68, 0.3)' },
          '100%': { backgroundColor: 'transparent' },
        },
        slideUp: {
          '0%': { transform: 'translateY(10px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' },
        },
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
      },
    },
  },
  plugins: [],
};
