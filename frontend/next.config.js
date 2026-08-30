/** @type {import('next').NextConfig} */

const devGatewayTarget = (process.env.DEV_GATEWAY_TARGET || "").trim().replace(/\/+$/, "");

const nextConfig = {
  reactStrictMode: true,
  output: 'standalone',
  images: {
    remotePatterns: [
      {
        protocol: 'https',
        hostname: '**',
      },
    ],
  },
};

if (devGatewayTarget) {
  // Dev local: espelha o roteamento de produção (nginx vhost envia /auth e /api
  // ao auth-service). Sem DEV_GATEWAY_TARGET (build de produção) não há rewrites.
  // beforeFiles: roda ANTES do filesystem — sem isso, /auth/callback (página
  // legada em app/auth/callback) capturaria o retorno do Keycloak e quebraria o
  // login no dev (o nginx de produção nunca entrega /auth ao Next).
  nextConfig.rewrites = async () => ({
    beforeFiles: [
      {
        source: "/auth/:path*",
        destination: `${devGatewayTarget}/auth/:path*`,
      },
      {
        source: "/api/:path*",
        destination: `${devGatewayTarget}/api/:path*`,
      },
    ],
  });
}

module.exports = nextConfig;