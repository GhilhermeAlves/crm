# Sprint 8.3 - Onboarding

**Data:** 2026-08-10 - **Ambiente:** backend + auth-service + frontend (build/typecheck locais) - **Status:** ✅ Concluída

## Identificação

- **Sprint:** 8.3
- **Nome:** Onboarding - fluxo self-service de criação de empresa para usuário sem empresa
- **Responsável:** AI Agent
- **Fase:** SaaS - Empresas / Multi-tenant
- **Dependência:** 8.2 (Membership) - plano da Sprint 8 em `sprints/8/SPRINT_PLAN.md`

## Objetivo

Entregar a **primeira experiência self-service** (D4 do plano): o usuário autenticado **sem empresa**
deve conseguir **criar a própria empresa** (passando a ser `OWNER` e membro ativo), com seed de
papéis e acesso ao CRM — e o caminho alternativo via convite fica encaminhado (aceite completo na
8.5).

## Arquitetura / Decisões

| Decisão | Detalhe |
|---|---|
| Modelo | `users.company_id` deixa de ser obrigatório (**V032 nullable**) — usuário "company-less". `AUTH_DEFAULT_COMPANY_ID` vira **fallback apenas quando habilitado**; o provisionamento (auth-service) passa a criar usuários sem empresa quando não há padrão/não há convite. |
| RLS | **V032**: `identity_onboarding_insert_policy` — policy de INSERT que permite ao `crm_app` criar a linha de usuário company-less durante o bootstrap/onboarding (sem perder o `FORCE ROW LEVEL SECURITY` do isolamento). |
| Provisionamento | `AuthService` (auth-service) deixa de obrigar role/membership no provisionamento quando o usuário nasce company-less; `resolveDefaultCompanyId` retorna `null` nesse caso. |
| Onboarding | Novo agregado `onboarding` no **backend**: `OnboardingUseCase` + `OnboardingService` + `OnboardingController` — `POST /api/v1/onboarding/companies` cria `companies`, **seed de papéis**, membership `OWNER/ACTIVE`, eleva o `company_id` do usuário (ativa) e concede acesso ao CRM. |
| Reuso de seed | `RoleSeedService` extraído de `RoleDataSeeder` e reutilizado pelo onboarding (paridade de roles: SUPER_ADMIN/ADMIN/MANAGER/AGENT/VIEWER). |
| `CurrentUser` | `companyId` passa a ser **nullable** (backend + auth-service). Ramos company-less em `CurrentUserResolutionService`/`LocalCurrentUserResolver` retornam roles/permissões **vazios** e **pulam o gate do CRM** (para não bloquear o login de quem ainda não tem empresa). |
| Plano | `CompanyService` expõe limites padrão (públicos); o onboarding aplica o `CompanyPlan` vindo da requisição (default STARTER). |
| Frontend | Rota `/onboarding` com layout próprio + **gate** (`ProtectedRoute` redireciona usuário company-less para `/onboarding`); feature `onboarding` (types/service/schema/hook/form `OnboardingCompanyForm`) e `User.companyId` nullable. |

## Migrações / Banco

- **V032** - `users.company_id` passa a **nullable** (relaxamento de `NOT NULL` da D3 do Sprint 8)
  + `identity_onboarding_insert_policy` (RLS INSERT para bootstrap company-less).

## Problemas encontrados e correções

- O gate de empresa (8.2) precisou ser **relaxado** para o company-less: a resolução do
  `CurrentUser` sem empresa retorna roles/permissões vazios em vez de **401**, permitindo a tela de
  onboarding. Comportamento validado por `CurrentUserResolutionServiceTest` (novo).

## Testes

- Backend: `mvn test` → **153 testes, 0 falhas** (inclui `OnboardingServiceTest` +182 e
  `OnboardingControllerTest` +184).
- Auth-service: `mvn test` → **282 testes, 0 falhas** (inclui `AuthServiceProvisioningTest`).
- Frontend: **56 testes, typecheck OK**.

## Observações / Follow-ups

1. **Convite como alternativa** (fluxo "Já tenho convite" no onboarding) aponta para o aceite de
   convite — implementação completa do aceite/`invitations` na **8.5**.
2. **`AUTH_DEFAULT_COMPANY_ID`**: permanece como fallback opcional quando habilitado; não é mais
   requisito para o fluxo de onboarding.
3. E2E em produção robusto (VPS) do fluxo completo: usuário sem empresa → criar empresa → virar
   OWNER + acessar CRM — **não executado nesta entrega** (ficou pendente de sessão VPS; o
   fechamento de 8.3 foi registrado junto ao início de 8.4). Regressão dirigida à **8.6** ou
   sprint de hardening para produção.