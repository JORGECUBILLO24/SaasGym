import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import process from 'node:process'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  server: {
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,
    // Mismo dominio que en producción (Vercel reenvía /api a Render): la cookie de sesión
    // HttpOnly funciona igual en desarrollo. VITE_API_PROXY permite apuntar a otra API.
    proxy: {
      '/api': { target: process.env.VITE_API_PROXY || 'http://localhost:8080', changeOrigin: true },
      '/gifs': { target: process.env.VITE_API_PROXY || 'http://localhost:8080', changeOrigin: true },
    },
  },
  preview: {
    host: '0.0.0.0',
    port: 4173,
    strictPort: true,
  },
  build: {
    rolldownOptions: {
      external: ['canvg'],
    },
  },
  // Vitest: pruebas de unidad y de componentes (jsdom). Ver src/test/setup.ts.
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
    css: false,
  },
})
