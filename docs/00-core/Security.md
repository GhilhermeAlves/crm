# Security — Diretrizes de Segurança

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Princípios](#princípios)
- [Autenticação e Autorização](#autenticação-e-autorização)
- [Proteção de Dados](#proteção-de-dados)
- [Segurança de Infraestrutura](#segurança-de-infraestrutura)
- [Segurança de Aplicação](#segurança-de-aplicação)
- [Compliance](#compliance)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Estabelecer as diretrizes e padrões de segurança para todo o sistema CRM SaaS Omnichannel.

## Descrição

Este documento consolida as políticas de segurança que permeiam todos os módulos do sistema, desde autenticação até proteção de dados em repouso e em trânsito. Serve como referência central para decisões de segurança.

## Princípios

1. **Defense in Depth** — Múltiplas camadas de proteção
2. **Least Privilege** — Acesso mínimo necessário
3. **Zero Trust** — Nunca confiar, sempre verificar
4. **Security by Design** — Segurança desde a concepção
5. **Fail Secure** — Em caso de falha, negar acesso

## Autenticação e Autorização

### JWT

| Componente | Configuração |
|---|---|
| Access Token | 15 minutos, assinatura RS256 |
| Refresh Token | 7 dias, rotação automática |
| Armazenamento | HttpOnly Cookie (refresh) + Memory (access) |
| Revogação | Family-based token blacklisting |

### RBAC

- 5 roles: SUPER_ADMIN, ADMIN, MANAGER, AGENT, VIEWER
- Permissões por recurso:ação (ex: `contacts:read`)
- Verificação via `@PreAuthorize` (Spring Security)
- Detalhes em `01-backend/Permissions.md`

### OAuth2 / Social Login

- Google OAuth2 para login social
- Tokens validados contra endpoints oficiais
- Email verificado obrigatório

## Proteção de Dados

### Em Trânsito

| Protocolo | Uso |
|---|---|
| TLS 1.3 | Todas as comunicações HTTP |
| HTTPS | Forçado via HSTS |
| WSS | WebSocket seguro |

### Em Repouso

| Dado | Método | Local |
|---|---|---|
| Senhas | Bcrypt (12 rounds) | PostgreSQL |
| Dados sensíveis | AES-256 | Column-level encryption |
| Tokens | Hash (SHA-256) | PostgreSQL |
| Chaves API | Vault / Environment Variables | Infraestrutura |

### Classificação de Dados

| Nível | Exemplos | Proteção |
|---|---|---|
| Público | Nome da empresa, logo | Nenhuma extra |
| Interno | Configurações, templates | Autenticação |
| Sensível | Email, telefone, documento | Criptografia + Audit |
| Crítico | Senhas, tokens, chaves | Criptografia forte + Vault |

## Segurança de Infraestrutura

### Headers HTTP

```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000; includeSubDomains
Content-Security-Policy: default-src 'self'
```

### CORS

| Configuração | Valor |
|---|---|
| AllowedOrigins | Variável de ambiente `CORS_ALLOWED_ORIGINS` |
| AllowedMethods | GET, POST, PUT, DELETE, PATCH, OPTIONS |
| AllowedHeaders | Authorization, Content-Type, X-Tenant-ID |
| AllowCredentials | true |
| MaxAge | 3600 |

### Rate Limiting

| Endpoint | Limite |
|---|---|
| Login | 10 req/min por IP |
| Cadastro | 5 req/min por IP |
| API geral | 100 req/min por tenant |
| Webhooks | 200 req/min por IP |

### Network Security

- Firewalls com regras de menor privilégio
- VPN para acesso administrativo
- Network policies no Kubernetes
- WAF para proteção contra OWASP Top 10

## Segurança de Aplicação

### Validação de Input

- Backend: Bean Validation (JSR 380)
- Frontend: Zod schemas
- Sanitização de HTML (XSS prevention)
- Prepared statements (SQL injection prevention)

### Upload de Arquivos

- Tipo permitido: whitelist explícita
- Tamanho máximo: 10MB (configurável)
- Scan de vírus: ClamAV
- Armazenamento: fora do diretório web

### Logging de Segurança

| Evento | Nível | Retenção |
|---|---|---|
| Login bem-sucedido | INFO | 90 dias |
| Login falhou | WARN | 90 dias |
| Acesso negado | WARN | 180 dias |
| Tentativa de injeção | ERROR | 1 ano |
| Mudança de permissão | INFO | 1 ano |
| Exportação de dados | INFO | 1 ano |

## Compliance

### LGPD

- Consentimento explícito para dados opcionais
- Direito de acesso, correção, exclusão e portabilidade
- DPO (Data Protection Officer) designado
- Registro de atividades de tratamento
- Detalhes em `LGPD.md`

### OWASP Top 10 (2021)

| Risco | Mitigação |
|---|---|
| A01 - Broken Access Control | RBAC + Tenant isolation |
| A02 - Cryptographic Failures | Bcrypt, AES-256, TLS 1.3 |
| A03 - Injection | Prepared statements + Bean Validation |
| A04 - Insecure Design | Threat modeling na conception |
| A05 - Security Misconfiguration | Security headers + CORS |
| A06 - Vulnerable Components | Dependabot + Snyk |
| A07 - Auth Failures | JWT rotation + Rate limiting |
| A08 - Data Integrity | Audit log + Signatures |
| A09 - Logging Failures | Centralized logging (Loki) |
| A10 - SSRF | Allowlist de URLs externas |

## Responsabilidades

- Definir padrões de segurança para todos os módulos
- Revisar decisões de segurança (ADRs)
- Auditoria periódica de código e configuração
- Responder a incidentes de segurança

## Dependências

- [00-core/Decisions.md](./Decisions.md) — ADRs de segurança
- [01-backend/Auth.md](../01-backend/Auth.md) — Implementação de autenticação
- [01-backend/Permissions.md](../01-backend/Permissions.md) — RBAC
- [LGPD.md](../LGPD.md) — Compliance LGPD
- [SECURITY_MAP.md](../SECURITY_MAP.md) — Mapa de segurança

## Regras

- Nenhuma chave ou segredo no código fonte
- Dependabot/Snyk habilitado para vulnerabilidades
- Code review obrigatório para mudanças de segurança
- Pen testing semestral
- Rotacionar chaves de criptografia anualmente
- Log de todos os acessos administrativos

## Futuras Melhorias

- WAF (Web Application Firewall) dedicado
- Secret management com HashiCorp Vault
- SIEM para correlação de eventos
- Bug bounty program
- SOC 2 Type II

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
