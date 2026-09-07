import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /**
   * Emits `.next/standalone` — the server plus only the node_modules it
   * actually traced — so the runtime image carries neither the build
   * toolchain nor the full dependency tree.
   */
  output: "standalone",
};

export default nextConfig;
