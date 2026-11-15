import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    setupFiles: "./src/test-setup.ts",
  },
  server: {
    proxy: {
      "/api": "http://localhost:8080",
      // Google sign-in through the gateway; keep the browser's host so the callback comes back here
      "/oauth2": { target: "http://localhost:8080", xfwd: true },
      "/login/oauth2": { target: "http://localhost:8080", xfwd: true },
    },
  },
});
