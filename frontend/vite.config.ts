import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The dev proxy is the whole point: the browser only ever talks to localhost:5173,
// so the backend's session cookie (SameSite=Lax, httpOnly) and CSRF cookie behave exactly
// as they will in production behind a single reverse-proxied origin. Never add a
// cross-origin base URL here instead of this proxy - it would force SameSite=None,
// which is a materially weaker cookie setting.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VYOU_BACKEND_URL ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
