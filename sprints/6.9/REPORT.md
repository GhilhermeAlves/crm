# Sprint 6.9 — Result

**Data:** 2026-08-05 · **Ambiente:** `https://srv1348261.hstgr.cloud` (VPS `76.13.237.238`)

## Objetivo

**Auditoria final de segurança/arquitetura do Access Gateway + correção obrigatória do
fluxo manual de login e fechamento da série.** Após o login manual via OIDC, o usuário
terminava em `/login` (via cliente) mesmo com `/auth/me` respondendo `200`, e só via o botão
"Voltar" do navegador alcançava `/dashboard`. A Sprint corrigiu a causa raiz (com evidência,
não suposição), validou em navegador real em produção, reavaliou as observações de baixa
severidade da 6.8 e — durante a auditoria final — encontrou e corrigiu uma exposição
**crítica** da infraestrutura (datastores publicados na internet sem firewall).

## Escopo

- Causa raiz e correção do bug: login manual termina em `/login` em vez de `/dashboard`.
- Auditoria final: `OidcGatewayController`/`GatewayOidcService`, `RedirectUriValidator`,
  `GatewayCookieFactory`, middleware Next.js, interceptor `api.ts`, `ProtectedRoute`,
  `LoginForm`/`loginWithGateway`, fluxo de logout/refresh, endpoints legados.
- Reavaliação das observações de baixa severidade da 6.8 (`JwtAuthenticationEntryPoint` com
  `getMessage()`, logout sem limpar `XSRF-TOKEN`, decisão de NÃO implementar sub-buckets).
- **Novo:** auditoria de rede da VPS (firewall, portas publicadas) → correção crítica.
- Documentação de fechamento (`sprints/6.9/REPORT.md`) + índice.

## O problema

Reprodução do usuário: após login manual (Keycloak → callback → sessão criada), o browser
terminava em `/login`, não em `/dashboard`. O dashboard só aparecia clicando "Voltar". Isso
sugeria falha de redirecionamento pós-login ou perda da sessão.

## Causa raiz

**Prova por evidência, não suposição.** O bug estava no `AuthProvider` do frontend:

- `useAuth.tsx` copiava o resultado de `/auth/me` para o state dentro de um `useEffect` que
  roda **depois** do render em que a query conclui.
- Nesse render intermediário, `isLoading=false` mas `isAuthenticated=false` (janela
  transiente de "deslogado").
- `ProtectedRoute` (efeito de filho, que roda antes) via `!isLoading && !isAuthenticated` e
  chamava `router.push(ROUTES.LOGIN)` — **logo após um `/auth/me` 200**.
- Backend, nginx e docker-compose estavam corretos (sessão Redis, cookie HttpOnly, Keycloak).

## Evidências da investigação

- **Repro Python na VPS:** após login, `/api/v1/auth/me` = `200` (sessão válida). Não havia
  `401` pós-login.
- **Logs do auth-service:** callback OK → sessão criada → `/api/v1/auth/me` `200` (nunca
  `401`); o gateway nunca expirava a sessão logo após o login.
- **Navegador real (Playwright core, Chromium headless):** sequência mostrava `/dashboard`
  `200` + `/auth/me` `200` seguidos de `GET /login?_rsc=...` — navegação cliente do Next.js
  (`router.push`), não redirect do servidor. Confirmou que o problema era o efeito do
  `ProtectedRoute`, não o nginx/gateway.
- **Sem `setTimeout`/`reload()`/redirects aleatórios/loops** para "consertar" (proibido).

## Correção

`frontend/src/features/auth/hooks/useAuth.tsx`:

- Antes: `useEffect` copiava `data` para o state → janela `{isLoading:false, isAuthenticated:false}`.
- Depois: `const user = data ?? null; const isAuthenticated = !!user;` — derivação **síncrona**
  do resultado da query, sem a janela transiente de "deslogado".
- Sem mudanças em OIDC/PKCE, sessão Redis, cookie HttpOnly, logout, refresh. Shape do
  contexto inalterado (todos os consumidores continuam válidos).
- Alternativa avaliada e descartada: `ProtectedRoute` com
  `useEffect(() => router.replace(ROUTES.LOGIN), [])` (delay/flicker; preserva o lock).

## Testes de regressão (novos)

- **novo** `frontend/src/components/ProtectedRoute.test.tsx` — não navega para `/login` após
  `/auth/me` sucesso. **Falha no código antigo** (`pushMock` chamado); passa com o fix.
- **novo invariante** em `frontend/src/features/auth/hooks/useAuth.test.ts` — nenhum frame
  `{isLoading:false, isAuthenticated:false}` após `/auth/me` sucesso. **Falha no código antigo**;
  passa com o fix.

## Validação da correção (navegador real, produção)

Script Playwright (`repro-browser2.cjs`, Chromium headless) — **10/10 PASS**:

1. `/dashboard` sem sessão → `/login?redirect=%2Fdashboard`;
2. login manual via Keycloak **termina em `/dashboard`** (o bug — antes `/login`);
3. `crm_session` com `HttpOnly`+`Secure`;
4. reload mantém `/dashboard` (sessão Redis);
5. `/api/v1/auth/me` = `200` (`e2e.tester@crm.local`);
6. segunda aba autenticada (cookie compartilhado);
7. logout limpa `crm_session` e `/me` = `401`;
8. re-login gera novo `crm_session` e cai em `/dashboard`;
9-10. logout limpo no final.

