# Sprint 5 — Tenant

## Identificação
- **Sprint:** 5
- **Nome:** Tenant
- **Data:** 2026-08-01
- **Status:** ✅ Concluída
- **Responsável:** AI Agent
- **Fase:** Segurança

## Objetivo
Isolamento real de dados multi-tenant via Row Level Security no PostgreSQL, com role não-superuser `crm_app`, contexto de tenant propagado pela aplicação e validação cross-tenant + concorrência na VPS.

## Escopo
- Migrations V011–V021 (infraestrutura multi-tenancy + RLS em 18 tabelas tenant-scoped)
- Role `crm_app` (LOGIN, NOSUPERUSER, NOBYPASSRLS) com privilégios mínimos
- Correções V018 + RoleDataSeeder + AuthService.assignDefaultRole
- Validação: matriz cross-tenant e concorrência A/B na VPS

## Sub-arquivos

| Arquivo | Finalidade | Status |
|---------|-----------|--------|
| SPRINT.md | Este arquivo | ✅ |
| REPORT.md | Relatório final com matriz RLS | ✅ |

## Dependências
- Sprint 4.1 — Infraestrutura Auth

---

*Data: 2026-08-01*
