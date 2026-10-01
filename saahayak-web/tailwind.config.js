/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        background: '#FBFBFE',
        surface: '#FFFFFF',
        primary: '#2E5EFF',
        coral: '#FF6B4A',
        success: '#1FAE6B',
        warning: '#F5A623',
        text: '#141B2D',
        muted: '#5B6578',
      },
      fontFamily: {
        heading: ['Sora', 'sans-serif'],
        body: ['Inter', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
      boxShadow: {
        soft: '0 1px 0 rgba(15, 23, 42, 0.04)',
      },
    },
  },
  plugins: [],
};

