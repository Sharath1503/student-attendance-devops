import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development the UI runs on :5173 and forwards /api calls to the Spring Boot backend on :8080,
// so the browser sees a single origin. In production the built files are served by Spring Boot itself.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: process.env.VITE_API_TARGET || 'http://localhost:8080', changeOrigin: true },
    },
  },
  build: { outDir: 'dist', sourcemap: false },
  test: { environment: 'node' },
});