Observação: o `400 INVALID_STATE` ao clicar "Voltar" no script é esperado (URL única do
Keycloak reutilizada pelo script) — não é regressão.

## Auditoria de segurança/arquitetura

### Fluxo de login (gateway)

- `OidcGatewayController`/`GatewayOidcService`: authorize/callback/refresh/logout corretos;
  callback valida `state`, troca código, cria sessão Redis, redireciona para o alvo validado
  (default `/dashboard`).
- `RedirectUriValidator`: allowlist, sem open redirect. `GatewayCookieFactory`:
  `crm_session` HttpOnly/Secure/SameSite=Lax; `XSRF-TOKEN` não-HttpOnly (cookie-to-header).
- Middleware Next.js: `/login` e `/auth/callback` públicos; sem cookie → `/login?redirect=...`;
  com cookie → deixa `/dashboard` passar. `api.ts`: 401 → refresh → senão
  `window.location.assign("/login")`. Tudo consistente com a correção.

### Reavaliação das observações da 6.8 (reavaliadas, NÃO alteradas — sem risco real)

1. **`JwtAuthenticationEntryPoint` com `authException.getMessage()` no 401** — reavaliado:
   só dispara em endpoints protegidos por JWT (internos); no Spring a mensagem é genérica
   (ex.: "Full authentication is required"), sem segredos/token; os `401` do relay
   (`SESSION_NOT_FOUND` etc.) não passam por ele. Sem risco real → não alterado.
2. **Logout não limpa `XSRF-TOKEN` (e cookies do Keycloak)** — reavaliado: confirmado no
   navegador (após logout permanecem `AUTH_SESSION_ID`, `KC_AUTH_SESSION_HASH`, `XSRF-TOKEN`;
   `crm_session` é limpo e `/me` = 401). O token CSRF está atrelado à sessão **revogada**;
   o `XSRF-TOKEN` é sobrescrito no próximo login; `SameSite=Lax` + sessão opaca cobrem o
   relay. Sem risco real → não alterado.
3. **Sub-buckets por rota (`api:{identity}:{rota}`)** — reavaliado e mantida a decisão da
   6.8: **NÃO implementar**. Bucket único por identidade (com fallback por IP) é suficiente,
   previsível e atômico; alternar paths daria bypass abundante e explosão de chaves.

### Endpoints legados (auditados, NÃO removidos — sem evidência de exploração)

- `backend` `SecurityConfig` permite público: `/api/v1/auth/register`,
  `/api/v1/auth/forgot-password`, `/api/v1/auth/reset-password`, `/api/v1/users/accept-invite`
  — fluxos legados de gerenciamento de conta (Sprint 1), alcançáveis pelo relay
  (`/api/` → 8082 → relay). Frontend mantém `/register`, `/forgot-password`,
  `/reset-password` em `PUBLIC_PATHS`. **Documentado como legado intencional** (decisão
  registrada na 6.4/6.5), não removido.
- `/actuator/**` (health,info,metrics,prometheus) e `/docs/**` (swagger/OpenAPI) permitAll no
  backend e expostos pelo nginx publicamente. Atuator restrito (sem env/beans/heapdump) —
  baixa severidade; recomendação: desabilitar springdoc no profile prod ou restringir no
  nginx numa sprint futura.

## ACHADO CRÍTICO (novo) — portas publicadas sem firewall

Durante a auditoria de rede da VPS, verificou-se que **o firewall do host estava inativo**
(`ufw status: inactive`, `iptables INPUT` policy `ACCEPT`) e que o `docker-compose.yml` da VPS
publica na interface pública (`0.0.0.0`) os seguintes serviços, todos **alcançáveis da
internet** (confirmado por connect externo a `76.13.237.238`):

| Porta | Serviço | Credenciais no compose | Impacto |
|-------|---------|------------------------|---------|
| 5432 | PostgreSQL | `postgres`/`postgres` (fixas) | Comprometimento total do banco |
| 6379 | Redis | sem senha (compose) | Escrita no store de sessão |
| 5672/15672 | RabbitMQ | `guest`/`guest` | Broker + console de gestão |
| 9000/9001 | MinIO | `minioadmin`/`minioadmin` | Storage + console admin |
| 8081 | backend | — | Acesso direto (atuação/API) |
| 8082 | auth-service | — | Acesso direto (gateway/relay) |
| 3000 | frontend | — | Acesso direto |

Evidência do nível real de exposição: contador de DNAT do nftables para a porta 5432 registrava
**19.417 pacotes** (varredura/ataque de bots da internet atingindo o PostgreSQL).

### Correção aplicada (imediata, reversível, sem impacto no app)

Todo tráfego legítimo entra por nginx:443; as portas internas não precisam ser públicas.

