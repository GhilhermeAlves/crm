# Tenant Context (Company/Multi-tenancy)

## Resumo do Módulo
Multi-tenancy via schema-per-tenant com isolamento completo de dados. Cada empresa possui configurações independentes e plano de assinatura.

## Objetivo
Garantir isolamento total entre empresas com schema dedicado e configurações independentes.

## Responsabilidades
- Criação e gerenciamento de empresas (tenants)
- Schema-per-tenant com PostgreSQL schemas
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
- `tenant` package (schema resolver, connection pool per tenant)
- `subscription` module

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
- **Auth** (company_id no JWT, schema resolution)
- **Users** (usuários vinculados à empresa)
- **Database** (schema-per-tenant, migrations)

## Fluxo Resumido
1. SUPER_ADMIN cria empresa → schema PostgreSQL criado → migrations executadas
2. Usuário faz login → JWT contém company_id → schema resolution automático
3. Todas as queries rodam no schema da empresa → isolamento garantido

## Checklist de Implementação
- [ ] Schema-per-tenant com PostgreSQL
- [ ] Schema resolver via JWT claim
- [ ] Configurações por empresa (timezone, idioma)
- [ ] Planos: PLANE/STARTER/PROFESSIONAL/ENTERPRISE
- [ ] Onboarding automático (schema + migrations)
- [ ] Limite de usuários por plano
- [ ] Hard delete após 90 dias de inatividade
- [ ] Isolamento de dados entre tenants

## Checklist de Testes
- [ ] Dados de empresa A não aparecem para empresa B
- [ ] Schema criado corretamente no onboarding
- [ ] Configurações são isoladas por empresa
- [ ] Migrações rodam em todos os schemas

## Documentação Oficial Relacionada
- `docs/tenant/MULTI-TENANCY.md`
- `docs/tenant/SCHEMA-STRATEGY.md`
- `docs/tenant/PLANS.md`

## Histórico de Revisão
| Data | Autor | Descrição |
|------|-------|-----------|
| 2026-07-15 | System | Criação do contexto |
