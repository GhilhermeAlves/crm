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
  // O proxy roda no processo Node (child_process ou rewrites do Next), nunca no
  // Edge: o fetch do sandbox Edge falha em TLS cross-origin.
  nextConfig.rewrites = async () => [
    {
      source: "/auth/:path*",
      destination: `${devGatewayTarget}/auth/:path*`,
    },
    {
      source: "/api/:path*",
      destination: `${devGatewayTarget}/api/:path*`,
    },
  ];
}

module.exports = nextConfig;