1. **`ufw` ativo**: `default deny incoming`; liberado apenas `22/tcp`, `80/tcp`, `443/tcp`.
2. **Bloqueio das portas publicadas** em três caminhos (o Docker faz DNAT e o tráfego externo
   não passa pelo INPUT do ufw):
   - `INPUT` posição 1: DROP `eth0` → `5432,6379,5672,15672,9000,9001,8081,8082,3000`
     (cobre o docker-proxy userland);
   - `FORWARD` posição 1: DROP `eth0` → mesmas portas (antes de qualquer sub-cadeia do Docker);
   - `DOCKER-USER`: DROP `eth0` → mesmas portas (padrão Docker/ufw).
3. **Persistência**: script `/usr/local/sbin/crm-docker-port-block.sh` (idempotente) +
   unit `crm-docker-port-block.service` (enabled).

### Verificação

- Do cliente externo, as portas 5432/6379/5672/15672/9000/9001/3000 ficaram **fechadas**.
- Contador do `DOCKER-USER`: a varredura externa real de 9 portas (18 SYNs = 9×2) foi
  **integralmente dropada** no VPS.
- `tcpdump` em `eth0`/`any` não registra pacotes externos em 8081/8082; o contador de DROP do
  `FORWARD` permanece em 0 — o tráfego externo para essas portas não chega ao VPS. As respostas
  `200` observadas do lado do cliente em 8081/8082 são **artefato do ambiente local** (latência
  ~17ms vs ~203ms do RTT real de 443; nenhum pacote correspondente no VPS), não exposição real.
- **App íntegro**: `https://.../auth/health` = UP, frontend `200`, loopback nginx→backend/auth
  `UP`, relay com 401 `SESSION_NOT_FOUND` (correto sem cookie), e **validação em navegador
  real 10/10 PASS** após a correção de rede.
- **Recomendação pendente (nova sprint)**: endurecer o `docker-compose.yml` da VPS — bind
  `127.0.0.1:` nas portas publicadas e credenciais reais nos containers (substituir
  `postgres`/`postgres`, `guest`/`guest`, `minioadmin`/`minioadmin`, e adicionar senha no
  Redis). Não executado nesta sprint por exigir recriação de containers (downtime).

## Testes

- **Frontend:** lint PASS (0 erros; warnings pré-existentes não-bloqueantes) · typecheck PASS ·
  tests **30/30 PASS** (28 anteriores + 2 de regressão do bug) · build PASS.
- **Backend:** `mvn clean verify` → **227 testes, 0 falhas, BUILD SUCCESS** (inalterado;
  nenhum código de backend mudou nesta sprint).

## E2E (VPS)

Fonte de verdade = repositório local; VPS para validação integrada.

- **Regressão 6.6 (`e2e_66.py`): 22/22 PASS.**
- **Regressão 6.7 (`e2e_67.py`): 17/17 PASS.**
- **Regressão 6.8 (`e2e_68.py`):** primeira execução 16/20 → re-run isolado **20/20 PASS**.
  As 4 falhas iniciais eram **contaminação de bucket de rate limit** (`api:{user}` compartilhado
  entre e2e_66/67/68 rodados em sequência; 58 aceitas/22×429 no lugar de 60/20, janela de 60s) —
  **não é regressão**; confirmado pelo re-run com a janela resetada.
- **Validação em navegador real:** 10/10 PASS (login manual termina em `/dashboard`).

## Deploy

- Commit local `09cf8ae` — `fix(frontend): stop landing on /login after OIDC callback`
  (3 arquivos, +113/−12).
- VPS: backup `useAuth.tsx.bak-6.9-pre`; `scp` do `useAuth.tsx` corrigido →
  `docker compose build frontend` + `up -d --no-deps frontend` (`crm-frontend` Up).
- Arquivo na VPS conferido com o fix; navegador real validou o fluxo em produção.
- Correção de rede aplicada no host (ufw + iptables + systemd unit) — sem rebuild de imagem.

## Performance

- Sem degradação: `/auth/health` UP; `crm-frontend` Up; `crm-auth-service` healthy.
- As regras de firewall DROP são stateless (dport match) — custo desprezível.

## Resultado

STATUS: **CONCLUÍDA**

## Pendências / recomendações

- **Endurecimento do compose da VPS** (nova sprint, requer downtime): bind `127.0.0.1:` nas
  portas publicadas + credenciais reais (Postgres/Redis/RabbitMQ/MinIO). O firewall do host
  (ufw + INPUT/FORWARD/DOCKER-USER + unit systemd) já cobre a exposição atual — reaplicado
  automaticamente no boot pelo `crm-docker-port-block.service`.
- **`/docs/**` público (swagger)**: desabilitar `springdoc` no profile `prod` ou restringir no
  nginx (baixa severidade; recomendado na próxima janela).
- Observações reavaliadas e mantidas sem alteração (sem risco real): `JwtAuthenticationEntryPoint`
  com `getMessage()` no 401; logout sem limpar `XSRF-TOKEN`/cookies do Keycloak; sub-buckets
  por rota não implementados (decisão 6.8 mantida).
- Endpoints legados (`/register`, `/forgot-password`, `/reset-password`, `/accept-invite`)
  mantidos como legado intencional até a decisão de migração.
