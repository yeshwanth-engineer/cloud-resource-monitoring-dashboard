import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// The dev server proxies /api to the Spring Boot backend, so the app always talks to a
// same-origin URL and never needs CORS. Point VITE_DEV_API_TARGET at another host to test
// against a remote API.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const apiTarget = env.VITE_DEV_API_TARGET || 'http://localhost:8080';
  // Hosts the dev server should answer for. Vite blocks unknown Host headers by default to
  // prevent DNS rebinding; add a tunnel or sandbox domain here (leading dot = all subdomains).
  const allowedHosts = (env.VITE_ALLOWED_HOSTS || '.e2b.app').split(',').map((host) => host.trim()).filter(Boolean);

  return {
    plugins: [react()],
    server: {
      host: true,
      port: Number(env.VITE_PORT || 5173),
      allowedHosts,
      hmr: { overlay: false },
      proxy: {
        '/api': { target: apiTarget, changeOrigin: true },
        '/actuator': { target: apiTarget, changeOrigin: true },
      },
    },
    preview: {
      host: true,
      port: Number(env.VITE_PREVIEW_PORT || 4173),
      allowedHosts,
    },
    build: {
      outDir: 'dist',
      sourcemap: true,
      // recharts is the bulk of the bundle and is loaded lazily by the CPU panel.
      chunkSizeWarningLimit: 600,
    },
  };
});
