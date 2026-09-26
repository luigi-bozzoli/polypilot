/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        bg0: '#0a0b0d',
        bg1: '#0f1114',
        bg2: '#15181d',
        bg3: '#1c2028',
        bg4: '#232830',
        border: 'rgba(255,255,255,0.07)',
        'border-mid': 'rgba(255,255,255,0.12)',
        'text-primary': '#e8eaed',
        'text-secondary': '#7a8394',
        'text-muted': '#454d5c',
        accent: '#00d4a8',
        'accent-dim': 'rgba(0,212,168,0.12)',
        'accent-border': 'rgba(0,212,168,0.3)',
        red: '#f05252',
        'red-dim': 'rgba(240,82,82,0.12)',
        'red-border': 'rgba(240,82,82,0.3)',
        yellow: '#f0a832',
        'yellow-dim': 'rgba(240,168,50,0.1)',
        blue: '#4d9ef5',
        'blue-dim': 'rgba(77,158,245,0.1)',
        purple: '#9b7af5',
        'purple-dim': 'rgba(155,122,245,0.1)',
        'purple-border': 'rgba(155,122,245,0.3)',
      },
      fontFamily: {
        mono: ['"IBM Plex Mono"', 'monospace'],
        sans: ['"DM Sans"', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
