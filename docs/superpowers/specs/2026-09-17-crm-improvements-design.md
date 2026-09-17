# Design — Melhorias e Implementações do CRM SaaS

- **Data:** 2026-09-17
- **Escopo:** backend (Java 25/Spring Boot), auth-service, frontend (Next.js 14), VPS de produção
- **Status:** Aprovado para escrita do plano de implementação

## 1. Contexto

CRM SaaS Omnichannel (WhatsApp-first, multi-tenant com RLS): 52 sprints, ~1100 testes, CI/CD via GitHub Actions, docs em `docs/`. Produção na VPS `crm-vps` (76.13.237.238) com 8 containers Docker em 2 stacks compose, atrás de Nginx TLS Let's Encrypt.

Análise combinada (código local + inspeção SSH em produção) identificou 4 eixos de melhoria. Cada fase é independente e executável; este spec fixa escopo, critérios de aceite e ordem.

## 2. Decisões de escopo (acordadas)

**Incluído:**
- Segurança/hardening/higiene (repo + backend + auth + VPS)
- Qualidade UX do frontend (fixes, estados, rotas, refactor de componentes, hooks)
- Refactor de autorização duplicada no backend + hardening de auth
- Testes E2E (Playwright)
- Um plano em fases, cada fase executável e revisada

**Excluído (explicitamente):**
- Reorganização de `docs/`, `docs-ai/`, `contexts/`, `docker/` vs raiz, `infra/` vs `infrastructure/`
- Refactor da dupla fonte de identidade (PostgreSQL + Keycloak)
- i18n e realtime (socket.io) — registrados como **roadmap futuro**
- Consolidar stacks de deploy da VPS (apenas limpeza de árvores órfãs)

## 3. Fase 1 — Segurança, hardening e higiene

### 3.1 Backend (código)

| # | Item | Detalhe | Critério de aceite |
|---|------|---------|---------------------|
| 1.1 | Webhook WhatsApp não assinado | `WHATSAPP_WEBHOOK_ALLOW_UNSIGNED` default `false`; rejeitar payload sem `X-Hub-Signature-256` válida quando desabilitado. Forçar `false` no `.env` prod | Payload sem assinatura → 401; teste novo |
| 1.2 | Rate limit fluxo OTP | Rate limit em `/auth/phone/send-otp` e `/verify-otp`, reusando padrão `InvitationRateLimiter`/Redis. Por IP e/ou telefone | Testes unit/integração do limitador |
| 1.3 | Validação `@Valid` em auth | Adicionar Bean Validation em `forgot-password`, `reset-password`, `change-password`, `users/invite` (padrão já usado em `register`) | Controller retorna `400` em payload inválido |
| 1.4 | Vazamento de exceções | `GlobalExceptionHandler` não devolve `Exception.getMessage()`/mensagens internas; mensagem genérica + log server-side | Teste do handler: resposta nunca contém stack/mensagens internas |
| 1.5 | Default CORS | `app.cors.allowed-origins` de `*` para lista explícita no `application.yml` | Sem origem `*` no default |
| 1.6 | `KeycloakJwtAuthenticationConverter` | Logar WARN com detalhes em vez de `catch (ignored) {}` na extração de roles | Log aparece em token malformado; testes verdes |

### 3.2 Repositório (higiene)

| # | Item | Detalhe |
|---|------|---------|
| 1.7 | Remover lixo commitado | `backend/fix_test.py`, `fix_webhook.py`, `tmp_apply.sh`, `hs_err_pid*.log`, `replay_pid*.log` |
| 1.8 | Limpar `.github` | Arquivos UUID JSON e scripts `recordToolUse.*` |
| 1.9 | Reforçar `.gitignore` | Padrões `hs_err_pid*.log`, `replay_pid*.log`, tooling IA local |

### 3.3 VPS (produção)

| # | Item | Detalhe | Critério de aceite |
|---|------|---------|---------------------|
| 1.10 | Webhook assinatura | `WHATSAPP_WEBHOOK_ALLOW_UNSIGNED=false` no `.env` prod | Assinado funciona; não assinado rejeitado |
| 1.11 | Permissão `.env` | `chmod 600` em `/opt/crm/.env` e `/opt/crm/docker/.env` | `ls -l` mostra 600 |
| 1.12 | Keycloak bootstrap | Remover `KC_BOOTSTRAP_ADMIN_USERNAME/PASSWORD` do compose prod | Compose sem bootstrap |
| 1.13 | Backup automático | Novos arquivos `scripts/backup-db.sh` e `scripts/restore-db.sh` no repositório; instalados na VPS em `/opt/crm/scripts/`; cron diário `pg_dump` custom de `crm` + `keycloak_db`, retenção 7 dias, restore testado 1x em container temporário | Restore validado |
| 1.14 | Swap | 2G swapfile, persistente em `/etc/fstab` | `swapon` ativo, `free -h` mostra swap |
| 1.15 | Healthcheck backend | Adicionar healthcheck no serviço `backend` do compose app (padrão auth-service) | `docker ps` healthy |
| 1.16 | Sessões ociosas | `AUTH_GATEWAY_SESSION_IDLE_TIMEOUT` `0s` → `4h` | Config aplicada em prod |
| 1.17 | Flyway out-of-order | Manter `true`; documentar correção como follow-up (dívida de schema) | Nenhuma mudança agora |
| 1.18 | Árvores órfãs VPS | Criar tar único datado `/root/crm-backup-forest-YYYYMMDD.tar.gz` com `local-main-tree`, `local-main-tree.old`, `crm-pre8.4-untracked-moved`, `crm-backup-pre8.4`, `local-main.tar`, `index-sync.tar.gz`; apagar `/opt/crm-backup-` + `crm-deploy-backup`; remover lixo de sessão `NUL`/`\=` | Apenas `/opt/crm` + `backups` padrão |

