import { defineConfig } from "vitest/config";

export default defineConfig({
  cacheDir: "/tmp/vite",
  test: {
    reporters: [["json", { stdout: true }]],
    fsModuleCachePath: "/tmp/vitest-cache",
  },
});
