# SPRINT 19 — ANALYTICS / DASHBOARD AVANÇADO — REPORT

> Status: ✅ CONCLUÍDA

## OBJETIVO

Transformar os dados operacionais existentes (CRM, campanhas, omnichannel,
automações) em KPIs gerenciais com filtro de período e comparação temporal —
predominantemente read-only.

## DECISÃO ARQUITETURAL

- **Zero cópia de dados**: todas as métricas são AGREGAÇÕES SQL
  (`NamedParameterJdbcTemplate`) sobre as tabelas operacionais existentes.
  **Nenhuma migration estrutural foi necessária.**
- **Endpoint agregado único** (`GET /analytics/summary`) para reduzir round
  trips do dashboard; retorna período atual + anterior + série diária.
- Períodos: datas interpretadas no timezone informado (default
  America/Sao_Paulo), início inclusivo / fim exclusivo; comparação contra o
  período imediatamente anterior de mesma duração.

## IMPLEMENTAÇÃO

### Backend

- `application/analytics`: `AnalyticsPeriod` (resolução de período + previous),
  DTOs (`AnalyticsSummaryResponse.Metrics/DailyPoint`), port
  `AnalyticsUseCase`, service `AnalyticsService` (~19 métricas por consulta
  agregada: contatos/leads/oportunidades criados e ganhos, valor ganho,
  pipeline aberto, atividades, tarefas criadas/concluídas/atrasadas,
  campanhas executadas/mensagens enviadas-falhadas, mensagens WhatsApp
  in/out, runs de automações total/sucesso/falha);
- `presentation/rest/analytics/AnalyticsController`:
  `GET /api/v1/companies/{companyId}/analytics/summary?from&to&timezone`,
  `@PreAuthorize('analytics:read')` + requireCompanyAccess + TenantContext;
- Migration **V063**: seed da permissão `analytics:read` + grants para
  ADMIN/MANAGER/AGENT/VIEWER de todas as companies (padrão V053/V061).

### Frontend

- `/reports` (rota já registrada no projeto): filtro global de período
  (7d/30d/mês atual/mês anterior), grid de 15 KPI cards com variação vs
  período anterior (com proteção de divisão por zero), gráfico de evolução
  diária (recharts — biblioteca já presente no projeto) e painel de resumo
  de automações/campanhas/WhatsApp; loading skeletons, empty/error states,
  gate por permissão `analytics:read`;
- `src/features/analytics/{types,services,hooks}` no padrão dos demais módulos;
  utilitário `delta()` testável.

## TESTES

- Backend: **525 unit verdes** (+5 novos em `AnalyticsPeriodTest`: limites
  inclusivo/exclusivo, previous period, timezone, range inválido, default 30d);
  checkstyle OK;
- **AnalyticsIsolationIT** (Testcontainers, padrão Sprints 16–18):
  Tenant A vê somente seus dados, Tenant B idem; série diária isolada;
  período anterior vazio → zeros sem erro; período vazio futuro → zeros;
- Frontend: lint/typecheck/format/build OK; **208 Vitest verdes**.

## PERFORMANCE

Todas as métricas são agregações executadas no banco (COUNT/SUM/GROUP BY com
CTE para a série diária); nenhum carregamento de dataset completo na aplicação;
sem N+1 (uma query por métrica, ~19 queries leves por request).

## GIT

Commit feat + docs; hash final abaixo; working tree limpa; `LOCAL == origin/main`.

## CI/CD E VPS

CI GREEN · GHCR GREEN · CD GREEN · deploy crm-vps validado:
containers UP/healthy, `/actuator/health` UP, frontend HTTP 200,
Flyway V063 aplicada, analytics API protegida (**401 sem sessão**),
scheduler sem erros.

## LIMITAÇÕES

- E2E autenticado na VPS permanece débito herdado (smoke 401 + isolation IT
  cobrem a proteção);
- Métricas "entregues/lidas" de campanha dependem de webhooks do provider
  (Sprint 17); hoje SENT+DELIVERED+READ compõem "enviadas";
- Origem de leads exibida apenas quando existir dado (campo source);
- Sem cache Redis (não necessário no volume atual — FUTURE se necessário);
- Export CSV permanece FUTURE/débito.

## DÉBITOS PERMANENTES REGISTRADOS

E2E autenticado na VPS; RabbitMQ real; e-mail real; backoff exponencial;
export CSV; provider WhatsApp conforme configuração.
