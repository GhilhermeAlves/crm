# Tenant Context (Company/Multi-tenancy)

## Resumo do Módulo
Multi-tenancy com isolamento de dados via Row Level Security (RLS) no PostgreSQL com `FORCE ROW LEVEL SECURITY`, usando uma role não-superuser (`crm_app`) e contexto de tenant propagado pela aplicação (`SET app.current_company_id`). Cada empresa possui configurações independentes e plano de assinatura.

## Objetivo
Garantir isolamento total entre empresas via RLS no banco de dados, com contexto de tenant por requisição e configurações independentes.

## Responsabilidades
- Criação e gerenciamento de empresas (tenants)
- Isolamento via RLS (FORCE RLS) + role não-superuser `crm_app`
- Contexto de tenant por requisição (TenantContext + SET app.current_company_id)
- Configurações por empresa (timezone, idioma, etc.)
- Gestão de planos de assinatura
- Onboarding de novos tenants

## Entidades Relacionadas
- Company, CompanySetting, Subscription

## APIs Relacionadas
- `POST /companies` - Criar empresa (SUPER_ADMIN)
- `GET /companies` - Listar empresas
- `GET /companies/:id` - Detalhes da empresa
- `PUT /companies/:id` - Atualizar empresa
- `PUT /companies/:id/settings` - Configurações
- `GET /companies/:id/subscription` - Assinatura atual

## Banco Relacionado
- `companies` - Dados da empresa (CNPJ, nome, plano)
- `company_settings` - Configurações por empresa
- `subscriptions` - Planos e billing

## Configurações Padrão
- **Timezone**: `America/Sao_Paulo`
- **Idioma**: `pt-BR`
- **Planos**: PLANE → STARTER → PROFESSIONAL → ENTERPRISE

## Componentes Frontend
- CompanySettings page
- TenantSwitcher (multi-empresa)
- Subscription management page

## Componentes Backend
- `company` module (Controllers, Services, Domain, Infrastructure)
- `tenant` package: TenantDataSourcePostProcessor, TenantAwareDataSource (SET/RESET `app.current_company_id` e `app.current_keycloak_sub`), TenantContext (ThreadLocal com companyId + keycloakSub), TenantFilter (ancorado após `BearerTokenAuthenticationFilter` no chain de segurança)
- `subscription` module
- Bootstrap de identidade sob RLS FORCE: policy `identity_bootstrap_policy` em `users` (V022) permite ler a própria linha via `app.current_keycloak_sub` antes de o tenant ser conhecido

## Eventos
- `CompanyCreated` - Nova empresa criada
- `CompanyUpdated` - Dados atualizados
- `SubscriptionChanged` - Plano alterado
- `CompanyDeactivated` - Empresa desativada

## Permissões
- `company:create` - SUPER_ADMIN
- `company:read` - ADMIN, MANAGER
- `company:update` - ADMIN
- `company:settings` - ADMIN
- `company:subscription` - SUPER_ADMIN

## Dependências
- **Auth** (company_id no JWT, resolução de tenant)
- **Users** (usuários vinculados à empresa)
- **Database** (RLS, migrations)

## Fluxo Resumido
1. SUPER_ADMIN cria empresa → registrada em `companies` (tenant)
2. Autenticação: resolvers definem `app.current_keycloak_sub` (do `sub` do JWT) para o bootstrap de identidade em `users` (V022)
3. Usuário faz login → JWT contém company_id → TenantFilter define TenantContext
4. Conexões do pool emitem `SET app.current_company_id` → RLS isola todas as queries por tenant

## Checklist de Implementação
- [x] RLS + FORCE RLS em 18 tabelas tenant-scoped
- [x] Role `crm_app` não-superuser (NOBYPASSRLS) com privilégios mínimos
- [x] Resolução de tenant via JWT claim (companyId)
- [x] Configurações por empresa (timezone, idioma)
- [x] Planos: PLANE/STARTER/PROFESSIONAL/ENTERPRISE
- [x] Limite de usuários por plano
- [x] Isolamento de dados entre tenants (validado cross-tenant + concorrência)

## Checklist de Testes
- [x] Dados de empresa A não aparecem para empresa B
- [x] Cross SELECT/INSERT/UPDATE/DELETE bloqueados por RLS
- [x] Configurações são isoladas por empresa
- [x] Migrations rodam sem quebrar o boot

## Documentação Oficial Relacionada
- `docs/tenant/MULTI-TENANCY.md`
- `docs/tenant/SCHEMA-STRATEGY.md`
- `docs/tenant/PLANS.md`

## Histórico de Revisão
| Data | Autor | Descrição |
|------|-------|-----------|
| 2026-07-15 | System | Criação do contexto |