## 4. Fase 2 — Qualidade UX do frontend

### 4.1 Fixes pontuais

| # | Item | Detalhe | Critério de aceite |
|---|------|---------|---------------------|
| 2.1 | Bug busca `/leads` | Busca/agrupamento filtram só a página corrente. Mover busca/filtro para query server-side (API suporta) | Busca em qualquer página usa dataset inteiro; testes service/hook |
| 2.2 | Busca fake do Header | Remover input "Search (UI only)" até existir busca global real | Sem input falso no Header |
| 2.3 | `deals.mock.ts` | Apagar `src/features/pipeline/data/deals.mock.ts` e dependências | Sem referências; build ok |

### 4.2 Estados de carregamento

| # | Item | Detalhe | Critério de aceite |
|---|------|---------|---------------------|
| 2.4 | `channels`, `storage`, `pipeline`, `follow-up-sequences` | Aplicar padrão pages-modelo (leads/tenants/contacts): Skeleton + ErrorCard(retry) + EmptyState | Estados verificáveis por rota |

### 4.3 Rotas duplicadas

| # | Item | Detalhe | Critério de aceite |
|---|------|---------|---------------------|
| 2.5 | `(authenticated)` → `(dashboard)/settings` | Mover `/roles*` e `/permissions` para `(dashboard)/settings/*`, remover group, atualizar `ROUTES.*` | Sem rota 404; navegação canônica |

### 4.4 Refactor de componentes gigantes

| # | Item | Detalhe | Critério de aceite |
|---|------|---------|---------------------|
| 2.6 | `design-system/page.tsx` (1199) | Fatiar em seções (typography, tokens, buttons, forms, feedback, data display, overlays) | Showcase intacto, sem regressão visual |
| 2.7 | `TenantForm.tsx` (479) | Sub-formulários (empresa, settings, quota) + seção de campos | Mesmo comportamento; testes verdes |
| 2.8 | `WorkflowForm.tsx` (419) | Extrair builders de condições/ações, validador, lista de execução | Comportamento idêntico; testes verdes |
| 2.9 | `Sidebar.tsx` (426) | `SidebarGroup` + `SidebarItem` + dados em `navigation.ts` | Mesma renderização; testes verdes |
| 2.10 | `data-table.tsx` (402) | Extrair paginação, skeleton, actions em subcomponentes | API pública inalterada; testes verdes |

### 4.5 Hooks de mutation

| # | Item | Detalhe | Critério de aceite |
|---|------|---------|---------------------|
| 2.11 | Mutation helper único | Encapsular `onSuccess → invalidate + toast` e `onError → toast(error)`. Aplicar em leads/users/tenants/tasks/workflows/contacts/campaigns. Unificar `aiErrorMessage` | Duplicação removida; testes de hooks verdes |

## 5. Fase 3 — Arquitetura / backend

| # | Item | Detalhe | Critério de aceite |
|---|------|---------|---------------------|
| 3.1 | Autorização triplicada | `requireCompanyAccess` no controller replica service (`TenantContext`) + RLS. Centralizar auth de tenant no service + `@PreAuthorize` por permissão; RLS última barreira | Sem duplicação nova; `*IsolationIT` verdes |
| 3.2 | Consistência | Curar inconsistências achadas durante 3.1 (validação, mensagens, logs), sem tocar identidade/docs | Checkstyle + testes verdes |

## 6. Fase 4 — Testes E2E (Playwright)

| # | Item | Detalhe | Critério de aceite |
|---|------|---------|---------------------|
| 4.1 | Setup Playwright | `@playwright/test`, config com webServer, scripts npm, job no CI | `npx playwright test` verde no CI |
| 4.2 | Login/logout | Fluxo real via gateway, sessão, logout | Specs verdes |
| 4.3 | Negócio | leads CRUD/filtro, pipeline/kanban, inbox omnichannel, members/roles | Specs verdes |
| 4.4 | Estabilidade | Dados por fixture/seed, specs isolados, sem dados de produção | CI estável |

**Fora de escopo (roadmap futuro):** realtime socket.io (notificações/inbox), i18n.

## 7. Order de execução

1. Fase 1 — Segurança/hardening/higiene (maior risco, menor esforço)
2. Fase 2 — UX frontend (precisa de base estável)
3. Fase 3 — Arquitetura backend
4. Fase 4 — E2E Playwright (protege tudo)

Cada fase termina com: suítes de teste verdes (back ~700, auth ~280, front ~128), lint + typecheck, e validação da VPS (health + fluxos públicos).

## 8. Notas de produção (gravadas durante inspeção)

- 2 stacks: `crm-infrastructure` (`/opt/crm/docker-compose.yml`, 5 datastores) + app (`/opt/crm/docker/docker-compose.yml`, 3 serviços)
- Nginx TLS hardening correto: loopback-only, `server_tokens off`, actuator restrito, docs desabilitados
- OIDC open-redirect funcionando (observado rejeição de candidato `:80`)
- Erro de runtime no frontend prod: `Cannot read properties of null (reading 'digest')` — investigar como follow-up do plano