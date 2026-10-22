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
  // A ficha do paciente saiu de Contatos (módulo Pacientes, V104): links antigos
  // das abas clínicas continuam funcionando. /contacts/:id/edit segue sendo a
  // edição do contato.
  async redirects() {
    return [
      { source: "/contacts/:id/consultas", destination: "/patients/:id", permanent: false },
      {
        source: "/contacts/:id/:tab(financeiro|arquivos|anotacoes)",
        destination: "/patients/:id/:tab",
        permanent: false,
      },
    ];
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